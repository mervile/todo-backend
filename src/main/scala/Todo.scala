package my.todos

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.DefaultJsonProtocol

case class Todos(userId: String)

case class Todo(id: Int, description: String, status: Int, userId: String)

case class CreateorUpdateTodo(todo: Todo)

case class FindTodobyId(id: Int)

case class DeleteTodobyId(id: Int)

case class FindUser(username: String)

case class LoginResponse(token_id: String, username: String)

trait TodosJSONSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val todoFormat = jsonFormat4(Todo)
  implicit val loginResponseFormat = jsonFormat2(LoginResponse)
}
