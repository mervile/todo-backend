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
    import Todo._
    import spray.http.MediaTypes

    val system = ActorSystem("simple-service")
    // default Actor constructor
    val todoService = system.actorOf(Props[TodoServiceActor], name = "todoServiceActor")

    var todos = Seq(
        Todo(0, "Setup a dev environment", 2),
        Todo(1, "Learn React with Typescript", 1),
        Todo(2, "Setup unit tests, Karma + shallow rendering?", 2),
        Todo(3, "Use UI library e.g. Material UI", 2),
        Todo(4, "Create small backend with Scala, Akka, Spray", 2),
        Todo(5, "Integrate app with backend", 2),
        Todo(6, "Learn redux", 0),
        Todo(7, "Routing between states", 0),
        Todo(8, "Add drag and drop feature for list items", 2),
        Todo(9, "Save items in a database", 0),
        Todo(10,"Delete a todo", 0)
    )

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
                        implicit val timeout = Timeout(5 seconds)
                        val future = todoService ? Todos
                        val todosList = Await.result(future, timeout.duration).asInstanceOf[List[Todo]]
                        complete(todosList)
                    }
                }
            } ~
            path("todo") {
                options {
                    complete("ok")
                } ~
                get {
                    parameters('id.as[Int]) { (id) =>
                        val todo = todos.find(_.id == id)
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
                        if (todo.id == -1) {
                            // Create a new one
                            val newTodo = Todo(scala.util.Random.nextInt, todo.description, todo.status)
                            todos = todos :+ newTodo
                            respondWithMediaType(MediaTypes.`application/json`) {
                                complete {
                                    newTodo
                                }
                            }
                        } else {
                            // Update old
                            val index = todos.indexWhere(_.id == todo.id)
                            todos = todos.updated(index, todo)
                            respondWithMediaType(MediaTypes.`application/json`) {
                                complete {
                                    todo
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}