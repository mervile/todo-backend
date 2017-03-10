package my.todos.http.routes

import akka.actor.ActorRef
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{AuthorizationFailedRejection, Route}
import akka.pattern.ask
import akka.util.Timeout
import com.github.t3hnar.bcrypt.{generateSalt, _}
import my.todos.models._
import my.todos.utils.TodosJSONSupport

import scala.concurrent.ExecutionContext
import my.todos.utils.UserPassAuthenticator.createSession

class UserServiceRoute(val userService: ActorRef)
                      (implicit executionContext: ExecutionContext, implicit val timeout: Timeout)
  extends TodosJSONSupport{

  val route: Route =
    pathPrefix("public") {
      path("validate-username") {
        pathEndOrSingleSlash {
          parameter('username.as[String]) { username =>
            onSuccess((userService ? FindUser(username)).mapTo[Option[ApiUser]]) { maybeUser =>
              maybeUser match {
                case Some(_) => complete(StatusCodes.OK, UsernameValidationResponse(username, false))
                case None => complete(StatusCodes.OK, UsernameValidationResponse(username, true))
              }
            }
          }
        }
      }
    } ~
    pathPrefix("auth") {
      path("register") {
        pathEndOrSingleSlash {
          post {
            formFields('username, 'password) { (username, password) =>
              onSuccess((userService ? FindUser(username)).mapTo[Option[ApiUser]]) { maybeUser =>
                maybeUser match {
                  case Some(foundUser) => throw new UsernameExistsException
                  case None => {
                    val newUser = ApiUser(username, Some(password.bcrypt(generateSalt)))
                    onSuccess(userService ? CreateUser(newUser)) { _ =>
                      complete(StatusCodes.Created, newUser.username)
                    }
                  }
                }
              }
            }
          }
        }
      } ~
      path("login") {
        pathEndOrSingleSlash {
          post {
            formFields('username, 'password) { (username, password) =>
              onSuccess((userService ? FindUser(username)).mapTo[Option[ApiUser]]) { maybeUser =>
                maybeUser match {
                  case Some(user: ApiUser) if user.passwordMatches(password) => {
                    val token = createSession(user)
                    complete(s"""{"token_id": "$token"}""")
                  }
                  case None => reject(AuthorizationFailedRejection)
                }
              }
            }
          }
        }
      }
    }
}