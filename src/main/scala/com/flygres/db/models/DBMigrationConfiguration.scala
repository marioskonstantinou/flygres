package com.flygres.db.models

import com.typesafe.config.Config
import scala.collection.JavaConverters._

class InvalidActionTypeException(action: String)
    extends RuntimeException(
      s"Invalid migration action provided ($action). Allowed actions (migrate, repair)")

object ActionType {
  def fromString(action: String): ActionType = action match {
    case "migrate" => Migrate
    case "repair"  => Repair
    case _         => throw new InvalidActionTypeException(action)
  }
}

sealed trait ActionType
case object Migrate extends ActionType
case object Repair  extends ActionType

case class DBMigrationConfiguration(
    driver: String,
    url: String,
    user: String,
    password: String,
    port: Int,
    outOfOrder: Boolean,
    checkSchemas: Seq[String],
    actionType: ActionType
)

object DBMigrationConfiguration {
  def prepareConfiguration(config: Config,
                           actionType: ActionType,
                           outOfOrder: Boolean = false): DBMigrationConfiguration = {
    val dbUser: String                = config.getString("username")
    val dbPassword: String            = config.getString("password")
    val dbDriver: String              = config.getString("driver")
    val dbUrl: String                 = config.getString("url")
    val dbPort: Int                   = config.getInt("port")
    val checkSchemas: Seq[String]     = config.getStringList("check_schemas").asScala

    DBMigrationConfiguration(
      dbDriver,
      dbUrl,
      dbUser,
      dbPassword,
      dbPort,
      outOfOrder,
      checkSchemas,
      actionType
    )
  }
}
