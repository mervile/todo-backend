package my.todos

import akka.actor.{Actor, ActorSystem, Props}
import spray.routing.HttpService
import spray.http.HttpHeaders._

import scala.concurrent.Await
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._

import scala.concurrent.ExecutionContext.Implicits.global

class RestAPIActor extends Actor with Routes {
  def actorRefFactory = context

  def receive = runRoute(route)
}

trait Routes extends HttpService with Authenticator {

  import spray.httpx.SprayJsonSupport._
  import spray.http.MediaTypes

  val system = ActorSystem("simple-service")
  // default Actor constructor
  val todoService = system.actorOf(Props[TodoServiceActor], name = "todoServiceActor")
  implicit val timeout = Timeout(10 seconds)

  def addHeaders = respondWithHeaders(
    RawHeader("Access-Control-Allow-Origin", "http://localhost:3000"),
    RawHeader("Access-Control-Allow-Headers", "Content-Type"),
    RawHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS, DELETE"),
    RawHeader("Access-Control-Max-Age", "86400"))

  val route = {
    addHeaders {
      pathPrefix("api") {
        authenticate(basicUserAuthenticator) { authInfo =>
          path("todos") {
            respondWithMediaType(MediaTypes.`application/json`) {
              get {
                complete {
                  val todosList = Await.result(todoService ? Todos(authInfo.user.id), timeout.duration).asInstanceOf[List[Todo]]
                  todosList
                }
              }
            }
          } ~
          path("todo") {
            options {
              complete("ok")
            } ~
            get {
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
            } ~
            post {
              entity(as[Todo]) { todo =>
                var newTodo = todo
                if (todo.id == -1) {
                  newTodo = Todo(scala.util.Random.nextInt, todo.description, todo.status)
                }
                val future = todoService ? CreateorUpdateTodo(newTodo)
                val success = Await.result(future, timeout.duration).asInstanceOf[Boolean]
                respondWithMediaType(MediaTypes.`application/json`) {
                  complete {
                    newTodo
                  }
                }
              }
            } ~
            delete {
              parameters('id.as[Int]) { (id) =>
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