/*
 * Copyright © 2022-2023 by Pervasive Cats S.r.l.s.
 *
 * All Rights Reserved.
 */

package io.github.pervasivecats
package users.administration

import scala.concurrent.duration.DurationInt
import scala.concurrent.duration.FiniteDuration

import com.dimafeng.testcontainers.JdbcDatabaseContainer.CommonParams
import com.dimafeng.testcontainers.PostgreSQLContainer
import com.dimafeng.testcontainers.scalatest.TestContainerForAll
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigValueFactory
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import eu.timepit.refined.auto.given
import org.scalatest.EitherValues.given
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers.*
import org.testcontainers.utility.DockerImageName

import users.user.valueobjects.{EncryptedPassword, PlainPassword, Username}
import users.ValidationError
import users.administration.entities.Administration
import users.administration.Repository
import users.user.services.PasswordAlgorithm
import users.administration.Repository.{AdministrationNotFound, OperationFailed}

class RepositoryTest extends AnyFunSpec with TestContainerForAll {

  private val timeout: FiniteDuration = 300.seconds

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
    repository = Some(
      Repository(
        ConfigFactory
          .load()
          .getConfig("repository")
          .withValue("dataSource.portNumber", ConfigValueFactory.fromAnyRef(containers.container.getFirstMappedPort.intValue()))
      )
    )

  private val rightUsername: Username = Username("elena").getOrElse(fail())

  describe("An administration repository") {
    describe("when asked to retrieve the administration account corresponding to a username") {
      it("should return the corresponding account") {
        repository.getOrElse(fail()).findByUsername(rightUsername).getOrElse(fail()).username shouldBe rightUsername
      }
    }

    val wrongUsername: Username = Username("nonelena").getOrElse(fail())

    describe("when asked to retrieve a non-existent administration account") {
      it("should return UserNotFound") {
        repository
          .getOrElse(fail())
          .findByUsername(wrongUsername) shouldBe Left[ValidationError, Administration](AdministrationNotFound)
      }
    }

    val newPlainPassword: PlainPassword = PlainPassword("PyW$s1sC").getOrElse(fail())
    val encryptedNewPassword: EncryptedPassword = summon[PasswordAlgorithm].encrypt(newPlainPassword).getOrElse(fail())

    describe("when asked to update the password of an administration account") {
      it("should update the specified password") {
        repository.getOrElse(fail()).updatePassword(Administration(rightUsername), encryptedNewPassword).getOrElse(fail())
        val updatedPassword = repository.getOrElse(fail()).findPassword(Administration(rightUsername)).getOrElse(fail())
        summon[PasswordAlgorithm].check(updatedPassword, newPlainPassword).value shouldBe ()
      }
    }

    describe("when asked to update the password of a non-existent administration account") {
      it("should return OperationFailed") {
        repository
          .getOrElse(fail())
          .updatePassword(Administration(wrongUsername), encryptedNewPassword) shouldBe Left[ValidationError, Unit](
            OperationFailed
          )
      }
    }

    describe("when asked to retrieve the password of an administration account") {
      it("should return the requested administration password") {
        repository.getOrElse(fail()).updatePassword(Administration(rightUsername), encryptedNewPassword).getOrElse(fail())
        repository.getOrElse(fail()).findPassword(Administration(rightUsername)).getOrElse(fail()) shouldBe encryptedNewPassword
      }
    }

    describe("when asked to retrieve the password of a non-existent administration account") {
      it("should return UserNotFound") {
        repository
          .getOrElse(fail())
          .findPassword(Administration(wrongUsername)) shouldBe Left[ValidationError, Administration](AdministrationNotFound)
      }
    }
  }
}
