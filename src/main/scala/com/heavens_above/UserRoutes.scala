package com.heavens_above

import scala.concurrent.{ ExecutionContextExecutor, Future }
import scala.util.{ Failure, Success }

import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ ActorRef, ActorSystem }
import akka.http.scaladsl.model.StatusCode
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{ Route, StandardRoute }
import akka.stream.Materializer
import akka.util.Timeout
import sangria.ast.Document
import sangria.execution.{ ErrorWithResolver, Executor, QueryAnalysisError }
import sangria.parser.DeliveryScheme.Try
import sangria.parser.QueryParser
import spray.json.{ JsObject, JsString, JsValue }

class UserRoutes(userRegistry: ActorRef[UserRegistry.Command])(implicit val system: ActorSystem[_])
    extends CorsHandler {

  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._

  // If ask takes more time than this to complete the request is failed
  private implicit val timeout: Timeout =
    Timeout.create(system.settings.config.getDuration("my-app.routes.ask-timeout"))

  import sangria.marshalling.sprayJson._

  implicit val materializer: Materializer = Materializer(system)

  implicit val ec: ExecutionContextExecutor = system.executionContext

  class Resolver(registry: ActorRef[UserRegistry.Command]) extends UserRegistry.Ask {
    override def getUser(id: String): Future[Option[User]] =
      registry.ask[UserRegistry.GetUserResponse](UserRegistry.GetUser(id, _)).map(_.maybeUser)
  }

  val resolver = new Resolver(userRegistry)

  def executeGraphQLQuery(
      query: Document,
      op: Option[String],
      vars: JsObject): Future[(StatusCode with Serializable, JsValue)] =
    Executor.execute(UserSchema.schema, query, resolver, variables = vars, operationName = op).map(OK -> _).recover {
      case error: QueryAnalysisError => BadRequest -> error.resolveError
      case error: ErrorWithResolver  => InternalServerError -> error.resolveError
    }

  def graphQLEndpoint(requestJson: JsValue): StandardRoute = {
    val JsObject(fields) = requestJson

    val JsString(query) = fields("query")

    val operation = fields.get("operationName").collect {
      case JsString(op) => op
    }

    val vars = fields.get("variables") match {
      case Some(obj: JsObject) => obj
      case _                   => JsObject.empty
    }

    QueryParser.parse(query) match {
      // query parsed successfully, time to execute it!
      case Success(queryAst) =>
        complete(executeGraphQLQuery(queryAst, operation, vars))

      // can't parse GraphQL query, return error
      case Failure(error) =>
        complete(BadRequest, JsObject("error" -> JsString(error.getMessage)))
    }
  }

  val userRoutes: Route =
    corsHandler {
      (post & path("graphql")) {
        entity(as[JsValue]) { requestJson =>
          graphQLEndpoint(requestJson)
        } ~
        get {
          getFromResource("playground.html")
        }
      }
    }
}
