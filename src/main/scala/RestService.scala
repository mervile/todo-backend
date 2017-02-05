package my.todos

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{AuthenticationFailedRejection, RejectionHandler, Route, ValidationRejection}
import akka.http.scaladsl.server.directives.Credentials
import akka.stream.ActorMaterializer
import akka.util.Timeout
import akka.pattern.ask
import com.github.t3hnar.bcrypt._
import org.mindrot.jbcrypt.BCrypt

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
      case p @ Credentials.Provided(id) => users get id
      case _ => None
    }

  val myRejectionHandler = RejectionHandler.newBuilder()
    .handleNotFound { complete((StatusCodes.NotFound, "Oh man, what you are looking for is long gone.")) }
    .handle {
      case ValidationRejection(msg, _) => complete((StatusCodes.InternalServerError, msg))
      case AuthenticationFailedRejection(msg, _) => complete(StatusCodes.Unauthorized)
    }
    .result()

  val users = mutable.Map[String, ApiUser]()
  val route: Route = {
    handleRejections(myRejectionHandler) {
      accessCtrlHeaders {
        cors {
          options {
            complete(StatusCodes.OK)
          } ~
          pathPrefix("auth") {
            path("register") {
              post {
                formFields('username.as[String], 'password.as[String]) { (username, password) =>
                  onComplete((todoService ? FindUser(username)).mapTo[Option[ApiUser]]) {
                    case Success(maybeUser) => {
                      maybeUser match {
                        case Some(foundUser) => complete(StatusCodes.NotAcceptable, "Username already exists")
                        case None =>
                          val newUser = ApiUser(username, Some(password.bcrypt(generateSalt)))
                          onComplete((todoService ? CreateUser(newUser))) {
                            case Success(_) => complete(StatusCodes.Created, newUser.username)
                            case Failure(ex) => complete(StatusCodes.InternalServerError, s"An error occurred: ${ex.getMessage}")
                          }
                      }
                    }
                    case Failure(ex) => complete(StatusCodes.InternalServerError, s"An error occurred: ${ex.getMessage}")
                  }
                }
              }
            } ~
            path("login") {
              post {
                formFields('username.as[String], 'password.as[String]) { (username, password) =>
                  val future = (todoService ? FindUser(username)).mapTo[Option[ApiUser]]
                  onComplete(future) {
                    case Success(maybeUser) => {
                      maybeUser match {
                        case Some(user) if user.passwordMatches(password) =>
                          val tokenGenerator = new BearerTokenGenerator
                          val token = tokenGenerator.generateMD5Token(user.username)
                          users += (token -> user)
                          complete(LoginResponse(token, user.username))
                        case _ => complete(StatusCodes.Unauthorized)
                      }
                    }
                    case Failure(ex) => complete(StatusCodes.InternalServerError, s"An error occurred: ${ex.getMessage}")
                  }
                }
              }
            }
          } ~
          authenticateOAuth2(realm = "secure site", myUserPassAuthenticator) { user =>
            pathPrefix("api") {
              path("todos") {
                get {
                  onComplete((todoService ? Todos(user.id.get)).mapTo[List[Todo]]) {
                    case Success(todos) => complete(todos)
                    case Failure(ex) => complete(StatusCodes.InternalServerError, s"An error occurred: ${ex.getMessage}")
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
                    onComplete((todoService ? CreateorUpdateTodo(newTodo))) {
                      case Success(_) => complete(StatusCodes.Created, newTodo)
                      case Failure(ex) => complete(StatusCodes.InternalServerError, s"An error occurred: ${ex.getMessage}")
                    }
                  }
                } ~
                delete {
                  parameters('id.as[Int]) { (id) =>
                    onComplete((todoService ? DeleteTodobyId(id)).mapTo[Option[Todo]]) {
                      case Success(maybeDeleted) => {
                        maybeDeleted match {
                          case Some(deleted) => complete(StatusCodes.OK, deleted)
                          case None => complete(StatusCodes.NotFound, s"Couldn't find todo by id ${id}")
                        }
                      }
                      case Failure(ex) => complete(StatusCodes.InternalServerError, s"An error occurred: ${ex.getMessage}")
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
