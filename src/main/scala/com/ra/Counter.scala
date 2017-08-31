package com.ra

import java.net.InetSocketAddress
import akka.actor.ActorSystem
import akka.http.scaladsl.coding.Gzip
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.settings.ConnectionPoolSettings
import akka.http.scaladsl.{ClientTransport, Http}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import akka.util.ByteString
import com.ra.Print._
import com.typesafe.config.{Config, ConfigFactory}
import org.json4s.DefaultFormats
import org.json4s.jackson.Serialization._
import scala.concurrent.ExecutionContext.Implicits.global

case class Resp(items: Seq[Items])
case class Items(tags: List[String], is_answered: Boolean)
case class Result(tag: String, total: Int, answered: Int)
case class Count(total: Int, answered: Int)

case class Counter(tags: Seq[(String, String)]) {

  implicit val system = ActorSystem("my-system")
  implicit val materializer = ActorMaterializer()
  implicit val formats = DefaultFormats
  implicit val config: Config = ConfigFactory.load()

  val proxyHost = config.getString("proxy.host")
  val proxyPort = config.getInt("proxy.port")
  val maxThreads = config.getInt("maxThreads.num")
  val httpsProxyTransport = ClientTransport.httpsProxy(InetSocketAddress.createUnresolved(proxyHost, proxyPort))

  val settings = if (proxyHost != "" || proxyPort != 0)
    ConnectionPoolSettings(system)
      .withTransport(httpsProxyTransport)
  else ConnectionPoolSettings(system)

  def countTagsAndAnswers() = {
    Source(tags.toList)
      .filter(tag => tag._1 == "tag")
      .map(tag => HttpRequest(
        uri = s"https://api.stackexchange.com/2.2/search?pagesize=100&order=desc&sort=creation&tagged=${tag._2}&site=stackoverflow"))
      .mapAsync(maxThreads) { request =>
        Http().singleRequest(request, settings = settings)
          .map(Gzip.decodeMessage(_))
          .flatMap(_.entity.dataBytes.runFold(ByteString.empty)(_ ++ _)
            .map(x => read[Resp](x.utf8String).items)
          )
      }
      .runFold(Seq.empty[Items])(_ ++ _)
      .map { x => aggregator(x) }
      .map { x => x.map(y => y.tag -> Count(y.total, y.answered)) }
      .map(x => writePretty(x.toMap).print)
  }

  def aggregator(data: Seq[Items]): Seq[Result] = {
    val allTags = data.flatMap(_.tags).distinct
    allTags.map { x =>
      val filtered = data.filter(_.tags.exists(_ == x))
      Result(x, filtered.length, filtered.count(_.is_answered))
    }
  }
}


