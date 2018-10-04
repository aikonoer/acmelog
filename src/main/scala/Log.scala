import java.time.{LocalDate, LocalTime}

case class Log(date: LocalDate, time: LocalTime, severity: Severity, description: String)

sealed trait Severity {
  val asString: String
}

case object Error extends Severity {
  override val asString: String = "ERROR"
}

case object Info extends Severity {
  override val asString: String = "INFO"
}

case object Warn extends Severity {
  override val asString: String = "WARN"
}

case class ParseError(error: String, log: String) extends Exception(s"$error,  Log: $log")
