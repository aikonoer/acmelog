import java.time.LocalDateTime

import org.scalatest.{FlatSpec, Matchers}

import scala.util.Left

class ConfigTest extends FlatSpec with Matchers {

  "Config.parse" should "parse" in {
    val args = "-j EXTRACT -s temp/all-logs.txt -f 2018-08-18T00:00:00 -v INFO -d temp/result1.txt"
    Config.parse(args.split(" ")) should be
    Right(Config(
      job = Some("EXTRACT"),
      source = Some("temp/all-logs.txt"),
      from = Some(LocalDateTime.parse("2018-08-18T00:00:00")),
      severity = Some(Array("INFO")),
      destination = Some("temp/result1.txt")
    ))
  }

  "Config.parse" should "return an error" in {
    val args = "-o OPTION -j EXTRACT -s temp/all-logs.txt -f 2018-08-18T00:00:00 -v INFO -d temp/result1.txt"
    Config.parse(args.split(" ")) should be
    Left(ParseError("Parsing argument error! Check format and try again"))
  }

  "Config.validate" should "validates the config object" in {
    val args = "-j EXTRACT -s temp/all-logs.txt -f 2018-08-18T00:00:00 -v INFO -d temp/result1.txt"
    Config.validate(Config.parse(args.split(" ")).right.get) should be
    Right(Config(
      job = Some("EXTRACT"),
      source = Some("temp/all-logs.txt"),
      from = Some(LocalDateTime.parse("2018-08-18T00:00:00")),
      severity = Some(Array("INFO")),
      destination = Some("temp/result1.txt")
    ))
  }

  "Config.validate" should "return no source error" in {
    val args = "-j EXTRACT -f 2018-08-18T00:00:00 -v INFO -d temp/result1.txt"
    Config.validate(Config.parse(args.split(" ")).right.get) should be
    Left(ValidationError("No source argument."))
  }

  "Config.validate" should "return first encountered error" in {
    val args = "-j EXTRACT -s temp/all-logs.txt -f 2018-08-18T00:00:00 -v INFO -d temp/result1.txt"
    Config.validate(Config.parse(args.split(" ")).right.get) should be
    Left(ValidationError("No job argument."))
  }

}
