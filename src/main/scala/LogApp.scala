import java.nio.file.Paths
import java.time.LocalDate

import akka.Done
import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl._
import akka.util.ByteString

import scala.concurrent.Future

object LogApp extends App {
  implicit val system: ActorSystem = ActorSystem("MyAkkaSystem")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  import scala.concurrent.ExecutionContext.Implicits.global

  val env: LogOps = new LogOps {}

  val searchFilter = FilterBuilder()
    .from(LocalDate.of(2018, 9, 20).atStartOfDay())
    .to(LocalDate.of(2018, 9, 30).atStartOfDay())
    .contains(Array("FINISHED", "UsEr"))
    .severity(Array(Info))
    .build

  val statsFilter = FilterBuilder()
    .from(LocalDate.of(2018, 9, 1).atStartOfDay())
    .to(LocalDate.of(2018, 10, 1).atStartOfDay())
    .severity(Array(Error, Warn))
    .build

  val right: PartialFunction[Either[Exception, Log], Log] = {
    case Right(log) => log
  }

  val byteStringToLog = Flow[ByteString]
    .via(env.frame)
    .map(env.decodeToString)
    .via(env.concatLines)
    .mapConcat(identity)
    .mapAsync[Either[Exception, Log]](10)(env.toLog)

  val source = FileIO.fromPath(Paths.get("temp/all-logs.txt"))
  val sink = FileIO.toPath(Paths.get("temp/result.txt"))

  val x = source
    .via(byteStringToLog)
    .collect(right)
    .via(searchFilter)
    .map(log => ByteString(log.toString + "\n"))
    .to(sink)
    .run()

  val y = source
    .via(byteStringToLog)
    .collect(right)
    .via(statsFilter)
    .map(_ => 1)
    .reduce(_ + _)
    .toMat(Sink.head)(Keep.right)
    .run()


  x.onComplete { r =>
    println(r.getOrElse(List.empty[Log]))
    system.terminate()
  }

  y.onComplete { r =>
    println(r.get)
    system.terminate()
  }

}






