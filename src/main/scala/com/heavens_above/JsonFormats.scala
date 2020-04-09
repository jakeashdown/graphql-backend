package com.heavens_above

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import spray.json.{ deserializationError, JsString, JsValue, JsonFormat }

object JsonFormats {
  implicit object LocalDateTimeJsonFormat extends JsonFormat[LocalDateTime] {
    val formatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    override def write(x: LocalDateTime): JsString =
      JsString(x.format(formatter))

    override def read(value: JsValue): LocalDateTime = value match {
      case JsString(x) => LocalDateTime.parse(x, formatter)
      case x           => deserializationError("Expected LocalDateTime as JsString, but got " + x)
    }
  }
}
