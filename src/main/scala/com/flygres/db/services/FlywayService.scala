package com.flygres.db.services

import java.io.File
import java.util.Properties

import com.flygres.db.models.{DBMigrationConfiguration, Migrate, Repair}
import com.typesafe.scalalogging.StrictLogging
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.MigrationInfoService
import org.flywaydb.core.internal.util.jdbc.DriverDataSource

import scala.util.Try

class FlywayService extends StrictLogging {

  private val ParentDBSchemas            = sys.env.getOrElse("MIGRATIONS_ROOT", "./db")
  private val MigrationDirectory: String = "migrations"
  private val ViewsDirectory: String     = "views"

  /**
    * Prepares a list of available schemas and executes migration process for each schema
    *
    * @param databaseConfiguration
    * @return
    */
  def runMigrations(databaseConfiguration: DBMigrationConfiguration): Seq[MigrationInfoService] = {
    val sortedFilteredSchemas: Seq[String] =
      filterAndSortSchemas(databaseConfiguration)

    sortedFilteredSchemas.foldLeft(Seq[MigrationInfoService]())((acc, schema) => {
      acc :+ migrateForSchema(databaseConfiguration,
                              schema,
        Map())
    })

  }

  def migrateForSchema(databaseConfiguration: DBMigrationConfiguration,
                       currentSchema: String,
                       placeholders: Map[String, String]): MigrationInfoService = {
    val flyway = new Flyway

    val migrationSources: Seq[String] = migrationsLocations(currentSchema)
    logger.info(migrationSources.mkString(", "))

    flyway.setLocations(migrationSources: _*)
    flyway.setDataSource(
      new DriverDataSource(
        Thread.currentThread().getContextClassLoader,
        databaseConfiguration.driver,
        databaseConfiguration.url,
        databaseConfiguration.user,
        databaseConfiguration.password,
        new Properties()
      ))

    placeholders.foreach { entry =>
      val (key, value) = entry
      flyway.getPlaceholders.put(key, value)
    }

    flyway.setSchemas(currentSchema)
    // true for `local` environment (see application.conf)
    flyway.setOutOfOrder(databaseConfiguration.outOfOrder)

    databaseConfiguration.actionType match {
      case Migrate => flyway.migrate()
      case Repair  => flyway.repair()
      case _       => logger.error("Could not identify action type")
    }

    flyway.info()
  }

  /**
    * Filter and sort schemas based on current directories (schemas) found and [[DBMigrationConfiguration.checkSchemas]]
    *
    * @param dbMigrationConfiguration
    * @return
    */
  def filterAndSortSchemas(dbMigrationConfiguration: DBMigrationConfiguration): Seq[String] = {
    val migrationRoot = new File(ParentDBSchemas)
    val unsortedDbNames = migrationRoot
      .list((file, name) => isMigrationDirectory(new File(file, name)))
      .toSeq
      .filter(dbMigrationConfiguration.checkSchemas.contains(_))

    val sortedSchemas: Seq[String] = unsortedDbNames.sortWith(
      (left, right) =>
        dbMigrationConfiguration.checkSchemas.indexOf(left) < dbMigrationConfiguration.checkSchemas
          .indexOf(right))
    logger.info(sortedSchemas.mkString(", "))
    sortedSchemas
  }

  /**
    * Populate migration locations based on passed `dbName` (schema)
    *
    * {{{
    *   Adds default initial population script for `public` schema
    * }}}
    *
    * @param dbName - current schema
    * @return - A list of available locations to migrate
    */
  def migrationsLocations(dbName: String): Seq[String] = {
    val locations = dbName match {
      case "public" => Seq("filesystem:db/initial")
      case _     => Seq[String]()
    }

    val migrationsWithViewsPaths: Seq[String] =
      Seq(migrationsPath(dbName), viewsPath(dbName)).flatten
    locations ++ migrationsWithViewsPaths
  }

  private def isMigrationDirectory(file: File): Boolean =
    file.isDirectory && new File(file, MigrationDirectory).isDirectory

  private def viewsPath(dbName: String): Option[String] = extractPath(dbName, ViewsDirectory)

  private def migrationsPath(dbName: String): Option[String] =
    extractPath(dbName, MigrationDirectory)

  private def extractPath(dbName: String, directoryType: String): Option[String] = {
    Try {
      val pathPrefix = s"$ParentDBSchemas/"
      val path       = new File(s"$pathPrefix$dbName", directoryType)
      path.list().length > 0 match {
        case true  => Some(s"filesystem:${path.getPath.replace(pathPrefix, "db/")}")
        case false => None
      }
    }.getOrElse(None)
  }

}
