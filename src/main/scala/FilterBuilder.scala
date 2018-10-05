import java.time.LocalDateTime

import akka.NotUsed
import akka.stream.scaladsl.Flow

case class FilterBuilder(build: Flow[Log, Log, NotUsed] = Flow[Log]) {

  def from(from: LocalDateTime): FilterBuilder = addFilter(log => log.date.atTime(log.time).isAfter(from))
  def to(to: LocalDateTime): FilterBuilder = addFilter(log => log.date.atTime(log.time).isBefore(to))

  def severity(sev: Array[Severity]): FilterBuilder = addFilter(log =>
    sev.foldLeft(false){(acc, next) =>
      if(acc || log.severity == next) true else false})

  def contains(words: Array[String]): FilterBuilder =
    addFilter { log =>
      val lowCase = log.description.toLowerCase
      words.foldLeft(false) { (acc, next) =>
        if (acc || lowCase.contains(next.toLowerCase)) true else false }
    }

  private def addFilter(filter: Log => Boolean): FilterBuilder = copy(build.filter(filter))


}


