import java.nio.file.Paths
import java.time.LocalDate

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.javadsl.Framing
import akka.stream.scaladsl.{FileIO, Flow, Keep, Sink, Source}
import akka.util.ByteString

import scala.collection.immutable
import scala.concurrent.Future
import scala.util.{Failure, Random, Success}

object LogGenerator extends App {

  implicit val system: ActorSystem = ActorSystem("LoggerApp")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  import scala.concurrent.ExecutionContext.Implicits.global

  val rnd = new Random(9)

  val text: Future[immutable.IndexedSeq[String]] =
    FileIO.fromPath(Paths.get("temp/descriptions.txt"))
      .via(Framing.delimiter(ByteString("\n"), Int.MaxValue))
      .map(_.utf8String)
      .toMat(Sink.collection)(Keep.right)
      .run()

  val level = Array("ERROR", "INFO", "WARN")
  var start = LocalDate.of(2017, 1, 1).atStartOfDay().plusSeconds(1)

  def getLog(n: Int, seq: IndexedSeq[String]): String = {
    val description = seq(n % seq.length)
    val sev = level(n % level.length)
    val from = start.plusMinutes(n).plusSeconds(n / 2)
    start = from
    val fromMod = if (from.getSecond == 0) from.plusSeconds(1) else from
    val dash = if (sev == "ERROR") " -" else "  -"
    if (n % 100 == 0 || n % 100 == 50) s"$description"
    else s"${fromMod.toString.replace('T', ' ')} $sev$dash $description"
  }

  val flows = (lines: IndexedSeq[String]) =>
    Flow[Int]
      .map(_ => rnd.nextInt(100))
      .map(n => getLog(n, lines))

  text.onComplete {
    case Success(lines) =>
      Source(1 to 10000)
        .via(flows(lines))
        .map(l => ByteString(l + "\n"))
        .toMat(FileIO.toPath(Paths.get("temp/generated.txt")))(Keep.right)
        .run()
        .onComplete {
          case scala.util.Success(_) => system.terminate()
          case scala.util.Failure(_) => system.terminate()
        }
    case Failure(_) => system.terminate()

  }
}
