package com.heavens_above.user

import java.io.{File, PrintWriter}
import java.time.LocalDateTime

import scala.io.Source

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import com.heavens_above.Identifiable

object User {
  def apply(id: String, name: String): User = User(id, Some(name))
  def apply(id: String, name: String, createdAt: LocalDateTime): User = User(id, Some(name), createdAt)
}

final case class User(id: String, name: Option[String] = None, createdAt: LocalDateTime = LocalDateTime.now())
    extends Identifiable

/**
 * Manages a collection of users, persisted as a JSON file.
 *
 * At the moment, this actor does nothing to ensure the id of each user is unique.
 *
 * ==Behaviour==
 *
 * [[com.heavens_above.user.UserRegistry.GetUsers]]
 * Replies with all users.
 *
 * [[com.heavens_above.user.UserRegistry.GetUser]]
 * Replies with the first user who has the given id.
 *
 * [[com.heavens_above.user.UserRegistry.CreateUser]]
 * Adds the given user to the collection.
 *
 * [[com.heavens_above.user.UserRegistry.DeleteUser]]
 * Removes the user with the given id from the collection.
 **/
object UserRegistry {

  import com.heavens_above.user.UserJson._
  import spray.json._
  import DefaultJsonProtocol._

  type Users = Seq[User]

  val jsonFile = "registry/users.json"

  sealed trait Command
  final case class GetUsers(replyTo: ActorRef[Users]) extends Command
  final case class GetUser(id: String, replyTo: ActorRef[Option[User]]) extends Command
  final case class CreateUser(user: User) extends Command
  final case class DeleteUser(id: String) extends Command

  def apply(): Behavior[Command] = Behaviors.setup { context =>
    context.log.info("reading JSON from file")
    val json = Source.fromFile(jsonFile).mkString

    context.log.info("parsing JSON")
    val users = json.parseJson.convertTo[Users]
    context.log.info(s"parsed ${users.length} users")

    writeUsersThenReceiveMessage(context, users)
  }

  private def writeUsersThenReceiveMessage(context: ActorContext[Command], users: Seq[User]): Behavior[Command] = {
    val writer = new PrintWriter(new File("registry/users.json"))
    try writer.write(users.toJson.prettyPrint) finally writer.close()

    Behaviors.receiveMessage {
      case GetUsers(replyTo) =>
        replyTo ! users
        Behaviors.same

      case GetUser(id, replyTo) =>
        replyTo ! users.find(_.id == id)
        Behaviors.same

      case CreateUser(user) =>
        writeUsersThenReceiveMessage(context, users :+ user)

      case DeleteUser(id) =>
        writeUsersThenReceiveMessage(context, users.filterNot(_.id == id))
    }
  }
}
