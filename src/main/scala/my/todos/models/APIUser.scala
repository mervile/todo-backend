package my.todos.models

import com.github.t3hnar.bcrypt._
import org.mindrot.jbcrypt.BCrypt

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

case class User(id: String, username: String)

case object GetUsers
