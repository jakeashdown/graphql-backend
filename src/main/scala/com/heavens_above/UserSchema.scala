package com.heavens_above

import scala.concurrent.ExecutionContext

import akka.actor.typed.{ActorRef, Scheduler}
import akka.util.Timeout
import com.heavens_above.UserRegistry.Command

object UserSchema {

  import sangria._
  import sangria.schema._
  import sangria._
  import sangria.marshalling._
  import sangria.validation._
  import sangria.schema._
  import sangria.macros.derive._

  // todo: implement type
  trait Identifiable {
    def id: String
  }

  val Id = Argument("id", StringType)

  val UserType: ObjectType[Unit, User] =
    deriveObjectType[Unit, User]()

  val ToEcho = Argument("toEcho", StringType)

  val QueryType =
    ObjectType(
      name = "Query",
      fields = fields[AskUserRegistry, Unit](
        Field(
          "echo",
          StringType,
          arguments = ToEcho :: Nil,
          resolve = c => s"server echoes '${c.arg(ToEcho)}'"
        )
      )
    )

  //def QueryType(registry: ActorRef[Command])(
  //               implicit timeout: Timeout,
  //               scheduler: Scheduler,
  //               executor: ExecutionContext
  //             ) = ObjectType(
  //  name = "Query",
  //  fields = fields[AskUserRegistry, Unit](
  //    Field("user", OptionType(UserType),
  //      description = Some("Returns a user with specific `id`."),
  //      arguments = Id :: Nil,
  //      resolve = c => c.ctx.getUser(c.arg(Id))(registry)),
  //  )
  //)

  val schema = Schema(
    query = QueryType
  )
}
