/*
 * Copyright © 2022-2023 by Pervasive Cats S.r.l.s.
 *
 * All Rights Reserved.
 */

package io.github.pervasivecats
package users.customer

import java.nio.file.attribute.UserPrincipalNotFoundException

import scala.concurrent.duration.DurationInt
import scala.concurrent.duration.FiniteDuration

import com.dimafeng.testcontainers.JdbcDatabaseContainer.CommonParams
import com.dimafeng.testcontainers.PostgreSQLContainer
import com.dimafeng.testcontainers.scalatest.TestContainerForAll
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigValueFactory
import org.scalatest.EitherValues.given
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers.*
import org.testcontainers.utility.DockerImageName

import users.customer.entities.Customer
import users.customer.valueobjects.{Email, NameComponent}
import users.customer.Repository.{CustomerAlreadyPresent, CustomerNotFound, OperationFailed}
import users.user.services.PasswordAlgorithm
import users.user.valueobjects.*
import users.customer.entities.CustomerOps.updated

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

  private val username: Username = Username("mar10").getOrElse(fail())
  private val email: Email = Email("mario@email.com").getOrElse(fail())
  private val firstName: NameComponent = NameComponent("Mario").getOrElse(fail())
  private val lastName: NameComponent = NameComponent("Rossi").getOrElse(fail())

  private val password: EncryptedPassword =
    summon[PasswordAlgorithm].encrypt(PlainPassword("Password1!").getOrElse(fail())).getOrElse(fail())
  private val customer: Customer = Customer(firstName, lastName, email, username)

  describe("A customer") {
    describe("after being registered") {
      it("should be present in the database") {
        val db: Repository = repository.getOrElse(fail())
        db.register(customer, password).getOrElse(fail())
        val databaseCustomer: Customer = db.findByEmail(email).getOrElse(fail())
        databaseCustomer.email shouldBe customer.email
        databaseCustomer.firstName shouldBe customer.firstName
        databaseCustomer.lastName shouldBe customer.lastName
        databaseCustomer.username shouldBe customer.username
        db.findPassword(customer).value shouldBe password
        db.deregister(customer).getOrElse(fail())
      }
    }

    describe("after being registered and then deleted") {
      it("should not be present in the database") {
        val db: Repository = repository.getOrElse(fail())
        db.register(customer, password).getOrElse(fail())
        db.deregister(customer).getOrElse(fail())
        db.findByEmail(email).left.value shouldBe CustomerNotFound
        db.findPassword(customer).left.value shouldBe CustomerNotFound
      }
    }

    describe("after being registered and then their data gets updated") {
      it("should show the update") {
        val db: Repository = repository.getOrElse(fail())
        val newCustomer: Customer = customer.updated(
          Email("luigi@mail.com").getOrElse(fail()),
          NameComponent("Luigi").getOrElse(fail()),
          NameComponent("Bianchi").getOrElse(fail()),
          Username("l0033gi").getOrElse(fail())
        )
        db.register(customer, password).getOrElse(fail())
        db.updateData(
          customer,
          newCustomer.firstName,
          newCustomer.lastName,
          newCustomer.email,
          newCustomer.username
        ).getOrElse(fail())
        val newDatabaseCustomer: Customer = db.findByEmail(newCustomer.email).getOrElse(fail())
        newDatabaseCustomer.email shouldBe newCustomer.email
        newDatabaseCustomer.firstName shouldBe newCustomer.firstName
        newDatabaseCustomer.lastName shouldBe newCustomer.lastName
        newDatabaseCustomer.username shouldBe newCustomer.username
        db.deregister(newCustomer).getOrElse(fail())
      }
    }

    describe("when their data gets updated but they were never registered in the first place") {
      it("should not be allowed") {
        val db: Repository = repository.getOrElse(fail())
        val newCustomer: Customer = customer.updated(
          Email("luigi@mail.com").getOrElse(fail()),
          NameComponent("Luigi").getOrElse(fail()),
          NameComponent("Bianchi").getOrElse(fail()),
          Username("l0033gi").getOrElse(fail())
        )
        db.updateData(
          customer,
          newCustomer.firstName,
          newCustomer.lastName,
          newCustomer.email,
          newCustomer.username
        ).left.value shouldBe OperationFailed
      }
    }

    describe("after being registered and their password gets updated") {
      it("should show the update") {
        val db: Repository = repository.getOrElse(fail())
        val newPassword: EncryptedPassword =
          summon[PasswordAlgorithm]
            .encrypt(PlainPassword("passWORD2?").getOrElse(fail()))
            .getOrElse(fail())
        db.register(customer, password).getOrElse(fail())
        db.updatePassword(customer, newPassword).getOrElse(fail())
        db.findPassword(customer).value shouldBe newPassword
        db.deregister(customer).getOrElse(fail())
      }
    }

    describe("when their password gets updated but they were never registered in the first place") {
      it("should not be allowed") {
        val db: Repository = repository.getOrElse(fail())
        val newPassword: EncryptedPassword =
          summon[PasswordAlgorithm]
            .encrypt(PlainPassword("passWORD2?").getOrElse(fail()))
            .getOrElse(fail())
        db.updatePassword(customer, newPassword).left.value shouldBe OperationFailed
      }
    }

    describe("if never registered") {
      it("should not be present") {
        val db: Repository = repository.getOrElse(fail())
        db.findByEmail(email).left.value shouldBe CustomerNotFound
        db.findPassword(customer).left.value shouldBe CustomerNotFound
        db.deregister(customer).left.value shouldBe OperationFailed
      }
    }

    describe("if already registered") {
      it("should not allow a new registration") {
        val db: Repository = repository.getOrElse(fail())
        db.register(customer, password).getOrElse(fail())
        db.register(customer, password).left.value shouldBe CustomerAlreadyPresent
      }
    }
  }
}
