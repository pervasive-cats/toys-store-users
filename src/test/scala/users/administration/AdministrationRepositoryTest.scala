package io.github.pervasivecats
package users.administration

import eu.timepit.refined.auto.given
import com.dimafeng.testcontainers.PostgreSQLContainer
import com.dimafeng.testcontainers.JdbcDatabaseContainer.CommonParams
import com.dimafeng.testcontainers.scalatest.TestContainerForAll
import org.testcontainers.utility.DockerImageName
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers.*
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import java.sql.DriverManager
import scala.concurrent.duration.{FiniteDuration, SECONDS}

import users.user.valueobjects.{EncryptedPassword, Username}
import users.ValidationError

class AdministrationRepositoryTest  extends AnyFunSpec with TestContainerForAll{

  val usernameString: String = "elena"
  val newPassword: String = "PyW$s1sC"

  val timeout: FiniteDuration = FiniteDuration(300, SECONDS)

  val initScriptParam: CommonParams =
    CommonParams(startupTimeout = timeout, connectTimeout = timeout, initScriptPath = Option("users.sql"))

  override val containerDef: PostgreSQLContainer.Def = PostgreSQLContainer.Def(
    dockerImageName = DockerImageName.parse("postgres:15.1"),
    databaseName = "users",
    username = "ismam",
    password = "ismam",
    commonJdbcParams = initScriptParam
  )

  @SuppressWarnings(Array("org.wartremover.warts.AutoUnboxing"))
  override def afterContainersStart(container: Containers): Unit = {
    super.afterContainersStart(container)

    container match {
      case c: PostgreSQLContainer => AdministrationRepository(c.container.getFirstMappedPort)
    }
  }

  describe("A PostgreSQL container") {
    describe("when started") {
      it("should stay connected") {
        withContainers { pgContainer =>
          Class.forName(pgContainer.driverClassName)
          val connection = DriverManager.getConnection(pgContainer.jdbcUrl, pgContainer.username, pgContainer.password)
          assert(!connection.isClosed)
        }
      }
    }
  }



}
