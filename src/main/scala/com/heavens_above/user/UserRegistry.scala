package com.heavens_above.user

import scala.collection.immutable
import scala.concurrent.Future

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ ActorRef, Behavior }
import com.heavens_above.Identifiable

object User {
  def apply(id: String, name: String): User = User(id, Some(name))
}

final case class User(id: String, name: Option[String] = None) extends Identifiable
final case class Users(users: immutable.Seq[User])

trait AsksUserRegistry {
  def getUsers: Future[Seq[User]]
  def getUser(id: String): Future[Option[User]]
}

object UserRegistry {

  // todo: remove
  val defaults: Set[User] = Set(
    User(id = "trashe-racer", name = "jake"),
    User(id = "emma.s")
  )

  sealed trait Command
  final case class GetUsers(replyTo: ActorRef[Users]) extends Command
  final case class CreateUser(user: User, replyTo: ActorRef[ActionPerformed]) extends Command
  final case class GetUser(id: String, replyTo: ActorRef[GetUserResponse]) extends Command
  final case class DeleteUser(id: String, replyTo: ActorRef[ActionPerformed]) extends Command

  final case class GetUserResponse(maybeUser: Option[User])
  final case class ActionPerformed(description: String)

  def apply(): Behavior[Command] = registry(defaults)

  private def registry(users: Set[User]): Behavior[Command] =
    Behaviors.receiveMessage {
      case GetUsers(replyTo) =>
        replyTo ! Users(users.toSeq)
        Behaviors.same

      case CreateUser(user, replyTo) =>
        replyTo ! ActionPerformed(s"User ${user.id} created.")
        registry(users + user)

      case GetUser(id, replyTo) =>
        replyTo ! GetUserResponse(users.find(_.id == id))
        Behaviors.same

      case DeleteUser(id, replyTo) =>
        replyTo ! ActionPerformed(s"User $id deleted.")
        registry(users.filterNot(_.id == id))
    }
}
