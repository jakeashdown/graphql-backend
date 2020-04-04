package com.heavens_above

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import scala.concurrent.Future
import scala.util.{Failure, Success}

import com.heavens_above.UserRegistry._
import akka.actor.typed.ActorRef
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.AskPattern._
import akka.util.Timeout
import sangria.execution.{ErrorWithResolver, Executor, QueryAnalysisError}
import sangria.parser.QueryParser
import spray.json.{JsObject, JsString, JsValue}
import sangria.ast.Document
import sangria.execution.deferred.DeferredResolver
import sangria.execution.{ErrorWithResolver, Executor, QueryAnalysisError}
import sangria.parser.{QueryParser, SyntaxError}
import sangria.parser.DeliveryScheme.Try
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.MediaTypes._
import akka.http.scaladsl.server._
import akka.stream.{ActorMaterializer, Materializer}
import scala.util.control.NonFatal
import scala.util.{Failure, Success}

//#import-json-formats
//#user-routes-class
class UserRoutes(userRegistry: ActorRef[UserRegistry.Command])(implicit val system: ActorSystem[_]) {

  //#user-routes-class
  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
  import JsonFormats._
  //#import-json-formats

  // If ask takes more time than this to complete the request is failed
  private implicit val timeout = Timeout.create(system.settings.config.getDuration("my-app.routes.ask-timeout"))

  def getUsers(): Future[Users] =
    userRegistry.ask(GetUsers)
  def getUser(name: String): Future[GetUserResponse] =
    userRegistry.ask(GetUser(name, _))
  def createUser(user: User): Future[ActionPerformed] =
    userRegistry.ask(CreateUser(user, _))
  def deleteUser(name: String): Future[ActionPerformed] =
    userRegistry.ask(DeleteUser(name, _))

  import sangria.marshalling.sprayJson._

  implicit val materializer = Materializer(system)

  implicit val ec = system.executionContext

  def executeGraphQLQuery(query: Document, op: Option[String], vars: JsObject) =
    Executor.execute(UserSchema.schema, query, (), variables = vars, operationName = op)
      .map(OK → _)
      .recover {
        case error: QueryAnalysisError ⇒ BadRequest → error.resolveError
        case error: ErrorWithResolver ⇒ InternalServerError → error.resolveError
      }


  def graphQLEndpoint(requestJson: JsValue) = {
    val JsObject(fields) = requestJson

    val JsString(query) = fields("query")

    val operation = fields.get("operationName") collect {
      case JsString(op) ⇒ op
    }

    val vars = fields.get("variables") match {
      case Some(obj: JsObject) ⇒ obj
      case _ ⇒ JsObject.empty
    }

    QueryParser.parse(query) match {
      // query parsed successfully, time to execute it!
      case Success(queryAst) ⇒
        complete(executeGraphQLQuery(queryAst, operation, vars))

      // can't parse GraphQL query, return error
      case Failure(error) ⇒
        complete(BadRequest, JsObject("error" → JsString(error.getMessage)))
    }
  }


  //#all-routes
  //#users-get-post
  //#users-get-delete
  val userRoutes: Route =
  (post & path("graphql")) {
    entity(as[JsValue]) { requestJson ⇒
      graphQLEndpoint(requestJson)
    }
  } ~
    get {
      getFromResource("playground.html")
    }
}
