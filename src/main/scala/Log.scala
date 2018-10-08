import java.time.{LocalDate, LocalTime}


/*
* Log line converts to Log object
* */
case class Log(date: LocalDate, time: LocalTime, severity: String, description: String){
  override def toString: String = s"$date $time $severity - $description"
}
/*
* Generic parsing error exception
* */
case class ParseError(error: String, log: String = "") extends Exception(s"$error $log")
case class ValidationError(error: String) extends Exception(error)
