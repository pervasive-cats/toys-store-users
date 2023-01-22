/*
 * Copyright © 2022-2023 by Pervasive Cats S.r.l.s.
 *
 * All Rights Reserved.
 */

package io.github.pervasivecats
package users.administration

import java.sql.DriverManager

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration.SECONDS

import com.dimafeng.testcontainers.JdbcDatabaseContainer.CommonParams
import com.dimafeng.testcontainers.PostgreSQLContainer
import com.dimafeng.testcontainers.scalatest.TestContainerForAll
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import eu.timepit.refined.auto.given
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

  val rightUsername: Username = Username("elena").getOrElse(fail())

  describe("An administration") {

    describe("when asked to retrieve the administration corresponding to a username") {
      it("should return the requested administration") {
        val db: Repository = repository.getOrElse(fail())

        db.findByUsername(rightUsername).getOrElse(fail()).username shouldBe rightUsername
      }
    }

    val wrongUsername: Username = Username("nonelena").getOrElse(fail())

    describe("when asked to retrieve a non-existent administration account") {
      it("should return UserNotFound") {
        val db: Repository = repository.getOrElse(fail())

        db.findByUsername(wrongUsername) shouldBe Left[ValidationError, Administration](AdministrationNotFound)
      }
    }

    val newPlainPassword: PlainPassword = PlainPassword("PyW$s1sC").getOrElse(fail())
    val encryptedNewPassword: EncryptedPassword = summon[PasswordAlgorithm].encrypt(newPlainPassword).getOrElse(fail())

    describe("when asked to update the password of an administration account") {
      it("should update the specified password") {
        val db: Repository = repository.getOrElse(fail())

        db.updatePassword(Administration(rightUsername), encryptedNewPassword).getOrElse(fail())

        val updatedPassword = db.findPassword(Administration(rightUsername)).getOrElse(fail())

        summon[PasswordAlgorithm].check(updatedPassword, newPlainPassword) shouldBe true
      }
    }

    describe("when asked to update the password of a non-existent administration account") {
      it("should return OperationFailed") {
        val db: Repository = repository.getOrElse(fail())

        db.updatePassword(Administration(wrongUsername), encryptedNewPassword) shouldBe Left[ValidationError, Unit](
          OperationFailed
        )
      }
    }

    describe("when asked to retrieve the password of an administration account") {
      it("should return the requested administration password") {
        val db: Repository = repository.getOrElse(fail())

        db.updatePassword(Administration(rightUsername), encryptedNewPassword)
        db.findPassword(Administration(rightUsername)).getOrElse(fail()) shouldBe encryptedNewPassword
      }
    }

    describe("when asked to retrieve the password of a non-existent administration account") {
      it("should return UserNotFound") {
        val db: Repository = repository.getOrElse(fail())

        db.findPassword(Administration(wrongUsername)) shouldBe Left[ValidationError, Administration](AdministrationNotFound)
      }
    }
  }
}
