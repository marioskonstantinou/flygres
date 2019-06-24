import com.flygres.db.models.{DBMigrationConfiguration, Migrate}
import com.whisk.docker.{DockerContainer, DockerKit, DockerReadyChecker}

/**
  * Creates docker postgres instance
  */
trait DockerPostgresService extends DockerKit with IntegrationTestKit {

  import scala.concurrent.duration._

  override val PullImagesTimeout: FiniteDuration      = 120.minutes
  override val StartContainersTimeout: FiniteDuration = 500.seconds
  override val StopContainersTimeout: FiniteDuration  = 10.seconds

  private val PostgresUser: String        = config.getString("username")
  private val PostgresPassword: String    = config.getString("password")
  private val PostgresAdvertisedPort: Int = config.getInt("port") - 1
  private val PostgresExposedPort: Int    = config.getInt("port")

  protected val PostgresConfiguration: DBMigrationConfiguration =
    DBMigrationConfiguration.prepareConfiguration(config, Migrate, false)

  val postgresContainer: DockerContainer = DockerContainer("postgres:9.6.14")
    .withPorts(PostgresAdvertisedPort -> Some(PostgresExposedPort))
    .withEnv("POSTGRES_DB=postgres",s"POSTGRES_USER=$PostgresUser", s"POSTGRES_PASSWORD=$PostgresPassword")
    .withReadyChecker(
      DockerReadyChecker.LogLineContains("PostgreSQL init process complete; ready for start up"))

  abstract override def dockerContainers: List[DockerContainer] =
    postgresContainer :: super.dockerContainers

}
