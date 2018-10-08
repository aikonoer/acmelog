import java.time.LocalDateTime

import akka.NotUsed
import akka.stream.scaladsl.Flow
import com.typesafe.scalalogging.Logger


/*
* build filter for timestamps, severities and words
* */
case class FilterBuilder(build: Flow[Log, Log, NotUsed] = Flow[Log]) {

  /*
  * date and time from, inclusive
  * */
  def from(from: LocalDateTime): FilterBuilder = append(log => log.date.atTime(log.time).isAfter(from))

  /*
  * date and time to, inclusive
  * */
  def to(to: LocalDateTime): FilterBuilder = append(log => log.date.atTime(log.time).isBefore(to))


  /*
  * check log line for severity level
  * */
  def severity(sev: Array[String]): FilterBuilder =
    if (sev.isEmpty) this
    else {
      append(log =>
        sev.foldLeft(false) { (acc, next) =>
          if (acc || log.severity == next) true else false
        })
    }

  /*
  * tests log description if it contains words (OR)
  * */
  def contains(words: Array[String]): FilterBuilder =
    if (words.isEmpty) this
    else {
      append { log =>
        val lowCase = log.description.toLowerCase
        words.foldLeft(false) { (acc, next) =>
          if (acc || lowCase.contains(next.toLowerCase)) true else false
        }
      }
    }

  private def append(filter: Log => Boolean): FilterBuilder = copy(build.filter(filter))

}

object FilterBuilder {
  /*
  * from defaults to start of current month
  * to defaults to now
  * severities and contain words defaults to empty array
  *
  * return Flow[Log, Log, Unused]
  *
  * */
  def build(config: LogConfig): Flow[Log, Log, NotUsed] = {
    val logger = Logger("FilterBuilder")
    logger.info("Building filters. From date time defaults to current month while to defaults to now")

    val from = config.from.getOrElse(LocalDateTime.now().withDayOfMonth(1))
    val to = config.to.getOrElse(LocalDateTime.now())
    val severity = config.severity.getOrElse(Array[String]())

    val words = config.words.getOrElse(Array())

    FilterBuilder()
      .from(from)
      .to(to)
      .severity(severity)
      .contains(words)
      .build
  }
}


