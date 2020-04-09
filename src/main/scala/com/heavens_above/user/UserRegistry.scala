package com.heavens_above.user

import java.time.LocalDateTime

import scala.collection.immutable
import scala.concurrent.Future

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import com.heavens_above.Identifiable

object User {
  def apply(id: String, name: String): User = User(id, Some(name))
  def apply(id: String, name: String, createdAt: LocalDateTime): User = User(id, Some(name), createdAt)
}

final case class User(id: String, name: Option[String] = None, createdAt: LocalDateTime = LocalDateTime.now()) extends Identifiable
final case class Users(users: immutable.Seq[User])

trait AsksUserRegistry {
  def getUsers: Future[Seq[User]]
  def getUser(id: String): Future[Option[User]]
}

object UserRegistry {

  // todo: remove
  val defaults: Set[User] = Set(
    User(id = "trashe-racer", name = "jake", createdAt = LocalDateTime.parse("2020-01-01T12:00:00")),
    User(id = "emma.s", createdAt = LocalDateTime.parse("2020-01-02T13:30:00"))
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
