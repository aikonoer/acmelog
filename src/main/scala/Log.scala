import java.time.{LocalDate, LocalTime}

case class Log(date: LocalDate, time: LocalTime, severity: Level, description: String)

sealed trait Level
case object Error extends Level
case object Info extends Level
case object Warn extends Level
