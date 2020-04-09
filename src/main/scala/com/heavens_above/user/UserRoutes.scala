package com.heavens_above.user

import akka.actor.typed.{ ActorRef, ActorSystem, Scheduler }
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import com.heavens_above.CorsHandler
import sangria.execution.{ ErrorWithResolver, Executor, QueryAnalysisError }
import sangria.parser.DeliveryScheme.Try
import sangria.parser.QueryParser
import spray.json.{ JsObject, JsString, JsValue }

class UserRoutes(registry: ActorRef[UserRegistry.Command])(implicit val system: ActorSystem[_]) extends CorsHandler {

  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
  import akka.http.scaladsl.model.StatusCodes._
  import akka.http.scaladsl.server.Directives._
  import sangria.marshalling.sprayJson._
  import system.executionContext

  implicit val timeout: Timeout =
    Timeout.create(system.settings.config.getDuration("my-app.routes.ask-timeout"))

  implicit val scheduler: Scheduler = system.scheduler

  val resolver = new UserResolver(registry)

  // todo: test error handling
  val route: Route =
    handleCorsRequests {
      getFromResource("playground.html") ~
      (post & path("graphql")) {
        entity(as[JsValue]) {
          case JsObject(fields) =>
            val JsString(query) = fields("query")

            val maybeOperation = fields.get("operationName").collect {
              case JsString(operation) => operation
            }

            val variables = fields.get("variables") match {
              case Some(jsObject: JsObject) => jsObject
              case _                        => JsObject.empty
            }

            QueryParser.parse(query) match {
              case util.Success(queryAst) =>
                complete(
                  Executor
                    .execute(
                      schema = UserSchema.schema,
                      queryAst = queryAst,
                      userContext = resolver,
                      variables = variables,
                      operationName = maybeOperation)
                    .map(OK -> _)
                    .recover {
                      case error: QueryAnalysisError => BadRequest -> error.resolveError
                      case error: ErrorWithResolver  => InternalServerError -> error.resolveError
                    })

              case util.Failure(error) =>
                complete(BadRequest, JsObject("error" -> JsString(error.getMessage)))
            }
        }
      }
    }
}
