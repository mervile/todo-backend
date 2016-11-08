import akka.actor.Actor
import spray.routing.HttpService

class SampleServiceActor extends Actor with SampleRoute {
    def actorRefFactory = context
    def receive = runRoute(route)
}

trait SampleRoute extends HttpService {
    val route = {
        get {
            path("stuff") {
                complete("That's my stuff!")
            }
        } ~ get {
            complete("I exist!")
        }
    }
}