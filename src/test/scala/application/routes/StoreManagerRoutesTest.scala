/*
 * Copyright © 2022-2023 by Pervasive Cats S.r.l.s.
 *
 * All Rights Reserved.
 */

package io.github.pervasivecats
package application.routes

import scala.concurrent.duration.DurationInt

import io.github.pervasivecats.ValidationError

import akka.actor.testkit.typed.scaladsl.TestProbe
import akka.actor.typed.ActorSystem
import akka.actor.typed.Scheduler
import akka.actor.typed.scaladsl.adapter.*
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.*
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.util.Timeout
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers.*
import spray.json.RootJsonReader
import spray.json.RootJsonWriter
import spray.json.given

import application.actors.{CustomerServerCommand, StoreManagerServerCommand}
import application.actors.StoreManagerServerCommand.*
import application.routes.Response.{EmptyResponse, StoreManagerResponse}
import application.routes.Routes
import application.Serializers.given
import application.routes.StoreManagerEntity.*
import application.routes.Entity.{ErrorResponseEntity, ResultResponseEntity}
import application.routes.Routes.{DeserializationFailed, RequestFailed}
import users.user.services.PasswordAlgorithm.PasswordNotMatching
import users.user.valueobjects.{PlainPassword, Username}
import users.storemanager.Repository.*
import users.RepositoryOperationFailed
import users.storemanager.entities.StoreManager
import users.storemanager.entities.StoreManagerOps.updateStore
import users.storemanager.valueobjects.Store

class StoreManagerRoutesTest extends AnyFunSpec with ScalatestRouteTest with SprayJsonSupport {

  private given typedSystem: ActorSystem[_] = system.toTyped
  private val storeManagerServerProbe = TestProbe[StoreManagerServerCommand]()
  private val routes: Route = Routes(TestProbe[CustomerServerCommand]().ref, storeManagerServerProbe.ref)

  private val username: Username = Username("mar10").getOrElse(fail())
  private val store: Store = Store(1).getOrElse(fail())
  private val password: PlainPassword = PlainPassword("Password1!").getOrElse(fail())
  private val storeManager: StoreManager = StoreManager(username, store)

  describe("A store manager service") {
    describe("when sending a POST request to the /store_manager endpoint") {
      it("should send a response creating a new user if everything is correct") {
        val test: RouteTestResult =
          Post("/store_manager", StoreManagerRegistrationEntity(username, store, password)) ~> routes
        val message: StoreManagerServerCommand = storeManagerServerProbe.receiveMessage(10.seconds)
        message match {
          case RegisterStoreManager(m, p, r) =>
            m shouldBe storeManager
            p shouldBe password
            r ! StoreManagerResponse(Right[ValidationError, StoreManager](storeManager))
          case _ => fail()
        }
        test ~> check {
          status shouldBe StatusCodes.OK
          contentType shouldBe ContentTypes.`application/json`
          entityAs[ResultResponseEntity[StoreManager]].result shouldBe storeManager
        }
      }

      it("should send a 400 response if the store manager already exists") {
        val test: RouteTestResult =
          Post("/store_manager", StoreManagerRegistrationEntity(username, store, password)) ~> routes
        val message: RegisterStoreManager = storeManagerServerProbe.expectMessageType[RegisterStoreManager](10.seconds)
        message.replyTo ! StoreManagerResponse(Left[ValidationError, StoreManager](StoreManagerAlreadyPresent))
        test ~> check {
          status shouldBe StatusCodes.BadRequest
          contentType shouldBe ContentTypes.`application/json`
          entityAs[ErrorResponseEntity].error shouldBe StoreManagerAlreadyPresent
        }
      }

      it("should send a 500 response if anything else happens") {
        val test: RouteTestResult =
          Post("/store_manager", StoreManagerRegistrationEntity(username, store, password)) ~> routes
        val message: RegisterStoreManager = storeManagerServerProbe.expectMessageType[RegisterStoreManager](10.seconds)
        message.replyTo ! StoreManagerResponse(Left[ValidationError, StoreManager](RepositoryOperationFailed))
        test ~> check {
          status shouldBe StatusCodes.InternalServerError
          contentType shouldBe ContentTypes.`application/json`
          entityAs[ErrorResponseEntity].error shouldBe RepositoryOperationFailed
        }
      }

      it("should send a 400 response if the request body is not correctly formatted") {
        Post("/store_manager", "{}".toJson) ~> routes ~> check {
          status shouldBe StatusCodes.BadRequest
          contentType shouldBe ContentTypes.`application/json`
          entityAs[ErrorResponseEntity].error shouldBe DeserializationFailed("Object expected in field 'username'")
        }
      }
    }

    describe("when sending a DELETE request to the /store_manager endpoint") {
      it("should send a response de-registering a store manager if everything is correct") {
        val test: RouteTestResult =
          Delete("/store_manager", StoreManagerDeregistrationEntity(username, password)) ~> routes
        val message: StoreManagerServerCommand = storeManagerServerProbe.receiveMessage(10.seconds)
        message match {
          case DeregisterStoreManager(u, p, r) =>
            u shouldBe username
            p shouldBe password
            r ! EmptyResponse(Right[ValidationError, Unit](()))
          case _ => fail()
        }
        test ~> check {
          status shouldBe StatusCodes.OK
          contentType shouldBe ContentTypes.`application/json`
          entityAs[ResultResponseEntity[Unit]].result shouldBe ()
        }
      }

      it("should send a 404 response if the store manager does not exists") {
        val test: RouteTestResult =
          Delete("/store_manager", StoreManagerDeregistrationEntity(username, password)) ~> routes
        val message: DeregisterStoreManager = storeManagerServerProbe.expectMessageType[DeregisterStoreManager](10.seconds)
        message.replyTo ! EmptyResponse(Left[ValidationError, Unit](StoreManagerNotFound))
        test ~> check {
          status shouldBe StatusCodes.NotFound
          contentType shouldBe ContentTypes.`application/json`
          entityAs[ErrorResponseEntity].error shouldBe StoreManagerNotFound
        }
      }

      it("should send a 400 response if the password provided is not valid") {
        val test: RouteTestResult =
          Delete("/store_manager", StoreManagerDeregistrationEntity(username, password)) ~> routes
        val message: DeregisterStoreManager = storeManagerServerProbe.expectMessageType[DeregisterStoreManager](10.seconds)
        message.replyTo ! EmptyResponse(Left[ValidationError, Unit](PasswordNotMatching))
        test ~> check {
          status shouldBe StatusCodes.BadRequest
          contentType shouldBe ContentTypes.`application/json`
          entityAs[ErrorResponseEntity].error shouldBe PasswordNotMatching
        }
      }

      it("should send a 500 response if anything else happens") {
        val test: RouteTestResult =
          Delete("/store_manager", StoreManagerDeregistrationEntity(username, password)) ~> routes
        val message: DeregisterStoreManager = storeManagerServerProbe.expectMessageType[DeregisterStoreManager](10.seconds)
        message.replyTo ! EmptyResponse(Left[ValidationError, Unit](RepositoryOperationFailed))
        test ~> check {
          status shouldBe StatusCodes.InternalServerError
          contentType shouldBe ContentTypes.`application/json`
          entityAs[ErrorResponseEntity].error shouldBe RepositoryOperationFailed
        }
      }

      it("should send a 400 response if the request body is not correctly formatted") {
        Delete("/store_manager", "{}".toJson) ~> routes ~> check {
          status shouldBe StatusCodes.BadRequest
          contentType shouldBe ContentTypes.`application/json`
          entityAs[ErrorResponseEntity].error shouldBe DeserializationFailed("Object expected in field 'username'")
        }
      }
    }

    describe("when sending a PUT request to the /store_manager endpoint") {
      val newStore: Store = Store(2).getOrElse(fail())
      val newStoreManager: StoreManager = storeManager.updateStore(newStore)

      it("should send a response updating a store manager data if everything is correct") {
        val test: RouteTestResult =
          Put("/store_manager", StoreManagerUpdateStoreEntity(username, newStore)) ~> routes
        val message: StoreManagerServerCommand = storeManagerServerProbe.receiveMessage(10.seconds)
        message match {
          case UpdateStoreManagerStore(u, ns, r) =>
            u shouldBe username
            ns shouldBe newStore
            r ! StoreManagerResponse(Right[ValidationError, StoreManager](newStoreManager))
          case _ => fail()
        }
        test ~> check {
          status shouldBe StatusCodes.OK
          contentType shouldBe ContentTypes.`application/json`
          entityAs[ResultResponseEntity[StoreManager]].result shouldBe newStoreManager
        }
      }

      it("should send a 404 response if the store manager does not exists") {
        val test: RouteTestResult = Put("/store_manager", StoreManagerUpdateStoreEntity(username, newStore)) ~> routes
        val message: UpdateStoreManagerStore = storeManagerServerProbe.expectMessageType[UpdateStoreManagerStore](10.seconds)
        message.replyTo ! StoreManagerResponse(Left[ValidationError, StoreManager](StoreManagerNotFound))
        test ~> check {
          status shouldBe StatusCodes.NotFound
          contentType shouldBe ContentTypes.`application/json`
          entityAs[ErrorResponseEntity].error shouldBe StoreManagerNotFound
        }
      }

      it("should send a 500 response if anything else happens") {
        val test: RouteTestResult = Put("/store_manager", StoreManagerUpdateStoreEntity(username, newStore)) ~> routes
        val message: UpdateStoreManagerStore = storeManagerServerProbe.expectMessageType[UpdateStoreManagerStore](10.seconds)
        message.replyTo ! StoreManagerResponse(Left[ValidationError, StoreManager](RepositoryOperationFailed))
        test ~> check {
          status shouldBe StatusCodes.InternalServerError
          contentType shouldBe ContentTypes.`application/json`
          entityAs[ErrorResponseEntity].error shouldBe RepositoryOperationFailed
        }
      }

      it("should send a 400 response if the request body is not correctly formatted") {
        Put("/store_manager", "{}".toJson) ~> routes ~> check {
          status shouldBe StatusCodes.BadRequest
          contentType shouldBe ContentTypes.`application/json`
          entityAs[ErrorResponseEntity].error shouldBe DeserializationFailed("Object expected in field 'username'")
        }
      }
    }

    describe("when sending a PUT request to the /store_manager/login endpoint") {
      it("should send a response logging in a store manager if everything is correct") {
        val test: RouteTestResult =
          Put("/store_manager/login", StoreManagerLoginEntity(username, password)) ~> routes
        val message: StoreManagerServerCommand = storeManagerServerProbe.receiveMessage(10.seconds)
        message match {
          case LoginStoreManager(u, p, r) =>
            u shouldBe username
            p shouldBe password
            r ! StoreManagerResponse(Right[ValidationError, StoreManager](storeManager))
          case _ => fail()
        }
        test ~> check {
          status shouldBe StatusCodes.OK
          contentType shouldBe ContentTypes.`application/json`
          entityAs[ResultResponseEntity[StoreManager]].result shouldBe storeManager
        }
      }

      it("should send a 404 response if the store manager does not exists") {
        val test: RouteTestResult =
          Put("/store_manager/login", StoreManagerLoginEntity(username, password)) ~> routes
        val message: LoginStoreManager = storeManagerServerProbe.expectMessageType[LoginStoreManager](10.seconds)
        message.replyTo ! StoreManagerResponse(Left[ValidationError, StoreManager](StoreManagerNotFound))
        test ~> check {
          status shouldBe StatusCodes.NotFound
          contentType shouldBe ContentTypes.`application/json`
          entityAs[ErrorResponseEntity].error shouldBe StoreManagerNotFound
        }
      }

      it("should send a 404 response if the password provided is not valid") {
        val test: RouteTestResult =
          Put("/store_manager/login", StoreManagerLoginEntity(username, password)) ~> routes
        val message: LoginStoreManager = storeManagerServerProbe.expectMessageType[LoginStoreManager](10.seconds)
        message.replyTo ! StoreManagerResponse(Left[ValidationError, StoreManager](StoreManagerNotFound))
        test ~> check {
          status shouldBe StatusCodes.NotFound
          contentType shouldBe ContentTypes.`application/json`
          entityAs[ErrorResponseEntity].error shouldBe StoreManagerNotFound
        }
      }

      it("should send a 500 response if anything else happens") {
        val test: RouteTestResult =
          Put("/store_manager/login", StoreManagerLoginEntity(username, password)) ~> routes
        val message: LoginStoreManager = storeManagerServerProbe.expectMessageType[LoginStoreManager](10.seconds)
        message.replyTo ! StoreManagerResponse(Left[ValidationError, StoreManager](RepositoryOperationFailed))
        test ~> check {
          status shouldBe StatusCodes.InternalServerError
          contentType shouldBe ContentTypes.`application/json`
          entityAs[ErrorResponseEntity].error shouldBe RepositoryOperationFailed
        }
      }

      it("should send a 400 response if the request body is not correctly formatted") {
        Put("/store_manager/login", "{}".toJson) ~> routes ~> check {
          status shouldBe StatusCodes.BadRequest
          contentType shouldBe ContentTypes.`application/json`
          entityAs[ErrorResponseEntity].error shouldBe DeserializationFailed("Object expected in field 'username'")
        }
      }
    }

    describe("when sending a PUT request to the /store_manager/password endpoint") {
      val newPassword: PlainPassword = PlainPassword("Password2?").getOrElse(fail())

      it("should send a response updating the store manager password if everything is correct") {
        val test: RouteTestResult =
          Put("/store_manager/password", StoreManagerUpdatePasswordEntity(username, password, newPassword)) ~> routes
        val message: StoreManagerServerCommand = storeManagerServerProbe.receiveMessage(10.seconds)
        message match {
          case UpdateStoreManagerPassword(u, p, np, r) =>
            u shouldBe username
            p shouldBe password
            np shouldBe newPassword
            r ! EmptyResponse(Right[ValidationError, Unit](()))
          case _ => fail()
        }
        test ~> check {
          status shouldBe StatusCodes.OK
          contentType shouldBe ContentTypes.`application/json`
          entityAs[ResultResponseEntity[Unit]].result shouldBe ()
        }
      }

      it("should send a 404 response if the store manager does not exists") {
        val test: RouteTestResult =
          Put("/store_manager/password", StoreManagerUpdatePasswordEntity(username, password, newPassword)) ~> routes
        val message: UpdateStoreManagerPassword =
          storeManagerServerProbe.expectMessageType[UpdateStoreManagerPassword](10.seconds)
        message.replyTo ! EmptyResponse(Left[ValidationError, Unit](StoreManagerNotFound))
        test ~> check {
          status shouldBe StatusCodes.NotFound
          contentType shouldBe ContentTypes.`application/json`
          entityAs[ErrorResponseEntity].error shouldBe StoreManagerNotFound
        }
      }

      it("should send a 400 response if the password provided is not valid") {
        val test: RouteTestResult =
          Put("/store_manager/password", StoreManagerUpdatePasswordEntity(username, password, newPassword)) ~> routes
        val message: UpdateStoreManagerPassword =
          storeManagerServerProbe.expectMessageType[UpdateStoreManagerPassword](10.seconds)
        message.replyTo ! EmptyResponse(Left[ValidationError, Unit](PasswordNotMatching))
        test ~> check {
          status shouldBe StatusCodes.BadRequest
          contentType shouldBe ContentTypes.`application/json`
          entityAs[ErrorResponseEntity].error shouldBe PasswordNotMatching
        }
      }

      it("should send a 500 response if anything else happens") {
        val test: RouteTestResult =
          Put("/store_manager/password", StoreManagerUpdatePasswordEntity(username, password, newPassword)) ~> routes
        val message: UpdateStoreManagerPassword =
          storeManagerServerProbe.expectMessageType[UpdateStoreManagerPassword](10.seconds)
        message.replyTo ! EmptyResponse(Left[ValidationError, Unit](RepositoryOperationFailed))
        test ~> check {
          status shouldBe StatusCodes.InternalServerError
          contentType shouldBe ContentTypes.`application/json`
          entityAs[ErrorResponseEntity].error shouldBe RepositoryOperationFailed
        }
      }

      it("should send a 400 response if the request body is not correctly formatted") {
        Put("/store_manager/password", "{}".toJson) ~> routes ~> check {
          status shouldBe StatusCodes.BadRequest
          contentType shouldBe ContentTypes.`application/json`
          entityAs[ErrorResponseEntity].error shouldBe DeserializationFailed("Object expected in field 'username'")
        }
      }
    }
  }
}
