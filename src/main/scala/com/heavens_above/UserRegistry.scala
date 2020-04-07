package com.heavens_above

import scala.collection.immutable
import scala.concurrent.Future

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ ActorRef, Behavior }

final case class User(id: String, name: String)
final case class Users(users: immutable.Seq[User])

object UserRegistry {

  trait Ask {
    def getUser(id: String): Future[Option[User]]
  }

  sealed trait Command
  final case class GetUsers(replyTo: ActorRef[Users]) extends Command
  final case class CreateUser(user: User, replyTo: ActorRef[ActionPerformed]) extends Command
  final case class GetUser(id: String, replyTo: ActorRef[GetUserResponse]) extends Command
  final case class DeleteUser(id: String, replyTo: ActorRef[ActionPerformed]) extends Command

  final case class GetUserResponse(maybeUser: Option[User])
  final case class ActionPerformed(description: String)

  def apply(): Behavior[Command] =
    registry(Set(User("trashe-racer", "jake"), User("emy", "emma")))

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
