/*
 * Copyright © 2022-2023 by Pervasive Cats S.r.l.s.
 *
 * All Rights Reserved.
 */

package io.github.pervasivecats
package users.storemanager

import java.sql.DriverManager

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration.SECONDS

import com.dimafeng.testcontainers.JdbcDatabaseContainer.CommonParams
import com.dimafeng.testcontainers.PostgreSQLContainer
import com.dimafeng.testcontainers.scalatest.TestContainerForAll
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import eu.timepit.refined.auto.given
import org.scalatest.EitherValues.given
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers.*
import org.testcontainers.utility.DockerImageName

import users.user.valueobjects.{EncryptedPassword, PlainPassword, Username}
import users.storemanager.Repository
import users.storemanager.Repository.{StoreManagerAlreadyPresent, StoreManagerNotFound, OperationFailed}
import users.storemanager.valueobjects.Store
import users.storemanager.entities.StoreManager
import users.ValidationError
import users.user.services.PasswordAlgorithm

class RepositoryTest extends AnyFunSpec with TestContainerForAll {

  private val timeout: FiniteDuration = FiniteDuration(300, SECONDS)

  override val containerDef: PostgreSQLContainer.Def = PostgreSQLContainer.Def(
    dockerImageName = DockerImageName.parse("postgres:15.1"),
    databaseName = "users",
    username = "test",
    password = "test",
    commonJdbcParams = CommonParams(timeout, timeout, Some("users.sql"))
  )

  @SuppressWarnings(Array("org.wartremover.warts.Var", "scalafix:DisableSyntax.var"))
  private var repository: Option[Repository] = None

  override def afterContainersStart(containers: Containers): Unit =
    repository = Some(Repository.withPort(containers.container.getFirstMappedPort.intValue()))

  private val usernameString: String = "matteo"
  private val storeId: Long = 10
  private val newStoreId: Long = 99

  private val username: Username = Username(usernameString).getOrElse(fail())
  private val wrongUsername: Username = Username("nonmatteo").getOrElse(fail())
  private val store: Store = Store(storeId).getOrElse(fail())
  private val newStore: Store = Store(newStoreId).getOrElse(fail())

  private val password: EncryptedPassword =
    summon[PasswordAlgorithm].encrypt(PlainPassword("Password1!").getOrElse(fail())).getOrElse(fail())

  private val newPassword: EncryptedPassword =
    summon[PasswordAlgorithm].encrypt(PlainPassword("NewPassword1!").getOrElse(fail())).getOrElse(fail())

  describe("The store manager repository") {
    describe("when asked to register a store manager") {
      it("should add the entry to the database") {
        repository
          .getOrElse(fail())
          .register(
            StoreManager(username, store),
            password
          ) shouldBe Right[ValidationError, Unit](())
      }
    }

    describe("when asked to register a store manager that already exists") {
      it("should return StoreManagerAlreadyPresent") {
        repository
          .getOrElse(fail())
          .register(
            StoreManager(username, store),
            password
          ) shouldBe Left[ValidationError, Unit](StoreManagerAlreadyPresent)
      }
    }

    describe("when asked to retrieve the store manager corresponding to a username") {
      it("should return the requested store manager") {
        val result = repository.getOrElse(fail()).findByUsername(username)
        (result.getOrElse(fail()).username.value.value: String) shouldBe usernameString
        (result.getOrElse(fail()).store.id.value: Long) shouldBe storeId
      }
    }

    describe("when asked to retrieve a non-existent store manager") {
      it("should return StoreManagerNotFound") {
        repository.getOrElse(fail()).findByUsername(wrongUsername) shouldBe Left[ValidationError, StoreManager](
          StoreManagerNotFound
        )
      }
    }

    describe("when asked to update a store manager's store") {
      it("should correctly update the store") {
        repository.getOrElse(fail()).updateStore(StoreManager(username, store), newStore) shouldBe Right[
          ValidationError,
          Unit
        ](())

        (repository.getOrElse(fail()).findByUsername(username).getOrElse(fail()).store.id.value: Long) shouldBe newStoreId
      }
    }

    describe("when asked to update a non-existent store manager's store") {
      it("should return OperationFailed") {
        repository.getOrElse(fail()).updateStore(StoreManager(wrongUsername, store), newStore) shouldBe Left[
          ValidationError,
          Unit
        ](
          OperationFailed
        )
      }
    }

    describe("when asked to retrieve a store manager's password") {
      it("should return the requested password") {
        repository.getOrElse(fail()).findPassword(StoreManager(username, store)) shouldBe Right[
          ValidationError,
          EncryptedPassword
        ](password)
      }
    }

    describe("when asked to retrieve a non-existent store manager's password") {
      it("should return StoreManagerNotFound") {
        repository.getOrElse(fail()).findPassword(StoreManager(wrongUsername, store)) shouldBe Left[
          ValidationError,
          EncryptedPassword
        ](StoreManagerNotFound)
      }
    }

    describe("when asked to update a store manager's password") {
      it("should correctly update the password") {
        repository.getOrElse(fail()).updatePassword(StoreManager(username, store), newPassword) shouldBe Right[
          ValidationError,
          Unit
        ](())

        repository.getOrElse(fail()).findPassword(StoreManager(username, store)).value shouldBe newPassword
      }
    }

    describe("when asked to update a non-existent store manager's password") {
      it("should return OperationFailed") {
        repository.getOrElse(fail()).updatePassword(StoreManager(wrongUsername, store), newPassword) shouldBe Left[
          ValidationError,
          EncryptedPassword
        ](OperationFailed)
      }
    }

    describe("when asked to delete a store manager") {
      it("should delete the specified store manager") {
        repository.getOrElse(fail()).unregister(StoreManager(username, store)) shouldBe Right[ValidationError, Unit](())
        repository.getOrElse(fail()).findByUsername(username) shouldBe Left[ValidationError, StoreManager](StoreManagerNotFound)
      }
    }

    describe("when asked to delete a non-existent store manager") {
      it("should return OperationFailed") {
        repository.getOrElse(fail()).unregister(StoreManager(wrongUsername, store)) shouldBe Left[ValidationError, StoreManager](
          OperationFailed
        )
      }
    }
  }
}
