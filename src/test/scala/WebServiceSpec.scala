package my.todos.test

import akka.http.scaladsl.model.headers.{HttpChallenge, Origin}
import akka.http.scaladsl.server.AuthenticationFailedRejection
import akka.http.scaladsl.server.AuthenticationFailedRejection.CredentialsMissing
import org.scalatest.{Matchers, WordSpec}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import my.todos.RestService

class WebServiceSpec extends WordSpec with Matchers with ScalatestRouteTest with RestService {

  "WebService" should {
    "reject API requests without proper Authorization header" in {
      Get("/api/todos")~> Origin("http://localhost:3000") ~> route ~> check {
        rejection shouldBe AuthenticationFailedRejection(CredentialsMissing, HttpChallenge("Bearer", "secure site"))
      }
    }
  }
}

