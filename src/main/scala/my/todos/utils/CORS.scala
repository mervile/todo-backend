package my.todos.utils

import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{Directive0, Route}

trait CORS {

  lazy val allowedOriginHeader = {
    `Access-Control-Allow-Origin`(HttpOrigin("http://localhost:3000"))
  }

  private def addAccessControlHeaders: Directive0 = {
    mapResponseHeaders { headers =>
      println("HEADERS", headers)
      allowedOriginHeader +:
        `Access-Control-Allow-Credentials`(true) +:
        `Access-Control-Allow-Headers`("Token", "Content-Type", "X-Requested-With", "Authorization") +:
        headers
    }
  }

  private def preflightRequestHandler: Route = options {
    println("PREFLIGHTREQ")
    complete(HttpResponse(200).withHeaders(
      `Access-Control-Allow-Methods`(OPTIONS, POST, PUT, GET, DELETE)
    ))
  }

  def cors(r: Route) = addAccessControlHeaders {
    preflightRequestHandler ~ r
  }
}
