import akka.actor.Actor
import spray.routing.HttpService

class SampleServiceActor extends Actor with SampleRoute {
    def actorRefFactory = context
    def receive = runRoute(route)
}

trait SampleRoute extends HttpService {
    import spray.httpx.SprayJsonSupport._
    import Todo._
    import spray.http.MediaTypes
    
    var todos = Seq(
        Todo(0, "Setup a dev environment", 0),
        Todo(1, "Learn React with Typescript", 1),
        Todo(2, "Setup unit tests, Karma + shallow rendering?", 2)
    )

    val route = {
         path("todo") {
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
                        todos = todos :+ todo
                        complete("New todo added!")
                    }
                }
        } ~
        path("todos") {
            respondWithMediaType(MediaTypes.`application/json`) {
                get {
                    complete(todos)
                }
            }
        } ~ get {
            complete("I exist!")
        }
    }
}