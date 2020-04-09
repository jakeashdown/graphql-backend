package com.heavens_above

import scala.util.{ Failure, Success }

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route

object QuickstartApp {

  import akka.actor.typed.scaladsl.adapter._

  private def startHttpServer(routes: Route, system: ActorSystem[_]): Unit = {

    // A classic actor system is still needed to start akka-http
    implicit val classicSystem: akka.actor.ActorSystem = system.toClassic

    import system.executionContext

    val futureBinding = Http().bindAndHandle(routes, "localhost", 8080)
    futureBinding.onComplete {
      case Success(binding) =>
        val address = binding.localAddress
        system.log.info("Server online at http://{}:{}/", address.getHostString, address.getPort)
      case Failure(ex) =>
        system.log.error("Failed to bind HTTP endpoint, terminating system", ex)
        system.terminate()
    }
  }

  def main(args: Array[String]): Unit = {

    val rootBehavior = Behaviors.setup[Nothing] { context =>
      implicit val system: ActorSystem[Nothing] = context.system

      val userRegistryActor = context.spawn(behavior = UserRegistry(), name = "UserRegistryActor")
      context.watch(other = userRegistryActor)

      val routes = new UserRoutes(userRegistry = userRegistryActor)
      startHttpServer(routes.route, context.system)

      Behaviors.empty
    }
    ActorSystem[Nothing](rootBehavior, "BackendGraphQlServer")
  }
}
