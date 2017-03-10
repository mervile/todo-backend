package my.todos.utils

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import my.todos.models._
import spray.json.DefaultJsonProtocol

trait TodosJSONSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val todoFormat = jsonFormat5(Todo)
  implicit val project = jsonFormat3(Project)
  implicit val projectUsers = jsonFormat2(ProjectUsers)
  implicit val projectswithtodosFormat = jsonFormat2(ProjectWithTodos)
  implicit val loginResponseFormat = jsonFormat1(LoginResponse)
  implicit val usernameValidationResponse = jsonFormat2(UsernameValidationResponse)
}
