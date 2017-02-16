package my.todos.models

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import com.github.t3hnar.bcrypt._
import org.mindrot.jbcrypt.BCrypt
import spray.json.DefaultJsonProtocol

/**
  * Test user in Mongo: test/password
  * @param username
  * @param hashedPassword
  * @param id
  */
case class ApiUser(username: String,
                   hashedPassword: Option[String] = None,
                   id: Option[String] = None) {
  def withPassword(password: String) = copy(hashedPassword = Some(password.bcrypt(generateSalt)))

  def passwordMatches(password: String): Boolean = hashedPassword.exists(hp => BCrypt.checkpw(password, hp))
}

case class FindUser(username: String)

case class LoginResponse(token_id: String)

case class CreateUser(user: ApiUser)

case class UsernameValidationResponse(username: String, isValid: Boolean)

case class UsernameExistsException() extends Exception

trait UserJSONSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val loginResponseFormat = jsonFormat1(LoginResponse)
  implicit val usernameValidationResponse = jsonFormat2(UsernameValidationResponse)
}
