package my.todos

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.directives.Credentials
import akka.pattern.ask
import akka.util.Timeout

import scala.collection.mutable
import scala.concurrent.duration._
import scala.io.StdIn
import scala.util.{Success, Failure}

object WebService extends TodosJSONSupport {

  val host = "localhost"
  val port = 8080
  val users = mutable.Map[String, ApiUser]()

  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem("todo-backend")
    implicit val materializer = ActorMaterializer()
    implicit val executionContext = system.dispatcher
    implicit val timeout = Timeout(10.seconds)

    val todoService = system.actorOf(TodoServiceActor.props(),"todoService")

    def addHeaders = respondWithDefaultHeaders(
      RawHeader("Access-Control-Allow-Origin", "http://localhost:3000"),
      RawHeader("Access-Control-Allow-Credentials", "http://localhost:3000"),
      RawHeader("Access-Control-Allow-Headers", "Content-Type, Authorization"),
      RawHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS, DELETE"),
      RawHeader("Access-Control-Max-Age", "86400"))

    def myUserPassAuthenticator(credentials: Credentials): Option[ApiUser] =
      credentials match {
        case p @ Credentials.Provided(id) => users get id
        case _ => None
      }

    val route: Route = {
      addHeaders {
        options {
          complete(StatusCodes.OK)
        } ~
        pathPrefix("auth") {
          path("login") {
            post {
              formFields('username.as[String], 'password.as[String]) { (username, password) =>
                val maybeUser = (todoService ? FindUser(username)).mapTo[Option[ApiUser]]
                onSuccess(maybeUser) {
                  case Some(user) if user.passwordMatches(password) =>
                    val tokenGenerator = new BearerTokenGenerator
                    val token = tokenGenerator.generateMD5Token(user.username)
                    users += (token -> user)
                    complete(LoginResponse(token, user.username))
                  case _ => complete(StatusCodes.Unauthorized)
                }
              }
            }
          }
        } ~
        authenticateOAuth2(realm = "secure site", myUserPassAuthenticator) { user =>
          pathPrefix("api") {
            path("todos") {
              get {
                onComplete((todoService ? Todos(user.id)).mapTo[List[Todo]]) {
                  case Success(todos)  => complete(todos)
                  case Failure(ex) => complete(StatusCodes.InternalServerError, s"An error occurred: ${ex.getMessage}")
                }
              }
            } ~
            path("todo") {
              post {
                entity(as[Todo]) { todo =>
                  var newTodo = todo
                  if (todo.id == -1) {
                    newTodo = Todo(scala.util.Random.nextInt, todo.description, todo.status, user.id)
                  }
                  onComplete((todoService ? CreateorUpdateTodo(newTodo))) {
                    case Success(_)  => complete(StatusCodes.Created, newTodo)
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
                        case None          => complete(StatusCodes.NotFound, s"Couldn't find todo by id ${id}")
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

    val bindingFuture = Http().bindAndHandle(route, host, port)
    println(s"Waiting for requests at http://$host:$port/...\nHit RETURN to terminate")
    StdIn.readLine()

    //Shutdown
    bindingFuture.flatMap(_.unbind())
    system.terminate()
  }
}