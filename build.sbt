import Dependencies._

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "com.example",
      scalaVersion := "2.12.6",
      version := "0.1.0-SNAPSHOT"
    )),
    name := "acme-logging",
    libraryDependencies += scalaTest % Test,
    libraryDependencies += "com.typesafe.akka" %% "akka-stream" % "2.5.17",
    libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3",
    libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.9.0"
  )
