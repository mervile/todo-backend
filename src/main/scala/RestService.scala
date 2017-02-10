package my.todos

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{AuthorizationFailedRejection, _}
import akka.http.scaladsl.server.directives.Credentials
import akka.stream.ActorMaterializer
import akka.util.Timeout
import akka.pattern.ask
import com.github.t3hnar.bcrypt._
import org.mindrot.jbcrypt.BCrypt
import pdi.jwt.{Jwt, JwtAlgorithm, JwtClaim}

import scala.collection.generic.SeqFactory
import scala.collection.mutable
import scala.concurrent.duration._
import scala.util.{Failure, Success}

trait RestService extends TodosJSONSupport with CORS {
  implicit val system:ActorSystem
  implicit val materializer:ActorMaterializer

  implicit val timeout = Timeout(10.seconds)

  val todoService = system.actorOf(TodoServiceActor.props(),"todoService")

  def myUserPassAuthenticator(credentials: Credentials): Option[ApiUser] =
    credentials match {
      case p @ Credentials.Provided(id) => {
        if (Jwt.isValid(id, "secretKey", Seq(JwtAlgorithm.HS256))) users get id
        else None
      }
      case _ => None
    }

  val myRejectionHandler = RejectionHandler.newBuilder()
    .handleNotFound { complete((StatusCodes.NotFound, "Oh man, what you are looking for is long gone.")) }
    .handle {
      case ValidationRejection(msg, _) => complete((StatusCodes.InternalServerError, msg))
      case AuthorizationFailedRejection => complete(StatusCodes.Unauthorized)
    }
    .result()

  val myExceptionHandler = ExceptionHandler {
    case ex: Exception =>
      extractUri { uri =>
        println(s"Request to $uri could not be handled normally")
        complete(HttpResponse(StatusCodes.InternalServerError, entity = s"An error occurred: ${ex.getMessage}"))
      }
  }

  val users = mutable.Map[String, ApiUser]()
  val route: Route = {
    handleExceptions(myExceptionHandler) {
      handleRejections(myRejectionHandler) {
        accessCtrlHeaders {
          cors {
            options {
              complete(StatusCodes.OK)
            } ~
            pathPrefix("public") {
              path("validate-username") {
                parameter('username.as[String]) { username =>
                  onSuccess((todoService ? FindUser(username)).mapTo[Option[ApiUser]]) { maybeUser =>
                    maybeUser match {
                      case Some(_) => complete(StatusCodes.OK, UsernameValidationResponse(username, false))
                      case None => complete(StatusCodes.OK, UsernameValidationResponse(username, true))
                    }
                  }
                }
              }
            } ~
            pathPrefix("auth") {
              path("register") {
                post {
                  formFields('username.as[String], 'password.as[String]) { (username, password) =>
                    onSuccess((todoService ? FindUser(username)).mapTo[Option[ApiUser]]) { maybeUser =>
                      maybeUser match {
                        case Some(foundUser) => complete(StatusCodes.OK, "Username already exists")
                        case None => {
                          val newUser = ApiUser(username, Some(password.bcrypt(generateSalt)))
                          onSuccess((todoService ? CreateUser(newUser))) { _ =>
                            complete(StatusCodes.Created, newUser.username)
                          }
                        }
                      }
                    }
                  }
                }
              } ~
              path("login") {
                post {
                  formFields('username.as[String], 'password.as[String]) { (username, password) =>
                    onSuccess((todoService ? FindUser(username)).mapTo[Option[ApiUser]]) { maybeUser =>
                      maybeUser match {
                        case Some(user) if user.passwordMatches(password) => {
                          val username = user.username
                          val token = Jwt.encode(JwtClaim({s"""{"username":"$username"}"""}).issuedNow.expiresIn(60*60), "secretKey", JwtAlgorithm.HS256)
                          users += (token -> user)
                          complete(LoginResponse(token))
                        }
                        case None => reject(AuthorizationFailedRejection)
                      }
                    }
                  }
                }
              }
            } ~
            authenticateOAuth2(realm = "secure site", myUserPassAuthenticator) { user =>
              pathPrefix("api") {
                path("todos") {
                  get {
                    onSuccess((todoService ? Todos(user.id.get)).mapTo[List[Todo]]) { todos =>
                      complete(todos)
                    }
                  }
                } ~
                path("todo") {
                  post {
                    entity(as[Todo]) { todo =>
                      var newTodo = todo
                      if (todo.id == -1) {
                        newTodo = Todo(scala.util.Random.nextInt, todo.description, todo.status, user.id.get)
                      }
                      onSuccess((todoService ? CreateorUpdateTodo(newTodo))) { _ =>
                        complete(StatusCodes.Created, newTodo)
                      }
                    }
                  } ~
                  delete {
                    parameters('id.as[Int]) { (id) =>
                      onSuccess((todoService ? DeleteTodobyId(id)).mapTo[Option[Todo]]) { maybeDeleted =>
                        maybeDeleted match {
                          case Some(deleted) => complete(StatusCodes.OK, deleted)
                          case None => complete(StatusCodes.NotFound, s"Couldn't find todo by id ${id}")
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}
