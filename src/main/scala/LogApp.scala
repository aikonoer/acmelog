import java.nio.file.Paths
import java.time.LocalDate

import akka.Done
import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl._

import scala.concurrent.Future

object LogApp extends App {
  implicit val system: ActorSystem = ActorSystem("MyAkkaSystem")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  import scala.concurrent.ExecutionContext.Implicits.global

  val env = new LogOps {}


  val filters = FilterBuilder()
    .from(LocalDate.of(2018, 9, 20).atStartOfDay())
    .to(LocalDate.of(2018, 9, 2).atStartOfDay())
    .contains(Array("FINISHED", "UsEr"))
    .severity(Array(Info))
    .build

  val right: PartialFunction[Either[Exception, Log], Log] = {
    case Right(log) => log
  }

  val x: Future[Done] = FileIO.fromPath(Paths.get("temp/all-logs.txt"))
    .via(env.frame)
    .map(env.decodeToString)
    .via(env.concatLines)
    .mapConcat(identity)
    .mapAsync[Either[Exception, Log]](10)(env.toLog)
    .collect(right)
    .via(filters)
    .runForeach(println)

  val y = FileIO.fromPath(Paths.get("temp/all-logs.txt"))
    .via(env.frame)
    .map(env.decodeToString)
    .via(env.concatLines)
    .mapConcat(identity)
    .mapAsync[Either[Exception, Log]](10)(env.toLog)
    .collect(right)
    .groupBy(5, log => log.severity)
    //    .via(Flow[Log].filter(_.severity == Info))
    .map(_ => 1)
    .fold(0)(_ + _)
    .to(Sink.foreach(println))
    .run()


  //  x.onComplete(_ => system.terminate())
  y.onComplete { r =>
    println(r.get.count)
    system.terminate()
  }

}






