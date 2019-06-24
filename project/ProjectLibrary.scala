import sbt._

sealed trait TestType {
  val testTypeLiteral: String
}

case object TestOnly extends TestType {
  override val testTypeLiteral: String = "test"
}

case object IntegrationTestOnly extends TestType {
  override val testTypeLiteral: String = "it"
}

case object AllTests extends TestType {
  override val testTypeLiteral: String = "it, test"
}

object ProjectLibrary {
  val whiskTestKitLibrary = "com.whisk" %% "docker-testkit-scalatest" % ProjectLibraryVersion.whiskVersion

  val whiskSpotifyTestKitLibrary = "com.whisk" %% "docker-testkit-impl-spotify" % ProjectLibraryVersion.whiskVersion

  val flywayLibrary = "org.flywaydb" % "flyway-core" % ProjectLibraryVersion.flywayVersion

  val scalaTestPlusLibrary = "org.scalatestplus.play" %% "scalatestplus-play" % ProjectLibraryVersion.scalaTestPlusVersion

  val typesafeConfigLibrary = "com.typesafe" % "config" % ProjectLibraryVersion.typesafeConfigVersion

  val postgresDriverLibrary = "org.postgresql" % "postgresql" % ProjectLibraryVersion.postgresDriverVersion

  val typesafeLoggingLibrary = "com.typesafe.scala-logging" %% "scala-logging" % ProjectLibraryVersion.typesafeLoggingVersion

  val scoptLibrary = "com.github.scopt" %% "scopt" % ProjectLibraryVersion.scoptVersion

  val logbackLibrary = "ch.qos.logback" % "logback-classic" % ProjectLibraryVersion.logbackVersion
}
