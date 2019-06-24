package com.flygres.db.common

import com.typesafe.config.{Config, ConfigFactory}

trait Configuration {

  def environment: String

  @transient lazy val config: Config = ConfigFactory.load.getConfig(environment)

}
