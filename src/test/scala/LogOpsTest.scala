import java.time.{LocalDate, LocalTime}

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.util.ByteString
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.Await
import scala.concurrent.duration._

class LogOpsTest extends FlatSpec with Matchers {

  implicit val system: ActorSystem = ActorSystem("LoggerApp")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  private val env = new LogOps {}

  val byteString = ByteString("2018-09-26 15:30:26 INFO  - Job 1F2UK301B finished.\n2018-09-26 15:30:26 ERROR - Unhandled exception.\njava.lang.IndexOutOfBoundsException\nat com.acme.SomeClass(SomeClass.java:12)")
  val souce: Source[ByteString, NotUsed] = Source.single(byteString)


  "frame" should "split with new line delimiter" in {

    val expected = Seq("2018-09-26 15:30:26 INFO  - Job 1F2UK301B finished.",
      "2018-09-26 15:30:26 ERROR - Unhandled exception.",
      "java.lang.IndexOutOfBoundsException",
      "at com.acme.SomeClass(SomeClass.java:12)")

    val result = souce.via(env.frame)
      .toMat(Sink.seq)(Keep.right).run()

    Await.result(result, 10 seconds) shouldBe expected.map(ByteString(_))
  }

  "concatLine" should "concatenate next line if it doesn't if a log line pattern" in {

    val expected = Seq("2018-09-26 15:30:26 INFO  - Job 1F2UK301B finished.",
      "2018-09-26 15:30:26 ERROR - Unhandled exception. java.lang.IndexOutOfBoundsException at com.acme.SomeClass(SomeClass.java:12)")
    val result = souce
      .via(env.frame)
      .map(_.utf8String)
      .via(env.concatLines)
      .mapConcat(identity)
      .toMat(Sink.seq)(Keep.right)
      .run()

    Await.result(result, 10 seconds).reverse.toList shouldBe expected
  }

  "isLogPattern" should "return true if a line is a valid log line" in {
    Seq("2018-09-26 15:30:26 INFO  - Job 1F2UK301B finished.",
      "2018-09-26 15:30:26 ERROR - Unhandled exception.",
      "java.lang.IndexOutOfBoundsException",
      "at com.acme.SomeClass(SomeClass.java:12)").map(env.isLogPattern) should be
    Seq(true, true, false, false)
  }

  "toLog" should "parse string to log object" in {
    val line = "2018-09-26 15:30:26 INFO  - Job 1F2UK301B finished."
    Await.result(env.toLog(line), 10 seconds) shouldBe Right(Log(LocalDate.parse("2018-09-26"), LocalTime.parse("15:30:26"), "INFO", "Job 1F2UK301B finished."))
  }


}
