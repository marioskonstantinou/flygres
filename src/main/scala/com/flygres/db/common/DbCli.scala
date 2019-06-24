package com.flygres.db.common

case class DbCli(environment: String = "unknown",
                 schemas: Seq[String] = Seq(),
                 action: String = "migrate")

object DbCli {
  val argumentsParser = new scopt.OptionParser[DbCli]("DbMigration") {
    head("Database migration job arguments")

    override def errorOnUnknownArgument: Boolean = true

    override def showUsageOnError = Some(true)

    opt[String]("environment")
      .required()
      .valueName("local")
      .action { (input, config) =>
        config.copy(environment = input)
      }
      .text("Environment")

    opt[Seq[String]]("schemas")
      .optional()
      .valueName("public")
      .action { (input, config) =>
        config.copy(schemas = input)
      }
      .text("Schemas to migrate")

    opt[String]("action")
      .required()
      .valueName("[migrate, repair]")
      .action { (input, config) =>
        config.copy(action = input)
      }
      .text("Migration action")

  }
}
