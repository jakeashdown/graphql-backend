package com.heavens_above.user

import com.heavens_above.user.UserRegistry.ActionPerformed
import spray.json.{ DefaultJsonProtocol, RootJsonFormat }

object UserJson {

  // Imports the default encoders for primitive types (Int, String, List, etc)
  import DefaultJsonProtocol._

  implicit val userJsonFormat: RootJsonFormat[User] =
    jsonFormat2((id: String, name: Option[String]) => User.apply(id, name))

  implicit val usersJsonFormat: RootJsonFormat[Users] =
    jsonFormat1(Users)

  implicit val actionPerformedJsonFormat: RootJsonFormat[ActionPerformed] =
    jsonFormat1(ActionPerformed)
}
