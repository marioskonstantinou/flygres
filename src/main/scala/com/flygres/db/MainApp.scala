package com.flygres.db

import com.flygres.db.common.{Configuration, DbCli}
import com.flygres.db.models.{ActionType, DBMigrationConfiguration}
import com.flygres.db.services.FlywayService
import com.typesafe.scalalogging.StrictLogging
import org.flywaydb.core.api.MigrationInfoService

class MainApp(args: DbCli) extends Configuration with StrictLogging {
  override def environment: String           = args.environment
  private lazy val schemasToRun: Seq[String] = args.schemas
  private lazy val migrationType             = ActionType.fromString(args.action)

  private def prepareConfiguration: DBMigrationConfiguration =
    DBMigrationConfiguration.prepareConfiguration(config, migrationType, false)

  def migrate: Seq[MigrationInfoService] = {
    val flywayService = new FlywayService
    val migrationConfiguration = schemasToRun.isEmpty match {
      case true  => prepareConfiguration
      case false => prepareConfiguration.copy(checkSchemas = schemasToRun)
    }
    flywayService.runMigrations(migrationConfiguration)
  }

}

object MainApp extends App with StrictLogging {

  import DbCli._

  argumentsParser.parse(args, DbCli()) match {
    case Some(arguments) =>
      val dbMigrator = new MainApp(arguments)
      dbMigrator.migrate
    case None =>
      logger.error("No arguments provided")
      sys.error("No arguments provided")
  }

}
