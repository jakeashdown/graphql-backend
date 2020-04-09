package com.heavens_above.user

import java.time.format.DateTimeFormatter

import scala.util.{ Failure, Success, Try }

import com.heavens_above.Identifiable

object UserSchema {

  import java.time._

  import sangria._
  import sangria.macros.derive._
  import sangria.marshalling._
  import sangria.schema._
  import sangria.validation._

  val Id: Argument[String] = Argument(name = "id", argumentType = StringType)

  object LocalDateTimeScalar {
    case object LocalDateTimeCoercionViolation extends ValueCoercionViolation("LocalDateTime value expected")

    private def parseDate(s: String) = Try(LocalDateTime.parse(s)) match {
      case Success(date) => Right(date)
      case Failure(_)    => Left(LocalDateTimeCoercionViolation)
    }

    val LocalDateTimeType: ScalarType[LocalDateTime] = ScalarType[LocalDateTime](
      "LocalDateTime",
      coerceOutput = (localDateTime, caps) =>
        if (caps.contains(DateSupport)) localDateTime.toLocalDate
        else DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(localDateTime),
      coerceUserInput = {
        case s: String => parseDate(s)
        case _         => Left(LocalDateTimeCoercionViolation)
      },
      coerceInput = {
        case ast.StringValue(s, _, _, _, _) => parseDate(s)
        case _                              => Left(LocalDateTimeCoercionViolation)
      })
  }

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

  val schema = Schema(query = QueryType)
}
