package my.todos

import akka.actor.{Actor, ActorSystem, Props}
import spray.routing.HttpService
import spray.http.HttpHeaders._ 

import scala.concurrent.Await
import scala.concurrent.Future
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._

class RestAPIActor extends Actor with Routes {
    def actorRefFactory = context
    def receive = runRoute(route)
}

trait Routes extends HttpService {
    import spray.httpx.SprayJsonSupport._
    import spray.http.MediaTypes

    val system = ActorSystem("simple-service")
    // default Actor constructor
    val todoService = system.actorOf(Props[TodoServiceActor], name = "todoServiceActor")
    implicit val timeout = Timeout(10 seconds)

    def addHeaders = respondWithHeaders(
        RawHeader("Access-Control-Allow-Origin", "http://localhost:3000"),
        RawHeader("Access-Control-Allow-Headers", "Content-Type"),
        RawHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS"),
        RawHeader("Access-Control-Max-Age", "86400"))

    val route = {
        addHeaders {
            path("todos") {
                respondWithMediaType(MediaTypes.`application/json`) {
                    get {
                        complete {
                            val future = todoService ? Todos
                            var todosList = Await.result(todoService ? Todos, timeout.duration).asInstanceOf[List[Todo]]
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
                }
            }
        }
    }
}