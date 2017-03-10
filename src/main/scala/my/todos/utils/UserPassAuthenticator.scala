package my.todos.utils

import akka.http.scaladsl.server.directives.Credentials
import my.todos.models.ApiUser
import pdi.jwt.{Jwt, JwtAlgorithm, JwtClaim}

import scala.collection.mutable

object UserPassAuthenticator {
  val users = mutable.Map[String, ApiUser]()

  def createSession(user: ApiUser): String = {
    val username = user.username
    val token = Jwt.encode(JwtClaim({s"""{"username":"$username"}"""}).issuedNow.expiresIn(60*60), "secretKey", JwtAlgorithm.HS256)
    users += (token -> user)
    token
  }

  def myUserPassAuthenticator(credentials: Credentials): Option[ApiUser] =
    credentials match {
      case p @ Credentials.Provided(id) => {
        if (Jwt.isValid(id, "secretKey", Seq(JwtAlgorithm.HS256))) users get id
        else None
      }
      case _ => None
    }
}
