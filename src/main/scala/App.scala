import java.nio.file.Paths

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{FileIO, Flow, Keep, Sink}
import akka.util.ByteString
import com.typesafe.scalalogging.Logger

import scala.util.{Failure, Success}

case class LogApp(env: LogOps, args: Array[String]) {

  implicit val system: ActorSystem = ActorSystem("LoggerApp")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  import scala.concurrent.ExecutionContext.Implicits.global

  val logger = Logger("LoggerApp")

  def run(): Unit = {

    val logConfig = for {
      parsed <- LogConfig.parse(args)
      validated <- LogConfig.validate(parsed)
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

      val flow = Flow[ByteString]
        .via(toLog)
        .collect(right)
        .via(filters)

      val graph = if (conf.destination.isDefined) {
        source
          .via(flow)
          .map(log => ByteString(log.toString + "\n"))
          .toMat(FileIO.toPath(Paths.get(conf.destination.get)))(Keep.right)
      } else {
        source
          .via(flow)
          .toMat(Sink.foreach(println))(Keep.right)
      }

      graph
        .run()
        .onComplete {
          case Success(_) => logger.info("Done."); system.terminate()
          case Failure(ex) => logger.error(ex.getLocalizedMessage); system.terminate()
        }
    }
  }
}