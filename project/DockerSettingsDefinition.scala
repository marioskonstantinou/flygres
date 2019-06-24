import com.typesafe.sbt.GitPlugin.autoImport.git
import sbt.Keys._
import sbt._
import sbtassembly.AssemblyKeys
import sbtdocker.DockerPlugin.autoImport.{Dockerfile, docker, dockerfile, imageNames}

object DockerSettingsDefinition {

  val dockerDependencies
    : Def.SettingsDefinition = docker := (docker dependsOn (AssemblyKeys.assembly in AssemblyKeys.assembly)).value

  val flygresDockerImageDefinition = dockerfile in docker := {
    val artifact =
      (AssemblyKeys.assemblyOutputPath in AssemblyKeys.assembly in AssemblyKeys.assembly).value
    val artifactTargetPath = s"/app/${artifact.name}"
    val scriptsTargetPath  = "/app/scripts"
    val entryPointPath     = s"$scriptsTargetPath/flygres.sh"
    val dbPath             = "/app/db"

    new Dockerfile {
      from("openjdk:8-jdk-alpine")
      run("apk", "update")
      run("apk", "--no-cache", "add", "bash")
      run("apk", "--no-cache", "add", "coreutils")
      copy(baseDirectory(_ / "flygres.sh").value, entryPointPath)
      copy(baseDirectory(_ / "db/").value, dbPath)
      env("PATH", "$PATH:/app/bin")
      env("SBT_OPTS", "-XX:MaxMetaspaceSize=10g")
      env("BUILD_JAR_NAME", artifactTargetPath)
      env("MIGRATIONS_ROOT", dbPath)
      env(
        "JAR_VERSION",
        s"${sys.env.getOrElse("DOCKER_TAG", version.value)}.${sys.env
          .getOrElse("BUILD_NUMBER", "local-build")}"
      )
      env(
        "GIT_BRANCH",
        s"${sys.env.get("GIT_BRANCH").filter(_.trim.nonEmpty).getOrElse(s"${git.gitCurrentBranch.value}")}")
      env("GIT_COMMIT_ID", git.gitHeadCommit.value.getOrElse("").trim)
      add(artifact, artifactTargetPath)
      run("mkdir", "-p", "/app/conf")
      run("chmod", "+x", entryPointPath)
      expose(5432)
      entryPoint(entryPointPath)
    }
  }

  def dockerImageName(moduleName: String) =
    imageNames in docker := Seq(
      sbtdocker.ImageName(
        namespace = Some("flygres"),
        repository = moduleName,
        tag = Some(s"${sys.env.getOrElse("DOCKER_TAG", "local-build")}")
      )
    )
}
