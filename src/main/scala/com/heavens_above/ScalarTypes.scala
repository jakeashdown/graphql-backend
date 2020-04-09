package com.heavens_above

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import scala.util.{ Failure, Success, Try }

import sangria.ast
import sangria.marshalling.DateSupport
import sangria.schema.ScalarType
import sangria.validation.ValueCoercionViolation

object ScalarTypes {

  object LocalDateTimeScalar {
    case object LocalDateTimeCoercionViolation extends ValueCoercionViolation("LocalDateTime value expected")

    private def parseDate(s: String) = Try(LocalDateTime.parse(s)) match {
      case Success(date) => Right(date)
      case Failure(_)    => Left(LocalDateTimeCoercionViolation)
    }

    implicit val LocalDateTimeType: ScalarType[LocalDateTime] = ScalarType[LocalDateTime](
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
}
