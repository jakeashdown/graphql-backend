package com.heavens_above

object UserSchema {

  import sangria.macros.derive._
  import sangria.schema._

  // todo: implement type for this trait
  trait Identifiable {
    def id: String
  }

  val Id: Argument[String] = Argument(name = "id", argumentType = StringType)

  val ToEcho: Argument[String] = Argument("toEcho", StringType)

  val UserType: ObjectType[Unit, User] =
    deriveObjectType[Unit, User]()

  val QueryType =
    ObjectType(
      name = "Query",
      fields = fields[UserRegistry.Ask, Unit](
        Field(
          name = "user",
          fieldType = OptionType(UserType),
          description = Some("Returns a user with specific `id`."),
          arguments = Id :: Nil,
          resolve = c => c.ctx.getUser(c.arg(Id)))))

  val schema = Schema(query = QueryType)
}
