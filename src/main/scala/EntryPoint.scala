import java.nio.file.Paths

import akka.Done
import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl._

import scala.concurrent.Future

object EntryPoint extends App {
  implicit val system: ActorSystem = ActorSystem("MyAkkaSystem")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  import scala.concurrent.ExecutionContext.Implicits.global

  val context = new LogMarshalling {}

  val x: Future[Done] = FileIO.fromPath(Paths.get("temp/all-logs.txt"))
    .via(context.frame)
    .map(context.decodeToString)
    .via(context.concatLines)
    .mapConcat(identity)
    .map(context.toLog)
    .runForeach(println)


  x.onComplete(_ => system.terminate())

}

case class ParseError(error: String, log: String) extends Exception(log)




