package my.todos

import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.server.Directives._

trait CORS {
  val origin = HttpOrigin("http://localhost:3000")

  val accessCtrlHeaders = respondWithDefaultHeaders(
    RawHeader("Access-Control-Allow-Origin", "http://localhost:3000"),
    RawHeader("Access-Control-Allow-Credentials", "http://localhost:3000"),
    RawHeader("Access-Control-Allow-Headers", "Content-Type, Authorization"),
    RawHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS, DELETE"),
    RawHeader("Access-Control-Max-Age", "86400"))

  def cors = {
    checkSameOrigin(HttpOriginRange(origin)) & (get | post | options | delete)
  }
}
