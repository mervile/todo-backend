package my.todos

import akka.actor.{Actor, ActorSystem, Props}
import spray.routing.{Directive1, HttpService}
import spray.http.HttpHeaders._

import scala.concurrent.{Await, Future}
import akka.pattern.ask
import akka.util.Timeout

import scala.collection.mutable
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

class RestAPIActor extends Actor with Routes {
  def actorRefFactory = context

  def receive = runRoute(route)
}

trait Routes extends HttpService with Authenticator {

  import spray.httpx.SprayJsonSupport._
  import spray.http.MediaTypes
  import spray.http._

  val system = ActorSystem("simple-service")
  // default Actor constructor
  val todoService = system.actorOf(Props[TodoServiceActor], name = "todoServiceActor")
  implicit val timeout = Timeout(10 seconds)
  val users = mutable.Map[String, ApiUser]()

  def addHeaders = respondWithHeaders(
    RawHeader("Access-Control-Allow-Origin", "http://localhost:3000"),
    RawHeader("Access-Control-Allow-Credentials", "http://localhost:3000"),
    RawHeader("Access-Control-Allow-Headers", "Content-Type, Authorization"),
    RawHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS, DELETE"),
    RawHeader("Access-Control-Max-Age", "86400"))

  def login(user: Option[ApiUser], password: String): Either[ErrorResponse,LoginResponse] = {
    user match {
      case Some(usr) =>
        if (usr.passwordMatches(password)) {
          val tokenGenerator = new BearerTokenGenerator
          val token = tokenGenerator.generateMD5Token(usr.username)
          users += (token -> usr)
          Right(LoginResponse(token, usr.username))
        }
        else Left(ErrorResponse("Wrong username or password!"))
      case None => Left(ErrorResponse("Wrong username or password!"))
    }
  }

  def findByAPIKey(key: String): Option[ApiUser] = users get key

  val authenticator = TokenAuthenticator[ApiUser](
    headerName = "Authorization",
    queryStringParameterName = ""
  ) { key =>
    println("KEY " + findByAPIKey(key.substring(7)))
    Future(findByAPIKey(key.substring(7)))
  }

  def auth: Directive1[ApiUser] = authenticate(authenticator)

  val route = {
    addHeaders {
      pathPrefix("auth") {
        options {
          complete("ok")
        } ~
        path("login") {
          post {
            formFields('username.as[String], 'password.as[String]) { (username, password) =>
              val user = Await.result(todoService ? FindUser(username), timeout.duration).asInstanceOf[Option[ApiUser]]
              // TODO add proper error handling, logging maybe use expiring cache for sessions
              respondWithMediaType(MediaTypes.`application/json`) {
                login(user, password) match {
                  case Left(error: ErrorResponse) => complete(StatusCodes.Unauthorized, error)
                  case Right(res) => complete(res)
                }
              }
            }
          }
        }
      } ~
      pathPrefix("api") {
        options {
          complete("ok")
        } ~
        path("todos") {
          respondWithMediaType(MediaTypes.`application/json`) {
            get {
              auth { user =>
                complete {
                  Await.result(todoService ? Todos(user.id), timeout.duration).asInstanceOf[List[Todo]]
                }
              }
            }
          }
        } ~
        path("todo") {
          get {
            auth { user =>
              parameters('id.as[Int]) { (id) =>
                val future = todoService ? FindTodobyId(id)
                val todo = Await.result(future, timeout.duration).asInstanceOf[Option[Todo]]
                todo match {
                  case Some(value) => {
                    respondWithMediaType(MediaTypes.`application/json`) {
                      complete(value)
                    }
                  }
                  case None => complete(s"Todo with id $id not found!")
                }
              }
            }
          } ~
          post {
            auth { user =>
              entity(as[Todo]) { todo =>
                var newTodo = todo
                if (todo.id == -1) {
                  newTodo = Todo(scala.util.Random.nextInt, todo.description, todo.status, user.id)
                }
                val future = todoService ? CreateorUpdateTodo(newTodo)
                val success = Await.result(future, timeout.duration).asInstanceOf[Boolean]
                respondWithMediaType(MediaTypes.`application/json`) {
                  complete {
                    newTodo
                  }
                }
              }
            }
          } ~
          delete {
            parameters('id.as[Int]) { (id) =>
              auth { user =>
                val future = todoService ? DeleteTodobyId(id)
                val deletedTodo = Await.result(future, timeout.duration).asInstanceOf[Option[Todo]]
                println("DELETED TODO", deletedTodo)
                deletedTodo match {
                  case Some(value) => {
                    respondWithMediaType(MediaTypes.`application/json`) {
                      complete(value)
                    }
                  }
                  case None => complete(s"Todo with id $id not found!")
                }
              }
            }
          }
        }
      }
    }
  }
}