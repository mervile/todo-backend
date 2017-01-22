package my.todos

import spray.json.DefaultJsonProtocol

case class Todos(userId: String)

case class Todo(id: Int, description: String, status: Int, userId: String)

case class CreateorUpdateTodo(todo: Todo)

case class FindTodobyId(id: Int)

case class DeleteTodobyId(id: Int)

case class FindUser(username: String)

case class LoginResponse(token_id: String, username: String)

case class ErrorResponse(message: String)

object Todo extends DefaultJsonProtocol {
    implicit val todoFormat = jsonFormat4(Todo.apply)
}

object LoginResponse extends DefaultJsonProtocol {
  implicit val loginResponseFormat = jsonFormat2(LoginResponse.apply)
}

object ErrorResponse extends DefaultJsonProtocol {
  implicit val errorResponseFormat = jsonFormat1(ErrorResponse.apply)
}
