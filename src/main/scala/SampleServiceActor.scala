import akka.actor.Actor
import spray.routing.HttpService
import spray.http.HttpHeaders._ 

class SampleServiceActor extends Actor with SampleRoute {
    def actorRefFactory = context
    def receive = runRoute(route)
}

trait SampleRoute extends HttpService {
    import spray.httpx.SprayJsonSupport._
    import Todo._
    import spray.http.MediaTypes

    var todos = Seq(
        Todo(0, "Setup a dev environment", 2),
        Todo(1, "Learn React with Typescript", 1),
        Todo(2, "Setup unit tests, Karma + shallow rendering?", 2),
        Todo(3, "Use UI library e.g. Material UI", 2),
        Todo(4, "Create small backend with Scala, Akka, Spray and some NoSQL DB", 1),
        Todo(5, "Integrate app with backend", 1),
        Todo(6, "Learn redux", 0),
        Todo(7, "Routing between states", 0),
        Todo(8, "Add drag and drop feature for list items", 2)
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
                        complete(todos)
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