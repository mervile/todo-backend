package my.todos

import akka.actor.Actor
import spray.json._
import DefaultJsonProtocol._

class TodoServiceActor extends Actor  {
    def receive = {
        case Todos                    => sender ! MongoFactory.findAll
        case CreateorUpdateTodo(todo) => {
            MongoFactory.createOrUpdate(todo)
            sender ! true
        }
        case FindTodobyId(id)         => sender ! MongoFactory.findById(id)
        case _                        => println("huh?")
    }
}