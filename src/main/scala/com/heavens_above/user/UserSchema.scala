package com.heavens_above.user

import scala.concurrent.{ ExecutionContext, Future }

import akka.actor.typed.{ ActorRef, Scheduler }
import akka.util.Timeout
import com.heavens_above.Identifiable
import com.heavens_above.user.UserRegistry.Command

class UserResolver(registry: ActorRef[Command])(
    implicit timeout: Timeout,
    scheduler: Scheduler,
    val executionContext: ExecutionContext) {

  import UserRegistry._
  import akka.actor.typed.scaladsl.AskPattern._

  def askRegistryForUsers: Future[Seq[User]] =
    registry.ask[Users](GetUsers)

  def askRegistryForUser(id: String): Future[Option[User]] =
    registry.ask[Option[User]](GetUser(id, _))

  def tellRegistryCreateUser(user: User): Unit =
    registry.tell(CreateUser(user))

  def tellRegistryDeleteUser(id: String): Unit =
    registry.tell(DeleteUser(id))
}

object UserSchema {

  import com.heavens_above.ScalarTypes.LocalDateTimeScalar._
  import sangria.macros.derive._
  import sangria.schema._

  val Id = Argument(name = "id", argumentType = StringType)
  val NameOption = Argument(name = "name", argumentType = OptionInputType(StringType))

  val IdentifiableType = InterfaceType(
    "Identifiable",
    "Entity that can be identified",
    fields[Unit, Identifiable](Field("id", StringType, resolve = _.value.id)))

  val UserType: ObjectType[Unit, User] =
    deriveObjectType[Unit, User](Interfaces(IdentifiableType))

  val QueryType =
    ObjectType(
      name = "Query",
      fields = fields[UserResolver, Unit](
        Field(
          name = "users",
          fieldType = ListType(UserType),
          description = Some("Returns all users."),
          resolve = c => c.ctx.askRegistryForUsers),
        Field(
          name = "user",
          fieldType = OptionType(UserType),
          description = Some("Returns a user with specific `id`."),
          arguments = Id :: Nil,
          resolve = c => c.ctx.askRegistryForUser(c.arg(Id)))))

  val MutationType =
    ObjectType(
      name = "Mutation",
      fields = fields[UserResolver, Unit](
        Field(
          name = "createUserIfUnique",
          fieldType = OptionType(UserType),
          arguments = Id :: NameOption :: Nil,
          resolve = { c =>
            import c.ctx.executionContext

            val id = c.arg(Id)
            c.ctx.askRegistryForUsers.map { users =>
              if (!users.exists(_.id == id)) {
                val maybeName = c.arg(NameOption)
                val user = User(id, maybeName)
                c.ctx.tellRegistryCreateUser(user)
                Some(user)
              } else None
            }
          }),
        Field(
          name = "updateUserName",
          fieldType = OptionType(UserType),
          arguments = Id :: NameOption :: Nil,
          resolve = { c =>
            import c.ctx.executionContext
            val (id, maybeName) = (c.arg(Id), c.arg(NameOption))

            c.ctx
              .askRegistryForUser(id)
              .map(_.fold[Option[User]](None) {
                case User(_, _, createdAt) =>
                  val user = User(id = id, maybeName, createdAt)
                  c.ctx.tellRegistryDeleteUser(id)
                  c.ctx.tellRegistryCreateUser(user)
                  Some(user)
              })
          })))

  val schema =
    Schema(query = QueryType, additionalTypes = List(LocalDateTimeType), mutation = Some(MutationType))
}
