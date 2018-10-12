import java.time.LocalDateTime

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Keep, RunnableGraph, Sink, Source}
import org.scalatest.{FlatSpec, Matchers}

import scala.collection.immutable
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class FilterBuilderTest extends FlatSpec with Matchers {

  implicit val system: ActorSystem = ActorSystem("LoggerApp")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  private val env = new LogOps {}

  def graph(flow: Flow[Log, Log, NotUsed]): RunnableGraph[Future[immutable.Seq[Log]]] = source
    .mapAsync(10)(env.toLog)
    .collect { case Right(log) => log }
    .via(flow)
    .toMat(Sink.seq[Log])(Keep.right)

  val source = Source(List("2018-09-25 15:30:26 INFO  - User 'joseph@acme.com' logged in to database.",
    "2018-09-25 15:30:27 WARN  - Client SSL certificate about to expire, 2018-10-01",
    "2018-09-25 15:30:28 ERROR - Failed to connect to database."))

  "from" should "returns log that happens on or after the date" in {

    val flow = FilterBuilder()
      .from(LocalDateTime.parse("2018-09-25T15:30:27"))
      .build

    Await.result(graph(flow).run(), 10 seconds).size shouldBe 2
  }

  "to" should "returns log that happens on or before the given date" in {

    val flow = FilterBuilder()
      .to(LocalDateTime.parse("2018-09-25T15:30:28"))
      .build

    Await.result(graph(flow).run(), 10 seconds).size shouldBe 3

  }

  "severity" should "return logs with chosen severity" in {
    val flow = FilterBuilder()
      .severity(Array("INFO", "WARN"))
      .build

    Await.result(graph(flow).run(), 10 seconds).size shouldBe 2
  }

  "contains" should "return logs with phrases or words" in {
    val flow = FilterBuilder()
      .contains(Array("database", "SSL"))
      .build
    Await.result(graph(flow).run(), 10 seconds).size shouldBe 3
  }
}
