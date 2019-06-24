val integrationTestLibraries: Seq[ModuleID] = Seq(
  ProjectLibrary.whiskTestKitLibrary,
  ProjectLibrary.whiskSpotifyTestKitLibrary,
  ProjectLibrary.flywayLibrary,
  ProjectLibrary.postgresDriverLibrary,
  ProjectLibrary.typesafeConfigLibrary,
  ProjectLibrary.typesafeLoggingLibrary,
  ProjectLibrary.scoptLibrary,
  ProjectLibrary.logbackLibrary,
  addToTestLibraries(TestOnly, ProjectLibrary.scalaTestPlusLibrary)
)

def addToTestLibraries[T <: TestType](testType: T, moduleID: ModuleID): ModuleID = {
  moduleID % testType.testTypeLiteral
}

val flygresImageName = "flygres"
val flygresMainClass = "com.flygres.db.MainApp"

libraryDependencies ++= integrationTestLibraries

lazy val root = Project(id = "root", base = file("."))
  .settings(CommonSettingsDefinition.commonSettings: _*)
  .settings(resourceDirectory in Test := baseDirectory.value / "src/test/resources-test")
  .settings(
    Seq(
      mainClass in Compile := Some(flygresMainClass),
      test in assembly := {},
      assemblyJarName in assembly := {
        val snapshotImageName = s"$flygresImageName-snapshot-${sys.env.getOrElse("DOCKER_TAG", version.value)}.${sys.env.getOrElse("BUILD_NUMBER", "local-build")}.jar"
        val productionImageName = s"$flygresImageName-${sys.env.getOrElse("DOCKER_TAG", version.value)}.${sys.env.getOrElse("BUILD_NUMBER", "local-build")}.jar"
        if (isSnapshot.value) snapshotImageName
        else productionImageName
      }
    )
  )
  .settings(
    DockerSettingsDefinition.dockerDependencies,
    DockerSettingsDefinition.flygresDockerImageDefinition,
    DockerSettingsDefinition.dockerImageName(flygresImageName)
  )
  .enablePlugins(GitVersioning)
  .enablePlugins(sbtdocker.DockerPlugin)
  .enablePlugins(JavaAppPackaging)

// Removes possible dependency conflicts warnings (play related)
// TODO check possible dependency conflicts (beside play)
evictionWarningOptions in update :=
  EvictionWarningOptions.default.withWarnTransitiveEvictions(false)
