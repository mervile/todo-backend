package my.todos

import akka.actor.{Actor, ActorLogging, Props}

object TodoServiceActor {
  def props(): Props = {
    Props(classOf[TodoServiceActor])
  }
}

class TodoServiceActor extends Actor with ActorLogging {
  def receive = {
    case Todos(userId)             => sender ! MongoFactory.findAllByUser(userId)
    case CreateorUpdateTodo(todo)  => sender ! MongoFactory.createOrUpdate(todo)
    case FindTodobyId(id)          => sender ! MongoFactory.findById(id)
    case DeleteTodobyId(id)        => sender ! MongoFactory.delete(id)
    case FindUser(username)        => sender ! MongoFactory.findUser(username)
    case CreateUser(user)          => sender ! MongoFactory.createUser(user)
    case _                         => println("huh?")
  }
}