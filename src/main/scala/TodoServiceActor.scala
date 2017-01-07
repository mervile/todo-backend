package my.todos

import akka.actor.Actor

class TodoServiceActor extends Actor  {
    def receive = {
        case Todos(userId)            => sender ! MongoFactory.findAllByUser(userId)
        case CreateorUpdateTodo(todo) => {
            MongoFactory.createOrUpdate(todo)
            sender ! true
        }
        case FindTodobyId(id)         => sender ! MongoFactory.findById(id)
        case DeleteTodobyId(id)       => sender ! MongoFactory.delete(id)
        case _                        => println("huh?")
    }
}