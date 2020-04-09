package com.heavens_above.user

import com.heavens_above.Identifiable

object UserSchema {

  import com.heavens_above.ScalarTypes.LocalDateTimeScalar._
  import sangria.macros.derive._
  import sangria.schema._

  val Id: Argument[String] = Argument(name = "id", argumentType = StringType)

  val IdentifiableType = InterfaceType(
    "Identifiable",
    "Entity that can be identified",
    fields[Unit, Identifiable](Field("id", StringType, resolve = _.value.id)))

  val UserType: ObjectType[Unit, User] =
    deriveObjectType[Unit, User](Interfaces(IdentifiableType))

  val QueryType =
    ObjectType(
      name = "Query",
      fields = fields[Resolver, Unit](
        Field(
          name = "users",
          fieldType = ListType(UserType),
          description = Some("Returns all users."),
          resolve = c => c.ctx.getUsers),
        Field(
          name = "user",
          fieldType = OptionType(UserType),
          description = Some("Returns a user with specific `id`."),
          arguments = Id :: Nil,
          resolve = c => c.ctx.getUser(c.arg(Id)))))

  //val MutationType =
  //  ObjectType(name = "Mutation", fields = fields[Resolver, Unit]())

  val schema = Schema(query = QueryType, additionalTypes = List(LocalDateTimeType))
}
