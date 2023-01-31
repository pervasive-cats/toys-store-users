/*
 * Copyright © 2022-2023 by Pervasive Cats S.r.l.s.
 *
 * All Rights Reserved.
 */

package io.github.pervasivecats
package users.application.routes

import scala.concurrent.duration.DurationInt

import io.github.pervasivecats.ValidationError
import io.github.pervasivecats.users.RepositoryOperationFailed

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
import application.actors.CustomerServerCommand.*
import application.routes.Response.{CustomerResponse, EmptyResponse}
import application.routes.Routes
import application.Serializers.given
import application.routes.CustomerEntity.*
import application.routes.Entity.{ErrorResponseEntity, ResultResponseEntity}
import application.routes.Routes.{DeserializationFailed, RequestFailed}
import users.customer.entities.Customer
import users.customer.entities.CustomerOps.updated
import users.customer.valueobjects.{Email, NameComponent}
import users.user.services.PasswordAlgorithm.PasswordNotMatching
import users.user.valueobjects.{PlainPassword, Username}
import users.customer.Repository.*

class CustomerRoutesTest extends AnyFunSpec with ScalatestRouteTest with SprayJsonSupport {

  private given typedSystem: ActorSystem[_] = system.toTyped
  private val customerServerProbe = TestProbe[CustomerServerCommand]()
  private val routes: Route = Routes(customerServerProbe.ref, TestProbe[StoreManagerServerCommand]().ref)

  private val username: Username = Username("mar10").getOrElse(fail())
  private val email: Email = Email("mario@email.com").getOrElse(fail())
  private val firstName: NameComponent = NameComponent("Mario").getOrElse(fail())
  private val lastName: NameComponent = NameComponent("Rossi").getOrElse(fail())
  private val password: PlainPassword = PlainPassword("Password1!").getOrElse(fail())
  private val customer: Customer = Customer(firstName, lastName, email, username)

  describe("A customer service") {
    describe("when sending a POST request to the /customer endpoint") {
      it("should send a response creating a new user if everything is correct") {
        val test: RouteTestResult =
          Post("/customer", CustomerRegistrationEntity(email, username, firstName, lastName, password)) ~> routes
        val message: CustomerServerCommand = customerServerProbe.receiveMessage(10.seconds)
        message match {
          case RegisterCustomer(c, p, r) =>
            c shouldBe customer
            p shouldBe password
            r ! CustomerResponse(Right[ValidationError, Customer](customer))
          case _ => fail()
        }
        test ~> check {
          status shouldBe StatusCodes.OK
          contentType shouldBe ContentTypes.`application/json`
          entityAs[ResultResponseEntity[Customer]].result shouldBe customer
        }
      }

      it("should send a 400 response if the customer already exists") {
        val test: RouteTestResult =
          Post("/customer", CustomerRegistrationEntity(email, username, firstName, lastName, password)) ~> routes
        val message: RegisterCustomer = customerServerProbe.expectMessageType[RegisterCustomer](10.seconds)
        message.replyTo ! CustomerResponse(Left[ValidationError, Customer](CustomerAlreadyPresent))
        test ~> check {
          status shouldBe StatusCodes.BadRequest
          contentType shouldBe ContentTypes.`application/json`
          entityAs[ErrorResponseEntity].error shouldBe CustomerAlreadyPresent
        }
      }

      it("should send a 500 response if anything else happens") {
        val test: RouteTestResult =
          Post("/customer", CustomerRegistrationEntity(email, username, firstName, lastName, password)) ~> routes
        val message: RegisterCustomer = customerServerProbe.expectMessageType[RegisterCustomer](10.seconds)
        message.replyTo ! CustomerResponse(Left[ValidationError, Customer](RepositoryOperationFailed))
        test ~> check {
          status shouldBe StatusCodes.InternalServerError
          contentType shouldBe ContentTypes.`application/json`
          entityAs[ErrorResponseEntity].error shouldBe RepositoryOperationFailed
        }
      }

      it("should send a 400 response if the request body is not correctly formatted") {
        Post("/customer", "{}".toJson) ~> routes ~> check {
          status shouldBe StatusCodes.BadRequest
          contentType shouldBe ContentTypes.`application/json`
          entityAs[ErrorResponseEntity].error shouldBe DeserializationFailed("Object expected in field 'email'")
        }
      }
    }

    describe("when sending a DELETE request to the /customer endpoint") {
      it("should send a response de-registering a customer if everything is correct") {
        val test: RouteTestResult =
          Delete("/customer", CustomerDeregistrationEntity(email, password)) ~> routes
        val message: CustomerServerCommand = customerServerProbe.receiveMessage(10.seconds)
        message match {
          case DeregisterCustomer(e, p, r) =>
            e shouldBe email
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

      it("should send a 404 response if the customer does not exists") {
        val test: RouteTestResult =
          Delete("/customer", CustomerDeregistrationEntity(email, password)) ~> routes
        val message: DeregisterCustomer = customerServerProbe.expectMessageType[DeregisterCustomer](10.seconds)
        message.replyTo ! EmptyResponse(Left[ValidationError, Unit](CustomerNotFound))
        test ~> check {
          status shouldBe StatusCodes.NotFound
          contentType shouldBe ContentTypes.`application/json`
          entityAs[ErrorResponseEntity].error shouldBe CustomerNotFound
        }
      }

      it("should send a 400 response if the password provided is not valid") {
        val test: RouteTestResult =
          Delete("/customer", CustomerDeregistrationEntity(email, password)) ~> routes
        val message: DeregisterCustomer = customerServerProbe.expectMessageType[DeregisterCustomer](10.seconds)
        message.replyTo ! EmptyResponse(Left[ValidationError, Unit](PasswordNotMatching))
        test ~> check {
          status shouldBe StatusCodes.BadRequest
          contentType shouldBe ContentTypes.`application/json`
          entityAs[ErrorResponseEntity].error shouldBe PasswordNotMatching
        }
      }

      it("should send a 500 response if anything else happens") {
        val test: RouteTestResult =
          Delete("/customer", CustomerDeregistrationEntity(email, password)) ~> routes
        val message: DeregisterCustomer = customerServerProbe.expectMessageType[DeregisterCustomer](10.seconds)
        message.replyTo ! EmptyResponse(Left[ValidationError, Unit](RepositoryOperationFailed))
        test ~> check {
          status shouldBe StatusCodes.InternalServerError
          contentType shouldBe ContentTypes.`application/json`
          entityAs[ErrorResponseEntity].error shouldBe RepositoryOperationFailed
        }
      }

      it("should send a 400 response if the request body is not correctly formatted") {
        Delete("/customer", "{}".toJson) ~> routes ~> check {
          status shouldBe StatusCodes.BadRequest
          contentType shouldBe ContentTypes.`application/json`
          entityAs[ErrorResponseEntity].error shouldBe DeserializationFailed("Object expected in field 'email'")
        }
      }
    }

    describe("when sending a PUT request to the /customer endpoint") {
      val newEmail: Email = Email("luigi@mail.com").getOrElse(fail())
      val newFirstName: NameComponent = NameComponent("Luigi").getOrElse(fail())
      val newLastName: NameComponent = NameComponent("Bianchi").getOrElse(fail())
      val newUsername: Username = Username("l0033gi").getOrElse(fail())
      val newCustomer: Customer = customer.updated(newEmail, newFirstName, newLastName, newUsername)

      it("should send a response updating a customer data if everything is correct") {
        val test: RouteTestResult =
          Put("/customer", CustomerUpdateDataEntity(email, newEmail, newUsername, newFirstName, newLastName)) ~> routes
        val message: CustomerServerCommand = customerServerProbe.receiveMessage(10.seconds)
        message match {
          case UpdateCustomerData(e, ne, nu, nf, nl, r) =>
            e shouldBe email
            ne shouldBe newEmail
            nu shouldBe newUsername
            nf shouldBe newFirstName
            nl shouldBe newLastName
            r ! CustomerResponse(Right[ValidationError, Customer](newCustomer))
          case _ => fail()
        }
        test ~> check {
          status shouldBe StatusCodes.OK
          contentType shouldBe ContentTypes.`application/json`
          entityAs[ResultResponseEntity[Customer]].result shouldBe newCustomer
        }
      }

      it("should send a 404 response if the customer does not exists") {
        val test: RouteTestResult =
          Put("/customer", CustomerUpdateDataEntity(email, newEmail, newUsername, newFirstName, newLastName)) ~> routes
        val message: UpdateCustomerData = customerServerProbe.expectMessageType[UpdateCustomerData](10.seconds)
        message.replyTo ! CustomerResponse(Left[ValidationError, Customer](CustomerNotFound))
        test ~> check {
          status shouldBe StatusCodes.NotFound
          contentType shouldBe ContentTypes.`application/json`
          entityAs[ErrorResponseEntity].error shouldBe CustomerNotFound
        }
      }

      it("should send a 500 response if anything else happens") {
        val test: RouteTestResult =
          Put("/customer", CustomerUpdateDataEntity(email, newEmail, newUsername, newFirstName, newLastName)) ~> routes
        val message: UpdateCustomerData = customerServerProbe.expectMessageType[UpdateCustomerData](10.seconds)
        message.replyTo ! CustomerResponse(Left[ValidationError, Customer](RepositoryOperationFailed))
        test ~> check {
          status shouldBe StatusCodes.InternalServerError
          contentType shouldBe ContentTypes.`application/json`
          entityAs[ErrorResponseEntity].error shouldBe RepositoryOperationFailed
        }
      }

      it("should send a 400 response if the request body is not correctly formatted") {
        Put("/customer", "{}".toJson) ~> routes ~> check {
          status shouldBe StatusCodes.BadRequest
          contentType shouldBe ContentTypes.`application/json`
          entityAs[ErrorResponseEntity].error shouldBe DeserializationFailed("Object expected in field 'email'")
        }
      }
    }

    describe("when sending a PUT request to the /customer/login endpoint") {
      it("should send a response logging in a customer if everything is correct") {
        val test: RouteTestResult =
          Put("/customer/login", CustomerLoginEntity(email, password)) ~> routes
        val message: CustomerServerCommand = customerServerProbe.receiveMessage(10.seconds)
        message match {
          case LoginCustomer(e, p, r) =>
            e shouldBe email
            p shouldBe password
            r ! CustomerResponse(Right[ValidationError, Customer](customer))
          case _ => fail()
        }
        test ~> check {
          status shouldBe StatusCodes.OK
          contentType shouldBe ContentTypes.`application/json`
          entityAs[ResultResponseEntity[Customer]].result shouldBe customer
        }
      }

      it("should send a 404 response if the customer does not exists") {
        val test: RouteTestResult =
          Put("/customer/login", CustomerLoginEntity(email, password)) ~> routes
        val message: LoginCustomer = customerServerProbe.expectMessageType[LoginCustomer](10.seconds)
        message.replyTo ! CustomerResponse(Left[ValidationError, Customer](CustomerNotFound))
        test ~> check {
          status shouldBe StatusCodes.NotFound
          contentType shouldBe ContentTypes.`application/json`
          entityAs[ErrorResponseEntity].error shouldBe CustomerNotFound
        }
      }

      it("should send a 404 response if the password provided is not valid") {
        val test: RouteTestResult =
          Put("/customer/login", CustomerLoginEntity(email, password)) ~> routes
        val message: LoginCustomer = customerServerProbe.expectMessageType[LoginCustomer](10.seconds)
        message.replyTo ! CustomerResponse(Left[ValidationError, Customer](CustomerNotFound))
        test ~> check {
          status shouldBe StatusCodes.NotFound
          contentType shouldBe ContentTypes.`application/json`
          entityAs[ErrorResponseEntity].error shouldBe CustomerNotFound
        }
      }

      it("should send a 500 response if anything else happens") {
        val test: RouteTestResult =
          Put("/customer/login", CustomerLoginEntity(email, password)) ~> routes
        val message: LoginCustomer = customerServerProbe.expectMessageType[LoginCustomer](10.seconds)
        message.replyTo ! CustomerResponse(Left[ValidationError, Customer](RepositoryOperationFailed))
        test ~> check {
          status shouldBe StatusCodes.InternalServerError
          contentType shouldBe ContentTypes.`application/json`
          entityAs[ErrorResponseEntity].error shouldBe RepositoryOperationFailed
        }
      }

      it("should send a 400 response if the request body is not correctly formatted") {
        Put("/customer/login", "{}".toJson) ~> routes ~> check {
          status shouldBe StatusCodes.BadRequest
          contentType shouldBe ContentTypes.`application/json`
          entityAs[ErrorResponseEntity].error shouldBe DeserializationFailed("Object expected in field 'email'")
        }
      }
    }

    describe("when sending a PUT request to the /customer/password endpoint") {
      val newPassword: PlainPassword = PlainPassword("Password2?").getOrElse(fail())

      it("should send a response updating the customer password if everything is correct") {
        val test: RouteTestResult =
          Put("/customer/password", CustomerUpdatePasswordEntity(email, password, newPassword)) ~> routes
        val message: CustomerServerCommand = customerServerProbe.receiveMessage(10.seconds)
        message match {
          case UpdateCustomerPassword(e, p, np, r) =>
            e shouldBe email
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

      it("should send a 404 response if the customer does not exists") {
        val test: RouteTestResult =
          Put("/customer/password", CustomerUpdatePasswordEntity(email, password, newPassword)) ~> routes
        val message: UpdateCustomerPassword = customerServerProbe.expectMessageType[UpdateCustomerPassword](10.seconds)
        message.replyTo ! EmptyResponse(Left[ValidationError, Unit](CustomerNotFound))
        test ~> check {
          status shouldBe StatusCodes.NotFound
          contentType shouldBe ContentTypes.`application/json`
          entityAs[ErrorResponseEntity].error shouldBe CustomerNotFound
        }
      }

      it("should send a 400 response if the password provided is not valid") {
        val test: RouteTestResult =
          Put("/customer/password", CustomerUpdatePasswordEntity(email, password, newPassword)) ~> routes
        val message: UpdateCustomerPassword = customerServerProbe.expectMessageType[UpdateCustomerPassword](10.seconds)
        message.replyTo ! EmptyResponse(Left[ValidationError, Unit](PasswordNotMatching))
        test ~> check {
          status shouldBe StatusCodes.BadRequest
          contentType shouldBe ContentTypes.`application/json`
          entityAs[ErrorResponseEntity].error shouldBe PasswordNotMatching
        }
      }

      it("should send a 500 response if anything else happens") {
        val test: RouteTestResult =
          Put("/customer/password", CustomerUpdatePasswordEntity(email, password, newPassword)) ~> routes
        val message: UpdateCustomerPassword = customerServerProbe.expectMessageType[UpdateCustomerPassword](10.seconds)
        message.replyTo ! EmptyResponse(Left[ValidationError, Unit](RepositoryOperationFailed))
        test ~> check {
          status shouldBe StatusCodes.InternalServerError
          contentType shouldBe ContentTypes.`application/json`
          entityAs[ErrorResponseEntity].error shouldBe RepositoryOperationFailed
        }
      }

      it("should send a 400 response if the request body is not correctly formatted") {
        Put("/customer/password", "{}".toJson) ~> routes ~> check {
          status shouldBe StatusCodes.BadRequest
          contentType shouldBe ContentTypes.`application/json`
          entityAs[ErrorResponseEntity].error shouldBe DeserializationFailed("Object expected in field 'email'")
        }
      }
    }
  }
}
