package my.todos.models

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.DefaultJsonProtocol

case class Todos(userId: String)

case class Todo(id: Int, description: String, status: Int, userId: String)

case class CreateorUpdateTodo(todo: Todo)

case class FindTodobyId(id: Int)

case class DeleteTodobyId(id: Int)

trait TodosJSONSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val todoFormat = jsonFormat4(Todo)
}
