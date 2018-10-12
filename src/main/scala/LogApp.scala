import java.io
import java.nio.file.Paths

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{FileIO, Flow, Keep, Sink}
import akka.util.ByteString
import com.typesafe.scalalogging.Logger

import scala.concurrent.Future
import scala.util.{Failure, Success}

case class LogApp(env: LogOps, args: Array[String]) {

  implicit val system: ActorSystem = ActorSystem("LoggerApp")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  import scala.concurrent.ExecutionContext.Implicits.global

  val logger = Logger("LoggerApp")

  /*
  * Run the whole process of parsing, validating, build filters and attaching all parts of the stream.
  * */
  def run(): Unit = {

    val logConfig = for {
      parsed <- Config.parse(args)
      validated <- Config.validate(parsed)
    } yield validated

    if (logConfig.isLeft) {
      system.terminate()
      logger.error(logConfig.left.get.getLocalizedMessage)
    } else {

      logger.info(logConfig.right.get.toString)
      val conf = logConfig.right.get

      val source = FileIO.fromPath(Paths.get(conf.source.get))
      val toLog = Flow[ByteString]
        .via(env.frame)
        .map(env.decodeToString)
        .via(env.concatLines)
        .mapConcat(identity)
        .mapAsync[Either[Exception, Log]](10)(env.toLog)

      val right: PartialFunction[Either[Exception, Log], Log] = {
        case Right(log) => log
      }

      val filters = FilterBuilder.build(conf)

      val flow: Flow[ByteString, Log, NotUsed] =
        Flow[ByteString]
          .via(toLog)
          .collect(right)
          .via(filters)


      def sink[T](toFile: Boolean): Sink[T, Future[io.Serializable]] = if (toFile) {
        Flow[T]
          .map(e => ByteString(e.toString + "\n"))
          .toMat(FileIO.toPath(Paths.get(conf.destination.get)))(Keep.right)
      } else {
        Flow[T]
          .toMat(Sink.foreach(println))(Keep.right)
      }

      val isSearch = {
        val toFile = conf.destination.isDefined
        if (conf.job.get == "SEARCH") {
          if (toFile) flow.toMat(sink[Log](toFile = true))(Keep.right)
          else flow.toMat(sink[Log](toFile = false))(Keep.right)
        } else {
          val folded = flow.fold(0)((acc, _) => acc + 1)
          if (toFile) folded.toMat(sink[Int](toFile = true))(Keep.right)
          else folded.toMat(sink[Int](toFile = false))(Keep.right)
        }
      }

      source
        .toMat(isSearch)(Keep.right)
        .run()
        .onComplete {
          case Success(_) => logger.info("Done."); system.terminate()
          case Failure(ex) => logger.error(ex.getLocalizedMessage); system.terminate()
        }
    }
  }
}