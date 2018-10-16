import java.io
import java.nio.file.Paths

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, IOResult}
import akka.stream.scaladsl.{FileIO, Flow, Keep, RunnableGraph, Sink, Source}
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

      def graph(isSearch: Boolean, toFile: Boolean, source: Source[Log, Future[IOResult]]): RunnableGraph[Future[io.Serializable]] = {
        val sourceComposed = if (isSearch) source else source.fold(0)((acc, _) => acc + 1)
        if (toFile)
          sourceComposed
            .map(log => ByteString(log + "\n"))
            .toMat(FileIO.toPath(Paths.get(conf.destination.get)))(Keep.right)
        else sourceComposed.toMat(Sink.foreach(println))(Keep.right)
      }

      graph(conf.job.get == "SEARCH", conf.destination.isDefined, source.via(flow))
        .run()
        .onComplete {
          case Success(_) => logger.info("Done."); system.terminate()
          case Failure(ex) => logger.error(ex.getLocalizedMessage); system.terminate()
        }
    }
  }
}