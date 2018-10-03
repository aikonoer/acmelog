import java.nio.file.Paths

import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl._
import akka.util.ByteString

import scala.concurrent.Future

object EntryPoint extends App {
  implicit val system: ActorSystem = ActorSystem("MyAkkaSystem")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  import scala.concurrent.ExecutionContext.Implicits.global

  val flow: Flow[String, List[String], NotUsed] = Flow[String].fold(List.empty[String]) { (acc, next) =>
    //    TODO: condition - if acc is empty, can't add head
    if (isLogPattern(next)) next :: acc else (acc.head + next) :: acc.tail
  }

  val framing = Framing.delimiter(ByteString("\n"), Int.MaxValue, allowTruncation = true)

  val x: Future[Done] = FileIO.fromPath(Paths.get("all-logs.txt"))
    .via(framing)
    .map(_.utf8String)
    .via(flow)
    .mapConcat(identity)
    .map(toLog)
    .runForeach(println)


  x.onComplete(_ => system.terminate())


  def toLog(log: String): Either[Exception, Log] =
    log.split(" - ") match {
      case Array(timeStamp, det) => {

        val first = timeStamp.split(" ")
        if (first.size == 3) Right(Log(first(0), first(1), first(2), det)) else Left(Unparsable(log))
      }
      case _ => Left(Unparsable(log))
    }

  def isLogPattern(entry: String): Boolean = {
    if (entry.charAt(10) == ' ' &&
      entry.charAt(19) == ' ' &&
      entry.charAt(25) == ' ' &&
      entry.charAt(26) == '-' &&
      entry.charAt(27) == ' ') true else false
  }


}

case class Log(date: String, time: String, severity: String, details: String)

case class Unparsable(msg: String) extends Exception(msg)




