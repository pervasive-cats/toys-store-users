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
import application.actors.AdministrationServerCommand.*
import application.actors.RootCommand.Startup
import application.routes.Response
import application.routes.Response.{AdministrationResponse, EmptyResponse}
import users.user.services.PasswordAlgorithm
import users.user.valueobjects.*
import users.administration.Repository.*
import users.user.services.PasswordAlgorithm.PasswordNotMatching

import users.administration.entities.Administration

class AdministrationServerActorTest extends AnyFunSpec with TestContainerForAll with BeforeAndAfterAll {

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
  private val administrationResponseProbe: TestProbe[AdministrationResponse] = testKit.createTestProbe[AdministrationResponse]()
  private val emptyResponseProbe: TestProbe[EmptyResponse] = testKit.createTestProbe[EmptyResponse]()

  @SuppressWarnings(Array("org.wartremover.warts.Var", "scalafix:DisableSyntax.var"))
  private var administrationServer: Option[ActorRef[AdministrationServerCommand]] = None

  override def afterContainersStart(containers: Containers): Unit =
    administrationServer = Some(
      testKit.spawn(
        AdministrationServerActor(
          rootActorProbe.ref,
          ConfigFactory
            .load()
            .getConfig("repository")
            .withValue(
              "dataSource.portNumber",
              ConfigValueFactory.fromAnyRef(containers.container.getFirstMappedPort.intValue())
            )
        )
      )
    )

  override def afterAll(): Unit = testKit.shutdownTestKit()

  private val username: Username = Username("elena").getOrElse(fail())
  private val otherUsername: Username = Username("mar10").getOrElse(fail())
  private val password: PlainPassword = PlainPassword("Password1!").getOrElse(fail())
  private val otherPassword: PlainPassword = PlainPassword("passWORD2?").getOrElse(fail())
  private val administration: Administration = Administration(username)

  describe("A store manager server actor") {
    describe("when first started up") {
      it("should notify the root actor of its start") {
        rootActorProbe.expectMessage(10.seconds, Startup(true))
      }
    }
  }

  describe("An administration account") {
    describe("when the system is first started up") {
      it("should be already present in the database") {
        val server: ActorRef[AdministrationServerCommand] = administrationServer.getOrElse(fail())
        server ! LoginAdministration(username, password, administrationResponseProbe.ref)
        administrationResponseProbe.expectMessage(
          10.seconds,
          AdministrationResponse(Right[ValidationError, Administration](administration))
        )
      }
    }

    describe("trying to get logged in with the wrong password") {
      it("should not be allowed to log in") {
        val server: ActorRef[AdministrationServerCommand] = administrationServer.getOrElse(fail())
        server ! LoginAdministration(username, otherPassword, administrationResponseProbe.ref)
        administrationResponseProbe.expectMessage(
          10.seconds,
          AdministrationResponse(Left[ValidationError, Administration](PasswordNotMatching))
        )
      }
    }

    describe("while trying to get its password updated with the wrong password") {
      it("should not be allowed") {
        val server: ActorRef[AdministrationServerCommand] = administrationServer.getOrElse(fail())
        server ! UpdateAdministrationPassword(username, otherPassword, password, emptyResponseProbe.ref)
        emptyResponseProbe.expectMessage(10.seconds, EmptyResponse(Left[ValidationError, Unit](PasswordNotMatching)))
      }
    }

    describe("after its password gets updated") {
      it("should show the update") {
        val server: ActorRef[AdministrationServerCommand] = administrationServer.getOrElse(fail())
        server ! UpdateAdministrationPassword(username, password, otherPassword, emptyResponseProbe.ref)
        emptyResponseProbe.expectMessage(10.seconds, EmptyResponse(Right[ValidationError, Unit](())))
        server ! LoginAdministration(username, password, administrationResponseProbe.ref)
        administrationResponseProbe.expectMessage(
          10.seconds,
          AdministrationResponse(Left[ValidationError, Administration](PasswordNotMatching))
        )
        server ! LoginAdministration(username, otherPassword, administrationResponseProbe.ref)
        administrationResponseProbe.expectMessage(
          10.seconds,
          AdministrationResponse(Right[ValidationError, Administration](administration))
        )
      }
    }

    describe("while trying to get logged in with the wrong username") {
      it("should not be allowed to log in") {
        val server: ActorRef[AdministrationServerCommand] = administrationServer.getOrElse(fail())
        server ! LoginAdministration(otherUsername, password, administrationResponseProbe.ref)
        administrationResponseProbe.expectMessage(
          10.seconds,
          AdministrationResponse(Left[ValidationError, Administration](AdministrationNotFound))
        )
      }
    }
  }
}
