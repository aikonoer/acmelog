import java.time.LocalDateTime

import com.typesafe.scalalogging.Logger

import scala.util.Try

case class Config(job: Option[String] = None,
                  source: Option[String] = None,
                  destination: Option[String] = None,
                  from: Option[LocalDateTime] = None,
                  to: Option[LocalDateTime] = None,
                  severity: Option[Array[String]] = None,
                  words: Option[Array[String]] = None) {

  override def toString: String = {
    s"""
       |Job: ${job.getOrElse("None")}
       |Source: ${source.getOrElse("None")}
       |Destination: ${destination.getOrElse("None")}
       |From: ${from.getOrElse("None")}
       |To: ${to.getOrElse("None")}
       |Severities: ${severity.map(_.mkString(", ")).getOrElse("None")}
       |Contain words: ${words.map(_.mkString(", ")).getOrElse("None")}
    """.stripMargin
  }
}


object Config {

  /*
  * Parse program arguments
  * */
  def parse(args: Array[String]): Either[Throwable, Config] = {
    val logger = Logger("LogConfig")
    logger.info("Parsing program arguments.")

    def loop(args: List[String]): Config = args match {
      case Nil => Config()
      case "-j" :: j :: tail => loop(tail).copy(job = Some(j))
      case "-s" :: s :: tail => loop(tail).copy(source = Some(s))
      case "-d" :: d :: tail => loop(tail).copy(destination = Some(d))
      case "-f" :: f :: tail => loop(tail).copy(from = Some(LocalDateTime.parse(f)))
      case "-t" :: t :: tail => loop(tail).copy(to = Some(LocalDateTime.parse(t)))
      case "-v" :: v :: tail => loop(tail).copy(severity = Some(v.split(',')))
      case "-w" :: w :: tail => loop(tail).copy(words = Some(w.split(',').map(_.trim)))
      case _ => throw ParseError("Parsing argument error! Check format and try again")
    }

    Try {
      loop(args.toList)
    }.toEither
  }

  /*
  * Validates LogConfig data
  * */
  def validate(config: Config): Either[Throwable, Config] = {

    val logger = Logger("LogConfig")
    logger.info("Validating program arguments.")
    Try {
      if (config.job.isEmpty) throw ValidationError("No job argument.")
      if (!Array("SEARCH", "EXTRACT").contains(config.job.get)) throw ValidationError("Job argument incorrect. Try SEARCH or EXTRACT")
      if (config.source.isEmpty) throw ParseError("No source argument.")
      if (!new java.io.File(config.source.get).exists) throw ValidationError("Source file doesn't exists! Please try again.")
      if (config.destination.isDefined) {
        val file = new java.io.File(config.destination.get)
        if (!file.getParentFile.isDirectory) throw ValidationError("Destination path invalid.")
        if (file.exists()) throw ValidationError("File already exists.")
      }
      config
    }.toEither
  }
}