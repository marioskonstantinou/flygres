import com.typesafe.config.{Config, ConfigFactory}

trait IntegrationTestKit {
  def config: Config =
    ConfigFactory
      .load("application.test.conf")
      .getConfig("local_integration_test")
}
