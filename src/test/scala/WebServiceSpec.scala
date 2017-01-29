package my.todos.test

import org.scalatest.{WordSpec, Matchers}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import my.todos.RestService

class WebServiceSpec extends WordSpec with Matchers with ScalatestRouteTest with RestService {

  "WebService" should {
    "reject API requests without proper Authorization header" in {
      Get("/api/todos") ~> route ~> check {
        handled shouldBe false
      }
    }
  }
}

