import sbt.Keys._
import sbt._

object CommonSettingsDefinition {
  val nameString         = "flygres"
  val organisationString = "com.flygres"
  val scalaVersionString = "2.12.8"

  lazy val allResolvers = Seq(
    "Akka Snapshot Repository" at "http://repo.akka.io/snapshots/",
    "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/",
    "Confluent Maven Repository" at "https://packages.confluent.io/maven/"
  )

  lazy val commonSettings = Seq(
    name := nameString,
    version := sys.env.getOrElse("DOCKER_TAG", "local-build"),
    organization := organisationString,
    scalaVersion := scalaVersionString,
    scalacOptions := Seq(
      //      "-Ystatistics:typer", // Enable compiler statistics (useful for benchmarking
      //      "-Ylog-classpath",
      "-deprecation", // Emit warning and location for usages of deprecated APIs.
      "-feature", // Emit warning and location for usages of features that should be imported explicitly.
      "-unchecked", // Enable additional warnings where generated code depends on assumptions.
      "-encoding",
      "UTF-8",
      //      "-Xlint", // Enable recommended additional warnings.
      //      "-Ywarn-adapted-args", // Warn if an argument list is modified to match the receiver.
      //      "-Ywarn-dead-code", // Warn when dead code is identified.
      //      "-Ywarn-inaccessible", // Warn about inaccessible types in method signatures.
      //      "-Ywarn-nullary-override", // Warn when non-nullary overrides nullary, e.g. def foo() over def foo.
      //      "-Ywarn-numeric-widen" // Warn when numerics are widened.
    ),
    scalacOptions in Test ~= { (options: Seq[String]) =>
      options filterNot (_ == "-Ywarn-dead-code") // Allow dead code in tests (to support using mockito).
    },
    resolvers ++= allResolvers
  )

}
