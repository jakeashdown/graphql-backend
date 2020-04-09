package com.heavens_above.user

import com.heavens_above.user.UserRegistry.ActionPerformed
import spray.json.{ DefaultJsonProtocol, RootJsonFormat }

object UserJson {

  // Imports the default encoders for primitive types (Int, String, List, etc)
  import DefaultJsonProtocol._

  implicit val userJsonFormat: RootJsonFormat[User] =
    jsonFormat2(User)

  implicit val usersJsonFormat: RootJsonFormat[Users] =
    jsonFormat1(Users)

  implicit val actionPerformedJsonFormat: RootJsonFormat[ActionPerformed] =
    jsonFormat1(ActionPerformed)
}
