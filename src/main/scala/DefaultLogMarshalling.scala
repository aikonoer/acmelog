import java.time.{LocalDate, LocalTime}

import akka.NotUsed
import akka.stream.scaladsl.{Flow, Framing}
import akka.util.ByteString

import scala.util.{Failure, Success, Try}

trait DefaultLogMarshalling {

  def frame: Flow[ByteString, ByteString, NotUsed] =
    Framing.delimiter(ByteString("\n"), Int.MaxValue, allowTruncation = true)

  def decodeToString: ByteString => String = byt => byt.utf8String

  def concatLines: Flow[String, List[String], NotUsed] = Flow[String].fold(List.empty[String]) { (acc, next) =>
    //    TODO: condition - if acc is empty, can't add head
    if (isLogPattern(next)) next :: acc else (acc.head + next) :: acc.tail
  }

  def isLogPattern(entry: String): Boolean = {
    if (entry.charAt(10) == ' ' &&
      entry.charAt(19) == ' ' &&
      entry.charAt(25) == ' ' &&
      entry.charAt(26) == '-' &&
      entry.charAt(27) == ' ') true else false
  }

  def toLog: String => Either[Exception, Log] = (log: String) =>
    log.split(" - ") match {
      case Array(timeStamp, det) =>
        val first = timeStamp.split(" ")
        if (first.size == 3) {
          Try{
            (LocalDate.parse(first(0)), LocalTime.parse(first(1)), first(2) match {
              case "INFO" => Info
              case "WARN" => Warn
              case "ERROR" => Error
              case _ => throw ParseError("Unknown severity", log)
            })
          } match {
            case Success(data) => Right(Log(data._1, data._2, data._3, det))
            case Failure(ex) => Left(ParseError(ex.getCause.getLocalizedMessage, log))
          }
        } else Left(ParseError("Can't parse!", log))

      case _ => Left(ParseError("Split Error", log))
    }

}
