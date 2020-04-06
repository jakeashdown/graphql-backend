package com.heavens_above

import akka.http.scaladsl.model.{ HttpHeader, HttpResponse, StatusCodes }
import akka.http.scaladsl.server.{ Directive0, Route }

trait CorsHandler {

  import akka.http.scaladsl.model.HttpMethods._
  import akka.http.scaladsl.model.headers._
  import akka.http.scaladsl.server.Directives._

  private val accessControlResponseHeaders: List[HttpHeader] = List(
    `Access-Control-Allow-Origin`.*,
    `Access-Control-Allow-Credentials`(true),
    `Access-Control-Allow-Headers`("Authorization", "Content-Type", "X-Requested-With"))

  private val respondWithCorsHeaders: Directive0 =
    respondWithHeaders(accessControlResponseHeaders)

  private def preflightComplete: Route =
    options {
      complete(
        HttpResponse(StatusCodes.OK).withHeaders(`Access-Control-Allow-Methods`(OPTIONS, POST, PUT, GET, DELETE)))

    }

  // Wraps the route to
  //  - respond with CORS headers, and
  //  - handle preflight requests
  def handleCorsRequests(route: Route): Route =
    respondWithCorsHeaders {
      preflightComplete ~ route
    }
}
