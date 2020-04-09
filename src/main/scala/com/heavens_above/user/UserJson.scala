package com.heavens_above.user

import java.time.LocalDateTime

import spray.json.{ DefaultJsonProtocol, RootJsonFormat }

object UserJson {

  import com.heavens_above.JsonFormats._

  // Imports the default encoders for primitive types (Int, String, List, etc)
  import DefaultJsonProtocol._

  implicit val userJsonFormat: RootJsonFormat[User] =
    jsonFormat3((id: String, name: Option[String], createdAt: LocalDateTime) =>
      User.apply(id = id, name = name, createdAt = createdAt))
}
