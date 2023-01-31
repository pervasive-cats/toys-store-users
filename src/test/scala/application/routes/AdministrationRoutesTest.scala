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
import application.actors.{AdministrationServerCommand, CustomerServerCommand, StoreManagerServerCommand}
import application.actors.AdministrationServerCommand.*
import application.routes.Response.{EmptyResponse, AdministrationResponse}
import application.routes.Routes
import application.Serializers.given
import application.routes.AdministrationEntity.*
import application.routes.Entity.{ErrorResponseEntity, ResultResponseEntity}
import application.routes.Routes.{DeserializationFailed, RequestFailed}
import users.user.services.PasswordAlgorithm.PasswordNotMatching
import users.user.valueobjects.{PlainPassword, Username}
import users.administration.Repository.*
import users.RepositoryOperationFailed

import users.administration.entities.Administration

class AdministrationRoutesTest extends AnyFunSpec with ScalatestRouteTest with SprayJsonSupport {

  private given typedSystem: ActorSystem[_] = system.toTyped
  private val administrationServerProbe = TestProbe[AdministrationServerCommand]()

  private val routes: Route =
    Routes(TestProbe[CustomerServerCommand]().ref, TestProbe[StoreManagerServerCommand]().ref, administrationServerProbe.ref)

  private val username: Username = Username("mar10").getOrElse(fail())
  private val password: PlainPassword = PlainPassword("Password1!").getOrElse(fail())
  private val administration: Administration = Administration(username)

  describe("An administration service") {
    describe("when sending a PUT request to the /administration/login endpoint") {
      it("should send a response logging in an administration account if everything is correct") {
        val test: RouteTestResult =
          Put("/administration/login", AdministrationLoginEntity(username, password)) ~> routes
        val message: AdministrationServerCommand = administrationServerProbe.receiveMessage(10.seconds)
        message match {
          case LoginAdministration(u, p, r) =>
            u shouldBe username
            p shouldBe password
            r ! AdministrationResponse(Right[ValidationError, Administration](administration))
          case _ => fail()
        }
        test ~> check {
          status shouldBe StatusCodes.OK
          contentType shouldBe ContentTypes.`application/json`
          entityAs[ResultResponseEntity[Administration]].result shouldBe administration
        }
      }

      it("should send a 404 response if the administration account does not exists") {
        val test: RouteTestResult =
          Put("/administration/login", AdministrationLoginEntity(username, password)) ~> routes
        val message: LoginAdministration = administrationServerProbe.expectMessageType[LoginAdministration](10.seconds)
        message.replyTo ! AdministrationResponse(Left[ValidationError, Administration](AdministrationNotFound))
        test ~> check {
          status shouldBe StatusCodes.NotFound
          contentType shouldBe ContentTypes.`application/json`
          entityAs[ErrorResponseEntity].error shouldBe AdministrationNotFound
        }
      }

      it("should send a 404 response if the password provided is not valid") {
        val test: RouteTestResult =
          Put("/administration/login", AdministrationLoginEntity(username, password)) ~> routes
        val message: LoginAdministration = administrationServerProbe.expectMessageType[LoginAdministration](10.seconds)
        message.replyTo ! AdministrationResponse(Left[ValidationError, Administration](AdministrationNotFound))
        test ~> check {
          status shouldBe StatusCodes.NotFound
          contentType shouldBe ContentTypes.`application/json`
          entityAs[ErrorResponseEntity].error shouldBe AdministrationNotFound
        }
      }

      it("should send a 500 response if anything else happens") {
        val test: RouteTestResult =
          Put("/administration/login", AdministrationLoginEntity(username, password)) ~> routes
        val message: LoginAdministration = administrationServerProbe.expectMessageType[LoginAdministration](10.seconds)
        message.replyTo ! AdministrationResponse(Left[ValidationError, Administration](RepositoryOperationFailed))
        test ~> check {
          status shouldBe StatusCodes.InternalServerError
          contentType shouldBe ContentTypes.`application/json`
          entityAs[ErrorResponseEntity].error shouldBe RepositoryOperationFailed
        }
      }

      it("should send a 400 response if the request body is not correctly formatted") {
        Put("/administration/login", "{}".toJson) ~> routes ~> check {
          status shouldBe StatusCodes.BadRequest
          contentType shouldBe ContentTypes.`application/json`
          entityAs[ErrorResponseEntity].error shouldBe DeserializationFailed("Object expected in field 'username'")
        }
      }
    }

    describe("when sending a PUT request to the /administration/password endpoint") {
      val newPassword: PlainPassword = PlainPassword("Password2?").getOrElse(fail())

      it("should send a response updating the administration password if everything is correct") {
        val test: RouteTestResult =
          Put("/administration/password", AdministrationUpdatePasswordEntity(username, password, newPassword)) ~> routes
        val message: AdministrationServerCommand = administrationServerProbe.receiveMessage(10.seconds)
        message match {
          case UpdateAdministrationPassword(u, p, np, r) =>
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

      it("should send a 404 response if the administration account does not exists") {
        val test: RouteTestResult =
          Put("/administration/password", AdministrationUpdatePasswordEntity(username, password, newPassword)) ~> routes
        val message: UpdateAdministrationPassword =
          administrationServerProbe.expectMessageType[UpdateAdministrationPassword](10.seconds)
        message.replyTo ! EmptyResponse(Left[ValidationError, Unit](AdministrationNotFound))
        test ~> check {
          status shouldBe StatusCodes.NotFound
          contentType shouldBe ContentTypes.`application/json`
          entityAs[ErrorResponseEntity].error shouldBe AdministrationNotFound
        }
      }

      it("should send a 400 response if the password provided is not valid") {
        val test: RouteTestResult =
          Put("/administration/password", AdministrationUpdatePasswordEntity(username, password, newPassword)) ~> routes
        val message: UpdateAdministrationPassword =
          administrationServerProbe.expectMessageType[UpdateAdministrationPassword](10.seconds)
        message.replyTo ! EmptyResponse(Left[ValidationError, Unit](PasswordNotMatching))
        test ~> check {
          status shouldBe StatusCodes.BadRequest
          contentType shouldBe ContentTypes.`application/json`
          entityAs[ErrorResponseEntity].error shouldBe PasswordNotMatching
        }
      }

      it("should send a 500 response if anything else happens") {
        val test: RouteTestResult =
          Put("/administration/password", AdministrationUpdatePasswordEntity(username, password, newPassword)) ~> routes
        val message: UpdateAdministrationPassword =
          administrationServerProbe.expectMessageType[UpdateAdministrationPassword](10.seconds)
        message.replyTo ! EmptyResponse(Left[ValidationError, Unit](RepositoryOperationFailed))
        test ~> check {
          status shouldBe StatusCodes.InternalServerError
          contentType shouldBe ContentTypes.`application/json`
          entityAs[ErrorResponseEntity].error shouldBe RepositoryOperationFailed
        }
      }

      it("should send a 400 response if the request body is not correctly formatted") {
        Put("/administration/password", "{}".toJson) ~> routes ~> check {
          status shouldBe StatusCodes.BadRequest
          contentType shouldBe ContentTypes.`application/json`
          entityAs[ErrorResponseEntity].error shouldBe DeserializationFailed("Object expected in field 'username'")
        }
      }
    }
  }
}
