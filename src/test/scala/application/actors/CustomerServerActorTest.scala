/*
 * Copyright © 2022-2023 by Pervasive Cats S.r.l.s.
 *
 * All Rights Reserved.
 */

package io.github.pervasivecats
package application.actors

import scala.concurrent.duration.DurationInt
import scala.concurrent.duration.FiniteDuration

import io.github.pervasivecats.ValidationError

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.testkit.typed.scaladsl.TestProbe
import akka.actor.typed.ActorRef
import com.dimafeng.testcontainers.JdbcDatabaseContainer.CommonParams
import com.dimafeng.testcontainers.PostgreSQLContainer
import com.dimafeng.testcontainers.scalatest.TestContainerForAll
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigValueFactory
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers.*
import org.testcontainers.utility.DockerImageName

import application.actors.*
import commands.CustomerServerCommand.*
import commands.MessageBrokerCommand.CustomerUnregistered
import commands.RootCommand.Startup
import application.routes.entities.Response.{CustomerResponse, EmptyResponse}
import users.customer.Repository
import users.customer.entities.Customer
import users.customer.valueobjects.{Email, NameComponent}
import users.user.services.PasswordAlgorithm
import users.user.valueobjects.*
import users.customer.Repository.*
import users.customer.entities.CustomerOps.updated
import users.user.services.PasswordAlgorithm.PasswordNotMatching
import commands.{CustomerServerCommand, MessageBrokerCommand, RootCommand}
import application.routes.entities.Response

class CustomerServerActorTest extends AnyFunSpec with TestContainerForAll with BeforeAndAfterAll {

  private val timeout: FiniteDuration = 300.seconds

  override val containerDef: PostgreSQLContainer.Def = PostgreSQLContainer.Def(
    dockerImageName = DockerImageName.parse("postgres:15.1"),
    databaseName = "users",
    username = "test",
    password = "test",
    commonJdbcParams = CommonParams(timeout, timeout, Some("users.sql"))
  )

  private val testKit: ActorTestKit = ActorTestKit()
  private val rootActorProbe: TestProbe[RootCommand] = testKit.createTestProbe[RootCommand]()
  private val messageBrokerProbe: TestProbe[MessageBrokerCommand] = testKit.createTestProbe[MessageBrokerCommand]()
  private val customerResponseProbe: TestProbe[CustomerResponse] = testKit.createTestProbe[CustomerResponse]()
  private val emptyResponseProbe: TestProbe[EmptyResponse] = testKit.createTestProbe[EmptyResponse]()

  @SuppressWarnings(Array("org.wartremover.warts.Var", "scalafix:DisableSyntax.var"))
  private var customerServer: Option[ActorRef[CustomerServerCommand]] = None

  override def afterContainersStart(containers: Containers): Unit =
    customerServer = Some(
      testKit.spawn(
        CustomerServerActor(
          rootActorProbe.ref,
          ConfigFactory
            .load()
            .getConfig("repository")
            .withValue(
              "dataSource.portNumber",
              ConfigValueFactory.fromAnyRef(containers.container.getFirstMappedPort.intValue())
            ),
          messageBrokerProbe.ref
        )
      )
    )

  override def afterAll(): Unit = testKit.shutdownTestKit()

  private val username: Username = Username("mar10").getOrElse(fail())
  private val email: Email = Email("mario@email.com").getOrElse(fail())
  private val firstName: NameComponent = NameComponent("Mario").getOrElse(fail())
  private val lastName: NameComponent = NameComponent("Rossi").getOrElse(fail())
  private val password: PlainPassword = PlainPassword("Password1!").getOrElse(fail())
  private val otherPassword: PlainPassword = PlainPassword("passWORD2?").getOrElse(fail())
  private val customer: Customer = Customer(firstName, lastName, email, username)

  private def checkCustomer(customer: Customer): Unit =
    customerResponseProbe.expectMessageType[CustomerResponse](10.seconds).result match {
      case Left(_) => fail()
      case Right(c) =>
        c.email shouldBe customer.email
        c.lastName shouldBe customer.lastName
        c.firstName shouldBe customer.firstName
        c.username shouldBe customer.username
    }

  describe("A customer server actor") {
    describe("when first started up") {
      it("should notify the root actor of its start") {
        rootActorProbe.expectMessage(10.seconds, Startup(true))
      }
    }
  }

  describe("A customer") {
    describe("after being registered") {
      it("should be present in the database") {
        val server: ActorRef[CustomerServerCommand] = customerServer.getOrElse(fail())
        server ! RegisterCustomer(customer, password, customerResponseProbe.ref)
        checkCustomer(customer)
        server ! LoginCustomer(email, password, customerResponseProbe.ref)
        checkCustomer(customer)
        server ! DeregisterCustomer(email, password, emptyResponseProbe.ref)
        emptyResponseProbe.expectMessage(10.seconds, EmptyResponse(Right[ValidationError, Unit](())))
        messageBrokerProbe.expectMessage(10.seconds, CustomerUnregistered(email))
      }
    }

    describe("after being registered and while trying to get logged in with the wrong password") {
      it("should not be allowed") {
        val server: ActorRef[CustomerServerCommand] = customerServer.getOrElse(fail())
        server ! RegisterCustomer(customer, password, customerResponseProbe.ref)
        checkCustomer(customer)
        server ! LoginCustomer(email, otherPassword, customerResponseProbe.ref)
        customerResponseProbe.expectMessage(10.seconds, CustomerResponse(Left[ValidationError, Customer](PasswordNotMatching)))
        server ! DeregisterCustomer(email, password, emptyResponseProbe.ref)
        emptyResponseProbe.expectMessage(10.seconds, EmptyResponse(Right[ValidationError, Unit](())))
        messageBrokerProbe.expectMessage(10.seconds, CustomerUnregistered(email))
      }
    }

    describe("after being registered and then deleted") {
      it("should not be present in the database") {
        val server: ActorRef[CustomerServerCommand] = customerServer.getOrElse(fail())
        server ! RegisterCustomer(customer, password, customerResponseProbe.ref)
        checkCustomer(customer)
        server ! DeregisterCustomer(email, password, emptyResponseProbe.ref)
        emptyResponseProbe.expectMessage(10.seconds, EmptyResponse(Right[ValidationError, Unit](())))
        messageBrokerProbe.expectMessage(10.seconds, CustomerUnregistered(email))
        server ! LoginCustomer(email, password, customerResponseProbe.ref)
        customerResponseProbe.expectMessage(10.seconds, CustomerResponse(Left[ValidationError, Customer](CustomerNotFound)))
      }
    }

    describe("after being registered and while trying to get deleted from the database with the wrong password") {
      it("should not be allowed") {
        val server: ActorRef[CustomerServerCommand] = customerServer.getOrElse(fail())
        server ! RegisterCustomer(customer, password, customerResponseProbe.ref)
        checkCustomer(customer)
        server ! DeregisterCustomer(email, otherPassword, emptyResponseProbe.ref)
        emptyResponseProbe.expectMessage(10.seconds, EmptyResponse(Left[ValidationError, Unit](PasswordNotMatching)))
        messageBrokerProbe.expectNoMessage(10.seconds)
        server ! DeregisterCustomer(email, password, emptyResponseProbe.ref)
        emptyResponseProbe.expectMessage(10.seconds, EmptyResponse(Right[ValidationError, Unit](())))
        messageBrokerProbe.expectMessage(10.seconds, CustomerUnregistered(email))
      }
    }

    describe("after being registered and then their data gets updated") {
      it("should show the update") {
        val server: ActorRef[CustomerServerCommand] = customerServer.getOrElse(fail())
        val newCustomer: Customer = customer.updated(
          Email("luigi@mail.com").getOrElse(fail()),
          NameComponent("Luigi").getOrElse(fail()),
          NameComponent("Bianchi").getOrElse(fail()),
          Username("l0033gi").getOrElse(fail())
        )
        server ! RegisterCustomer(customer, password, customerResponseProbe.ref)
        checkCustomer(customer)
        server ! UpdateCustomerData(
          email,
          newCustomer.email,
          newCustomer.username,
          newCustomer.firstName,
          newCustomer.lastName,
          customerResponseProbe.ref
        )
        checkCustomer(newCustomer)
        server ! LoginCustomer(newCustomer.email, password, customerResponseProbe.ref)
        checkCustomer(newCustomer)
        server ! DeregisterCustomer(newCustomer.email, password, emptyResponseProbe.ref)
        emptyResponseProbe.expectMessage(10.seconds, EmptyResponse(Right[ValidationError, Unit](())))
        messageBrokerProbe.expectMessage(10.seconds, CustomerUnregistered(newCustomer.email))
      }
    }

    describe("when their data gets updated but they were never registered in the first place") {
      it("should not be allowed") {
        val server: ActorRef[CustomerServerCommand] = customerServer.getOrElse(fail())
        val newCustomer: Customer = customer.updated(
          Email("luigi@mail.com").getOrElse(fail()),
          NameComponent("Luigi").getOrElse(fail()),
          NameComponent("Bianchi").getOrElse(fail()),
          Username("l0033gi").getOrElse(fail())
        )
        server ! UpdateCustomerData(
          email,
          newCustomer.email,
          newCustomer.username,
          newCustomer.firstName,
          newCustomer.lastName,
          customerResponseProbe.ref
        )
        customerResponseProbe.expectMessage(10.seconds, CustomerResponse(Left[ValidationError, Customer](CustomerNotFound)))
      }
    }

    describe("after being registered and their password gets updated") {
      it("should show the update") {
        val server: ActorRef[CustomerServerCommand] = customerServer.getOrElse(fail())
        server ! RegisterCustomer(customer, password, customerResponseProbe.ref)
        checkCustomer(customer)
        server ! UpdateCustomerPassword(email, password, otherPassword, emptyResponseProbe.ref)
        emptyResponseProbe.expectMessage(10.seconds, EmptyResponse(Right[ValidationError, Unit](())))
        server ! LoginCustomer(email, password, customerResponseProbe.ref)
        customerResponseProbe.expectMessage(10.seconds, CustomerResponse(Left[ValidationError, Customer](PasswordNotMatching)))
        server ! LoginCustomer(email, otherPassword, customerResponseProbe.ref)
        checkCustomer(customer)
        server ! DeregisterCustomer(email, otherPassword, emptyResponseProbe.ref)
        emptyResponseProbe.expectMessage(10.seconds, EmptyResponse(Right[ValidationError, Unit](())))
        messageBrokerProbe.expectMessage(10.seconds, CustomerUnregistered(email))
      }
    }

    describe("after being registered and while trying to get their password updated with the wrong old password") {
      it("should not be allowed") {
        val server: ActorRef[CustomerServerCommand] = customerServer.getOrElse(fail())
        server ! RegisterCustomer(customer, password, customerResponseProbe.ref)
        checkCustomer(customer)
        server ! UpdateCustomerPassword(email, otherPassword, password, emptyResponseProbe.ref)
        emptyResponseProbe.expectMessage(10.seconds, EmptyResponse(Left[ValidationError, Unit](PasswordNotMatching)))
        server ! DeregisterCustomer(email, password, emptyResponseProbe.ref)
        emptyResponseProbe.expectMessage(10.seconds, EmptyResponse(Right[ValidationError, Unit](())))
        messageBrokerProbe.expectMessage(10.seconds, CustomerUnregistered(email))
      }
    }

    describe("when their password gets updated but they were never registered in the first place") {
      it("should not be allowed") {
        val server: ActorRef[CustomerServerCommand] = customerServer.getOrElse(fail())
        server ! UpdateCustomerPassword(email, password, otherPassword, emptyResponseProbe.ref)
        emptyResponseProbe.expectMessage(10.seconds, EmptyResponse(Left[ValidationError, Unit](CustomerNotFound)))
      }
    }

    describe("if never registered") {
      it("should not be present") {
        val server: ActorRef[CustomerServerCommand] = customerServer.getOrElse(fail())
        server ! LoginCustomer(email, password, customerResponseProbe.ref)
        customerResponseProbe.expectMessage(10.seconds, CustomerResponse(Left[ValidationError, Customer](CustomerNotFound)))
        server ! DeregisterCustomer(email, password, emptyResponseProbe.ref)
        emptyResponseProbe.expectMessage(10.seconds, EmptyResponse(Left[ValidationError, Unit](CustomerNotFound)))
        messageBrokerProbe.expectNoMessage(10.seconds)
      }
    }

    describe("if already registered") {
      it("should not allow a new registration") {
        val server: ActorRef[CustomerServerCommand] = customerServer.getOrElse(fail())
        server ! RegisterCustomer(customer, password, customerResponseProbe.ref)
        checkCustomer(customer)
        server ! RegisterCustomer(customer, password, customerResponseProbe.ref)
        customerResponseProbe.expectMessage(10.seconds, CustomerResponse(Left[ValidationError, Customer](CustomerAlreadyPresent)))
      }
    }
  }
}
