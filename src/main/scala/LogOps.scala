import java.time.{LocalDate, LocalTime}

import akka.NotUsed
import akka.stream.scaladsl.{Flow, Framing}
import akka.util.ByteString

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}


/*
* Default implementations of the operations to parse ByteString to Log
* */
trait LogOps {

  /*
  * Frames byteString with line break
  * */
  def frame: Flow[ByteString, ByteString, NotUsed] =
    Framing.delimiter(ByteString("\n"), Int.MaxValue, allowTruncation = true)

  /*
  * Decodes byteString to UTF8 encoded String
  * */
  def decodeToString: ByteString => String = _.utf8String

  /*
  * Appends description from next line/s
  * */
  def concatLines: Flow[String, List[String], NotUsed] =
    Flow[String].fold(List.empty[String]) { (acc, next) =>
      if (isLogPattern(next) || acc.isEmpty) next :: acc
      else (acc.head + next) :: acc.tail
    }

  /*
  * Checks if string patterns a valid log entry
  * */
  def isLogPattern(entry: String): Boolean = {
    val firstSpace = 10
    val secondSpace = 19
    val thirdSpace = 25
    val dash = 26
    val fourthSpace = 27

    if (entry.charAt(firstSpace) == ' ' &&
      entry.charAt(secondSpace) == ' ' &&
      entry.charAt(thirdSpace) == ' ' &&
      entry.charAt(dash) == '-' &&
      entry.charAt(fourthSpace) == ' ') true else false
  }

  /*
  * Parses log line to Log object
  * */
  def toLog: String => Future[Either[Exception, Log]] = (log: String) =>
    log.splitAt(26) match {
      case (timeStamp, details) =>
        val dateTimeLevel = timeStamp.split(" ")
        if (dateTimeLevel.size == 3) {
          Try {
            val date = LocalDate.parse(dateTimeLevel(0))
            val time = LocalTime.parse(dateTimeLevel(1))
            val severity = dateTimeLevel(2)
            (date, time, severity)
          } match {
            case Success((date, time, severity)) => Future.successful(Right(Log(date, time, severity, details.drop(2))))
            case Failure(ex) => Future.successful(Left(ParseError(ex.getMessage, log)))
          }
        } else Future.successful(Left(ParseError("Can't parse!", log)))

      case _ => Future.successful(Left(ParseError("Split Error", log)))
    }

}
