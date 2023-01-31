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
import application.actors.StoreManagerServerCommand.*
import application.actors.RootCommand.Startup
import application.routes.Response
import application.routes.Response.{EmptyResponse, StoreManagerResponse}
import users.user.services.PasswordAlgorithm
import users.user.valueobjects.*
import users.storemanager.Repository.*
import users.storemanager.entities.StoreManagerOps.updateStore
import users.user.services.PasswordAlgorithm.PasswordNotMatching
import users.storemanager.entities.StoreManager
import users.storemanager.valueobjects.Store

class StoreManagerServerActorTest extends AnyFunSpec with TestContainerForAll with BeforeAndAfterAll {

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
  private val storeManagerResponseProbe: TestProbe[StoreManagerResponse] = testKit.createTestProbe[StoreManagerResponse]()
  private val emptyResponseProbe: TestProbe[EmptyResponse] = testKit.createTestProbe[EmptyResponse]()

  @SuppressWarnings(Array("org.wartremover.warts.Var", "scalafix:DisableSyntax.var"))
  private var storeManagerServer: Option[ActorRef[StoreManagerServerCommand]] = None

  override def afterContainersStart(containers: Containers): Unit =
    storeManagerServer = Some(
      testKit.spawn(
        StoreManagerServerActor(
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

  private val username: Username = Username("mar10").getOrElse(fail())
  private val store: Store = Store(1).getOrElse(fail())
  private val password: PlainPassword = PlainPassword("Password1!").getOrElse(fail())
  private val otherPassword: PlainPassword = PlainPassword("passWORD2?").getOrElse(fail())
  private val storeManager: StoreManager = StoreManager(username, store)

  describe("A store manager server actor") {
    describe("when first started up") {
      it("should notify the root actor of its start") {
        rootActorProbe.expectMessage(10.seconds, Startup(true))
      }
    }
  }

  describe("A store manager") {
    describe("after being registered") {
      it("should be present in the database") {
        val server: ActorRef[StoreManagerServerCommand] = storeManagerServer.getOrElse(fail())
        server ! RegisterStoreManager(storeManager, password, storeManagerResponseProbe.ref)
        storeManagerResponseProbe.expectMessage(
          10.seconds,
          StoreManagerResponse(Right[ValidationError, StoreManager](storeManager))
        )
        server ! LoginStoreManager(username, password, storeManagerResponseProbe.ref)
        storeManagerResponseProbe.expectMessage(
          10.seconds,
          StoreManagerResponse(Right[ValidationError, StoreManager](storeManager))
        )
        server ! DeregisterStoreManager(username, password, emptyResponseProbe.ref)
        emptyResponseProbe.expectMessage(10.seconds, EmptyResponse(Right[ValidationError, Unit](())))
      }
    }

    describe("after being registered and while trying to get logged in with the wrong password") {
      it("should not be allowed") {
        val server: ActorRef[StoreManagerServerCommand] = storeManagerServer.getOrElse(fail())
        server ! RegisterStoreManager(storeManager, password, storeManagerResponseProbe.ref)
        storeManagerResponseProbe.expectMessage(
          10.seconds,
          StoreManagerResponse(Right[ValidationError, StoreManager](storeManager))
        )
        server ! LoginStoreManager(username, otherPassword, storeManagerResponseProbe.ref)
        storeManagerResponseProbe.expectMessage(
          10.seconds,
          StoreManagerResponse(Left[ValidationError, StoreManager](PasswordNotMatching))
        )
        server ! DeregisterStoreManager(username, password, emptyResponseProbe.ref)
        emptyResponseProbe.expectMessage(10.seconds, EmptyResponse(Right[ValidationError, Unit](())))
      }
    }

    describe("after being registered and then deleted") {
      it("should not be present in the database") {
        val server: ActorRef[StoreManagerServerCommand] = storeManagerServer.getOrElse(fail())
        server ! RegisterStoreManager(storeManager, password, storeManagerResponseProbe.ref)
        storeManagerResponseProbe.expectMessage(
          10.seconds,
          StoreManagerResponse(Right[ValidationError, StoreManager](storeManager))
        )
        server ! DeregisterStoreManager(username, password, emptyResponseProbe.ref)
        emptyResponseProbe.expectMessage(10.seconds, EmptyResponse(Right[ValidationError, Unit](())))
        server ! LoginStoreManager(username, password, storeManagerResponseProbe.ref)
        storeManagerResponseProbe.expectMessage(
          10.seconds,
          StoreManagerResponse(Left[ValidationError, StoreManager](StoreManagerNotFound))
        )
      }
    }

    describe("after being registered and while trying to get deleted from the database with the wrong password") {
      it("should not be allowed") {
        val server: ActorRef[StoreManagerServerCommand] = storeManagerServer.getOrElse(fail())
        server ! RegisterStoreManager(storeManager, password, storeManagerResponseProbe.ref)
        storeManagerResponseProbe.expectMessage(
          10.seconds,
          StoreManagerResponse(Right[ValidationError, StoreManager](storeManager))
        )
        server ! DeregisterStoreManager(username, otherPassword, emptyResponseProbe.ref)
        emptyResponseProbe.expectMessage(10.seconds, EmptyResponse(Left[ValidationError, Unit](PasswordNotMatching)))
        server ! DeregisterStoreManager(username, password, emptyResponseProbe.ref)
        emptyResponseProbe.expectMessage(10.seconds, EmptyResponse(Right[ValidationError, Unit](())))
      }
    }

    describe("after being registered and then their data gets updated") {
      it("should show the update") {
        val server: ActorRef[StoreManagerServerCommand] = storeManagerServer.getOrElse(fail())
        val newStoreManager: StoreManager = storeManager.updateStore(
          Store(2).getOrElse(fail())
        )
        server ! RegisterStoreManager(storeManager, password, storeManagerResponseProbe.ref)
        storeManagerResponseProbe.expectMessage(
          10.seconds,
          StoreManagerResponse(Right[ValidationError, StoreManager](storeManager))
        )
        server ! UpdateStoreManagerStore(
          username,
          newStoreManager.store,
          storeManagerResponseProbe.ref
        )
        storeManagerResponseProbe.expectMessage(
          10.seconds,
          StoreManagerResponse(Right[ValidationError, StoreManager](newStoreManager))
        )
        server ! LoginStoreManager(newStoreManager.username, password, storeManagerResponseProbe.ref)
        storeManagerResponseProbe.expectMessage(
          10.seconds,
          StoreManagerResponse(Right[ValidationError, StoreManager](newStoreManager))
        )
        server ! DeregisterStoreManager(newStoreManager.username, password, emptyResponseProbe.ref)
        emptyResponseProbe.expectMessage(10.seconds, EmptyResponse(Right[ValidationError, Unit](())))
      }
    }

    describe("when their data gets updated but they were never registered in the first place") {
      it("should not be allowed") {
        val server: ActorRef[StoreManagerServerCommand] = storeManagerServer.getOrElse(fail())
        val newStoreManager: StoreManager = storeManager.updateStore(
          Store(2).getOrElse(fail())
        )
        server ! UpdateStoreManagerStore(
          username,
          newStoreManager.store,
          storeManagerResponseProbe.ref
        )
        storeManagerResponseProbe.expectMessage(
          10.seconds,
          StoreManagerResponse(Left[ValidationError, StoreManager](StoreManagerNotFound))
        )
      }
    }

    describe("after being registered and their password gets updated") {
      it("should show the update") {
        val server: ActorRef[StoreManagerServerCommand] = storeManagerServer.getOrElse(fail())
        server ! RegisterStoreManager(storeManager, password, storeManagerResponseProbe.ref)
        storeManagerResponseProbe.expectMessage(
          10.seconds,
          StoreManagerResponse(Right[ValidationError, StoreManager](storeManager))
        )
        server ! UpdateStoreManagerPassword(username, password, otherPassword, emptyResponseProbe.ref)
        emptyResponseProbe.expectMessage(10.seconds, EmptyResponse(Right[ValidationError, Unit](())))
        server ! LoginStoreManager(username, password, storeManagerResponseProbe.ref)
        storeManagerResponseProbe.expectMessage(
          10.seconds,
          StoreManagerResponse(Left[ValidationError, StoreManager](PasswordNotMatching))
        )
        server ! LoginStoreManager(username, otherPassword, storeManagerResponseProbe.ref)
        storeManagerResponseProbe.expectMessage(
          10.seconds,
          StoreManagerResponse(Right[ValidationError, StoreManager](storeManager))
        )
        server ! DeregisterStoreManager(username, otherPassword, emptyResponseProbe.ref)
        emptyResponseProbe.expectMessage(10.seconds, EmptyResponse(Right[ValidationError, Unit](())))
      }
    }

    describe("after being registered and while trying to get their password updated with the wrong old password") {
      it("should not be allowed") {
        val server: ActorRef[StoreManagerServerCommand] = storeManagerServer.getOrElse(fail())
        server ! RegisterStoreManager(storeManager, password, storeManagerResponseProbe.ref)
        storeManagerResponseProbe.expectMessage(
          10.seconds,
          StoreManagerResponse(Right[ValidationError, StoreManager](storeManager))
        )
        server ! UpdateStoreManagerPassword(username, otherPassword, password, emptyResponseProbe.ref)
        emptyResponseProbe.expectMessage(10.seconds, EmptyResponse(Left[ValidationError, Unit](PasswordNotMatching)))
        server ! DeregisterStoreManager(username, password, emptyResponseProbe.ref)
        emptyResponseProbe.expectMessage(10.seconds, EmptyResponse(Right[ValidationError, Unit](())))
      }
    }

    describe("when their password gets updated but they were never registered in the first place") {
      it("should not be allowed") {
        val server: ActorRef[StoreManagerServerCommand] = storeManagerServer.getOrElse(fail())
        server ! UpdateStoreManagerPassword(username, password, otherPassword, emptyResponseProbe.ref)
        emptyResponseProbe.expectMessage(10.seconds, EmptyResponse(Left[ValidationError, Unit](StoreManagerNotFound)))
      }
    }

    describe("if never registered") {
      it("should not be present") {
        val server: ActorRef[StoreManagerServerCommand] = storeManagerServer.getOrElse(fail())
        server ! LoginStoreManager(username, password, storeManagerResponseProbe.ref)
        storeManagerResponseProbe.expectMessage(
          10.seconds,
          StoreManagerResponse(Left[ValidationError, StoreManager](StoreManagerNotFound))
        )
        server ! DeregisterStoreManager(username, password, emptyResponseProbe.ref)
        emptyResponseProbe.expectMessage(10.seconds, EmptyResponse(Left[ValidationError, Unit](StoreManagerNotFound)))
      }
    }

    describe("if already registered") {
      it("should not allow a new registration") {
        val server: ActorRef[StoreManagerServerCommand] = storeManagerServer.getOrElse(fail())
        server ! RegisterStoreManager(storeManager, password, storeManagerResponseProbe.ref)
        storeManagerResponseProbe.expectMessage(
          10.seconds,
          StoreManagerResponse(Right[ValidationError, StoreManager](storeManager))
        )
        server ! RegisterStoreManager(storeManager, password, storeManagerResponseProbe.ref)
        storeManagerResponseProbe.expectMessage(
          10.seconds,
          StoreManagerResponse(Left[ValidationError, StoreManager](StoreManagerAlreadyPresent))
        )
      }
    }
  }
}
