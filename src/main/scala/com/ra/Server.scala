package com.ra

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives
import akka.stream.ActorMaterializer
import scala.io.StdIn

object Server extends Directives {
  def main(args: Array[String]) {

    implicit val system = ActorSystem("my-system")
    implicit val materializer = ActorMaterializer()
    implicit val executionContext = system.dispatcher

    val route =
      get {
        path("search") {
          parameterSeq { tags =>
            onSuccess(Counter(tags).countTagsAndAnswers) { x =>
              complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, x.replace("\n", "<br>")))
            }
          }
        }
      } ~ get{
        complete("Сервер обрабатывает запросы с uri /search")
      }

    val bindingFuture = Http().bindAndHandle(route, "0.0.0.0", 8080)

    println(s"Server online at http://localhost:8080 \nPress RETURN to stop...")
        StdIn.readLine() // let it run until user presses return
        bindingFuture
          .flatMap(_.unbind()) // trigger unbinding from the port
          .onComplete(_ => system.terminate()) // and shutdown when done
  }
}

