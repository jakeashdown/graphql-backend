package com.heavens_above.user

import java.time.LocalDateTime

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ ActorRef, Behavior }
import com.heavens_above.Identifiable

object User {
  def apply(id: String, name: String): User = User(id, Some(name))
  def apply(id: String, name: String, createdAt: LocalDateTime): User = User(id, Some(name), createdAt)
}

final case class User(id: String, name: Option[String] = None, createdAt: LocalDateTime = LocalDateTime.now())
    extends Identifiable

object UserRegistry {

  type Users = Seq[User]

  // todo: remove
  val defaults: Seq[User] = Seq(
    User(id = "trashe-racer", name = "jake", createdAt = LocalDateTime.parse("2020-01-01T12:00:00")),
    User(id = "emma.s", createdAt = LocalDateTime.parse("2020-01-02T13:30:00")))

  sealed trait Command
  final case class GetUsers(replyTo: ActorRef[Users]) extends Command
  final case class GetUser(id: String, replyTo: ActorRef[Option[User]]) extends Command
  final case class CreateUser(user: User) extends Command
  final case class DeleteUser(id: String) extends Command

  def apply(): Behavior[Command] = registry(defaults)

  private def registry(users: Seq[User]): Behavior[Command] =
    Behaviors.receiveMessage {
      case GetUsers(replyTo) =>
        replyTo ! users
        Behaviors.same

      case CreateUser(user) =>
        registry(users :+ user)

      case GetUser(id, replyTo) =>
        replyTo ! users.find(_.id == id)
        Behaviors.same

      case DeleteUser(id) =>
        registry(users.filterNot(_.id == id))
    }
}
