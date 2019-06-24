import java.time.{ZoneId, ZonedDateTime}
import java.util.Date

import com.flygres.db.models.{DBMigrationConfiguration, Migrate}
import com.flygres.db.services.FlywayService
import com.spotify.docker.client.DefaultDockerClient
import com.whisk.docker.DockerFactory
import com.whisk.docker.impl.spotify.SpotifyDockerFactory
import com.whisk.docker.scalatest.DockerTestKit
import org.flywaydb.core.api.{MigrationInfo, MigrationType}
import org.joda.time.{DateTime, DateTimeZone}
import org.scalatest._

import scala.collection.JavaConverters._
import scala.util.Try

case class MigrationExecutionResult(execution: MigrationExecution,
                                    failureCause: Option[FailureCause] = None)

sealed trait MigrationExecution
case object Success extends MigrationExecution
case object Failure extends MigrationExecution

sealed trait FailureCause {
  def cause: String
}

case object FileSuffix extends FailureCause {
  def cause: String = "File must be of type (.sql)"
}

case object MigrationPrefix extends FailureCause {
  def cause: String = "Scripts must be migration (V) or repeatable (R)"
}

case object MigrationVersion extends FailureCause {
  def cause: String =
    "Versions must be 10 digit timestamps of no later than today's date e.g. 1550664911"
}

case class MigrationResult(script: String,
                           result: MigrationExecutionResult,
                           installedOn: Date,
                           checksum: Int)

class FlywayServiceSpec
    extends WordSpec
    with Matchers
    with DockerTestKit
    with DockerPostgresService {

  override implicit val dockerFactory: DockerFactory =
    new SpotifyDockerFactory(DefaultDockerClient.fromEnv().build())

  "FlywayService" should {
    val flyway                 = new FlywayService
    val currentExistingSchemas = config.getStringList("check_schemas").asScala

    val publicCurrentLocations =
      Seq("filesystem:db/initial", "filesystem:db/public/migrations", "filesystem:db/public/views")

    "fetch migration locations" in {
      flyway.filterAndSortSchemas(PostgresConfiguration) should be(currentExistingSchemas)
    }
    "extract flyway paths" in {
      val (extractedLocations, expectedLocations): (Seq[String], Seq[String]) =
        currentExistingSchemas.foldLeft((Seq[String](), Seq[String]()))((acc, i) => {
          val schemaCurrentLocations = i match {
            case "public" => publicCurrentLocations
          }

          val extractLocations: Seq[String] = flyway.migrationsLocations(i)

          (acc._1 ++ extractLocations, acc._2 ++ schemaCurrentLocations)
        })

      extractedLocations.size should be(expectedLocations.size)
    }
    "Start postgres docker container successfully" in {
      isContainerReady(postgresContainer).futureValue should be(true)
    }
    "prepare configuration for DBMigrationConfiguration" in {
      val configuration = DBMigrationConfiguration.prepareConfiguration(config, Migrate, false)
      configuration.actionType should be(Migrate)
      configuration.outOfOrder should be(false)
    }
    "Run flyway migration scripts successfully" in {
      val migrationInfoServices = flyway.runMigrations(PostgresConfiguration)

      val executionResult = migrationInfoServices
        .map { migrationInfoService =>
          val migrationResults = migrationInfoService.all().toList

          val (success, failed) = migrationResults
            .foldLeft(List[MigrationResult]())((acc, i) => {
              val migrationScript = i.getScript
              acc :+ MigrationResult(migrationScript,
                                     validateScript(i),
                                     i.getInstalledOn,
                                     i.getChecksum)
            })
            .partition(x => x.result.execution == Success)

          failed.groupBy(_.result.failureCause.get).foreach { x =>
            val failedScripts = x._2.map(_.script).mkString("\n")
            println("------------------------------------------------------------------------")
            println(x._1.cause)
            println(failedScripts)
            println("------------------------------------------------------------------------")
          }

          failed.isEmpty
        }
        .reduce(_ && _)

      executionResult should be(true)
    }
  }

  def now(): ZonedDateTime = ZonedDateTime.now(UtcZone)

  val UtcZone: ZoneId                         = ZoneId.of("UTC")
  val MigrationSeparator: String              = "__"
  val AllowedScriptSuffix: String             = ".sql"
  val skipChecksForMigrations                 = config.getStringList("skip_checks").asScala
  val MigrationScriptPrefix: String           = "V"
  val RepeatableScriptPrefix: String          = "R"
  val FlywaySchemaCreationDescription: String = "<< Flyway Schema Creation >>"

  def validateScript(migrationInfo: MigrationInfo): MigrationExecutionResult = {
    val script = migrationInfo.getScript
    script match {
      case x
          if migrationInfo.getType == MigrationType.SCHEMA && migrationInfo.getDescription == FlywaySchemaCreationDescription =>
        MigrationExecutionResult(Success)
      case x if skipChecksForMigrations.contains(script) => MigrationExecutionResult(Success)
      case x if !x.endsWith(AllowedScriptSuffix) =>
        MigrationExecutionResult(Failure, Some(FileSuffix))
      case x if x.startsWith(MigrationScriptPrefix)  => validateMigrationScript(script)
      case x if x.startsWith(RepeatableScriptPrefix) => validateRepeatableScript(script)
      case _                                         => MigrationExecutionResult(Failure, Some(MigrationPrefix))
    }
  }

  def validateRepeatableScript(script: String): MigrationExecutionResult =
    Try {
      val versionWithName = script.split(MigrationSeparator)(1).split("_")(0).toLong
      validateVersion(versionWithName)
    }.getOrElse(MigrationExecutionResult(Failure, Some(MigrationVersion)))

  def validateMigrationScript(script: String): MigrationExecutionResult =
    Try {
      val versionWithName = script.split(MigrationSeparator)(0).drop(1).toLong
      validateVersion(versionWithName)
    }.getOrElse(MigrationExecutionResult(Failure, Some(MigrationVersion)))

  def validateVersion(version: Long): MigrationExecutionResult =
    Try {
      val migrationZonedDateTime = new DateTime(version * 1000).withZone(DateTimeZone.UTC)

      migrationZonedDateTime.isAfter(new DateTime(now().toInstant.getEpochSecond * 1000)) match {
        case true  => MigrationExecutionResult(Failure, Some(MigrationVersion))
        case false => MigrationExecutionResult(Success)
      }
    }.getOrElse(MigrationExecutionResult(Failure, Some(MigrationVersion)))

}
