/*
 * Copyright © 2022-2023 by Pervasive Cats S.r.l.s.
 *
 * All Rights Reserved.
 */

package io.github.pervasivecats
package users.storemanager

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
import users.user.valueobjects.{EncryptedPassword, PlainPassword, Username}
import users.storemanager.valueobjects.Store
import users.storemanager.entities.StoreManager
import users.ValidationError

import io.github.pervasivecats.users.user.services.PasswordAlgorithm

class StoreManagerRepositoryTest extends AnyFunSpec with TestContainerForAll {

  val usernameString: String = "matteo"
  val plainPassword: String = "Password1!"
  val storeID: Long = 10

  val timeout: FiniteDuration = FiniteDuration(240, SECONDS)

  val initScriptParam: CommonParams =
    CommonParams(startupTimeout = timeout, connectTimeout = timeout, initScriptPath = Option("users.sql"))

  override val containerDef: PostgreSQLContainer.Def = PostgreSQLContainer.Def(
    dockerImageName = DockerImageName.parse("postgres:15.1"),
    databaseName = "users",
    username = "test",
    password = "test",
    commonJdbcParams = initScriptParam
  )

  @SuppressWarnings(Array("org.wartremover.warts.AutoUnboxing"))
  override def afterContainersStart(container: Containers): Unit = {
    super.afterContainersStart(container)

    container match {
      case c: PostgreSQLContainer => StoreManagerRepository(c.container.getFirstMappedPort)
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

  describe("The store manager repository") {
    describe("when asked to register a store manager") {
      it("should add the entry to the database") {
        withContainers { _ =>
          for {
            username <- Username(usernameString)
            store <- Store(storeID)
          } do {
            val result = StoreManagerRepository
              .getInstance()
              .getOrElse(fail())
              .register(
                StoreManager(username, store),
                summon[PasswordAlgorithm].encrypt(PlainPassword(plainPassword).getOrElse(fail()))
              )
            result shouldBe Right[ValidationError, Unit](println("store manager added"))
          }
        }
      }
    }

    describe("when asked to retrieve the store manager corresponding to a username") {
      it("should return the requested store manager") {
        withContainers { _ =>
          for {
            username <- Username(usernameString)
          } do {
            val result = StoreManagerRepository.getInstance().getOrElse(fail()).findByUsername(username)
            (result.getOrElse(fail()).username.value.value: String) shouldBe usernameString
            (result.getOrElse(fail()).store.value.value: Long) shouldBe storeID
          }
        }
      }
    }
  }
}
