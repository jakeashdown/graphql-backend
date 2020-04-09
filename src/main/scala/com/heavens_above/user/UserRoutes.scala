package com.heavens_above.user

import scala.concurrent.{ ExecutionContext, ExecutionContextExecutor, Future }

import akka.actor.typed.{ ActorRef, ActorSystem, Scheduler }
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import akka.util.Timeout
import UserRegistry.{ Command, GetUser, GetUserResponse, GetUsers }
import com.heavens_above.CorsHandler
import sangria.ast.Document
import sangria.execution.{ ErrorWithResolver, Executor, QueryAnalysisError }
import sangria.parser.DeliveryScheme.Try
import sangria.parser.QueryParser
import spray.json.{ JsObject, JsString, JsValue }

class Resolver(registry: ActorRef[Command])(
    implicit timeout: Timeout,
    scheduler: Scheduler,
    executionContext: ExecutionContext)
    extends AsksUserRegistry {

  import akka.actor.typed.scaladsl.AskPattern._

  override def getUsers: Future[Seq[User]] =
    registry.ask[Users](GetUsers).map(_.users)

  override def getUser(id: String): Future[Option[User]] =
    registry.ask[GetUserResponse](GetUser(id, _)).map(_.maybeUser)
}

object UserRoutes {

  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
  import akka.http.scaladsl.model.StatusCodes._
  import sangria.marshalling.sprayJson._

  def executeQuery(query: Document, resolver: Resolver, maybeOperation: Option[String], variables: JsObject)(
      implicit executionContext: ExecutionContext): ToResponseMarshallable =
    Executor
      .execute(
        schema = UserSchema.schema,
        queryAst = query,
        userContext = resolver,
        variables = variables,
        operationName = maybeOperation)
      .map(OK -> _)
      .recover {
        case error: QueryAnalysisError => BadRequest -> error.resolveError
        case error: ErrorWithResolver  => InternalServerError -> error.resolveError
      }
}

class UserRoutes(userRegistry: ActorRef[UserRegistry.Command])(implicit val system: ActorSystem[_])
    extends CorsHandler {

  import UserRoutes._
  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
  import akka.http.scaladsl.model.StatusCodes._
  import akka.http.scaladsl.server.Directives._

  implicit val materializer: Materializer = Materializer(system)

  implicit val timeout: Timeout =
    Timeout.create(system.settings.config.getDuration("my-app.routes.ask-timeout"))

  implicit val executionContext: ExecutionContextExecutor = system.executionContext
  implicit val scheduler: Scheduler = system.scheduler

  val resolver = new Resolver(userRegistry)

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
                complete(executeQuery(queryAst, resolver, maybeOperation, variables))

              case util.Failure(error) =>
                complete(BadRequest, JsObject("error" -> JsString(error.getMessage)))
            }
        }
      }
    }
}
