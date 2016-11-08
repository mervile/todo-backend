import akka.actor.Actor
import spray.routing.HttpService

class SampleServiceActor extends Actor with SampleRoute {
    def actorRefFactory = context
    def receive = runRoute(route)
}

trait SampleRoute extends HttpService {
    val route = {
         path("stuff") {
            get {
                complete("That's my stuff!")
            } ~
              post {
                  complete("stuff posted!")
              }
        } ~ get {
            complete("I exist!")
        }
    }
}