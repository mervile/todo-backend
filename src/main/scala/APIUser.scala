package my.todos

import com.github.t3hnar.bcrypt._
import org.mindrot.jbcrypt.BCrypt
import spray.routing.authentication.{BasicAuth, UserPass}
import spray.routing.directives.AuthMagnet

import scala.concurrent.{ExecutionContext, Future}

/**
  * Test user in Mongo: test/password
  * @param username
  * @param hashedPassword
  */
case class ApiUser(username: String,
                   hashedPassword: Option[String] = None) {
  def withPassword(password: String) = copy(hashedPassword = Some(password.bcrypt(generateSalt)))

  def passwordMatches(password: String): Boolean = hashedPassword.exists(hp => BCrypt.checkpw(password, hp))
}

class AuthInfo(val user: ApiUser) {
  def hasPermission(permission: String) = {
    true
  }
}

trait Authenticator {
  def basicUserAuthenticator(implicit ec: ExecutionContext): AuthMagnet[AuthInfo] = {
    def validateUser(userPass: Option[UserPass]): Option[AuthInfo] = {
      for {
          p <- userPass
          user <- MongoFactory.findUser(p.user)
          if user.passwordMatches(p.pass)
      } yield new AuthInfo(user)
    }

    def authenticator(userPass: Option[UserPass]): Future[Option[AuthInfo]] = Future {
      validateUser(userPass)
    }

    BasicAuth(authenticator _, realm = "Private API")
  }
}
