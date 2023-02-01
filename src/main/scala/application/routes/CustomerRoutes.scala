/*
 * Copyright © 2022-2023 by Pervasive Cats S.r.l.s.
 *
 * All Rights Reserved.
 */

package io.github.pervasivecats
package application.routes

import scala.concurrent.duration.DurationInt
import scala.util.Failure
import scala.util.Success

import io.github.pervasivecats.Validated
import io.github.pervasivecats.ValidationError

import akka.actor.typed.ActorRef
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.actor.typed.scaladsl.AskPattern.schedulerFromActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.marshalling.ToResponseMarshaller
import akka.http.scaladsl.model.*
import akka.http.scaladsl.server.*
import akka.http.scaladsl.unmarshalling.FromRequestUnmarshaller
import akka.util.Timeout
import spray.json.DefaultJsonProtocol
import spray.json.DeserializationException
import spray.json.JsonWriter

import application.actors.*
import application.actors.commands.CustomerServerCommand.*
import entities.CustomerEntity.{
  CustomerDeregistrationEntity,
  CustomerLoginEntity,
  CustomerRegistrationEntity,
  CustomerUpdateDataEntity,
  CustomerUpdatePasswordEntity
}
import entities.Entity.{ErrorResponseEntity, ResultResponseEntity}
import entities.Response.{CustomerResponse, EmptyResponse}
import application.Serializers.given
import application.routes.Routes.RequestFailed
import users.customer.entities.Customer
import users.customer.valueobjects.{Email, NameComponent}
import users.customer.Repository.{CustomerAlreadyPresent, CustomerNotFound}
import users.user.services.PasswordAlgorithm.PasswordNotMatching
import users.user.valueobjects.{PlainPassword, Username}
import application.actors.commands.CustomerServerCommand
import entities.Response

private object CustomerRoutes extends SprayJsonSupport with DefaultJsonProtocol with Directives {

  private given Timeout = 30.seconds

  private def route[A: FromRequestUnmarshaller, B <: CustomerServerCommand, C <: Response[D], D: JsonWriter](
    server: ActorRef[CustomerServerCommand],
    request: A => ActorRef[C] => B,
    responseHandler: C => Route
  )(
    using
    ActorSystem[_]
  ): Route =
    entity(as[A]) { e =>
      onComplete(server ? request(e)) {
        case Failure(_) => complete(StatusCodes.InternalServerError, ErrorResponseEntity(RequestFailed))
        case Success(value) => responseHandler(value)
      }
    }

  private def handleCustomerAndPasswordResult(result: Validated[Unit]): Route = result match {
    case Right(value) => complete(ResultResponseEntity(value))
    case Left(error) =>
      error match {
        case CustomerNotFound => complete(StatusCodes.NotFound, ErrorResponseEntity(error))
        case PasswordNotMatching => complete(StatusCodes.BadRequest, ErrorResponseEntity(error))
        case _ => complete(StatusCodes.InternalServerError, ErrorResponseEntity(error))
      }
  }

  private def handleCustomerResult(result: Validated[Customer]): Route = result match {
    case Right(value) => complete(ResultResponseEntity(value))
    case Left(error) =>
      error match {
        case CustomerNotFound => complete(StatusCodes.NotFound, ErrorResponseEntity(error))
        case _ => complete(StatusCodes.InternalServerError, ErrorResponseEntity(error))
      }
  }

  def apply(server: ActorRef[CustomerServerCommand])(using ActorSystem[_]): Route =
    concat(
      path("customer") {
        concat(
          post {
            route[CustomerRegistrationEntity, RegisterCustomer, CustomerResponse, Customer](
              server,
              e => RegisterCustomer(Customer(e.firstName, e.lastName, e.email, e.username), e.password, _),
              _.result match {
                case Right(value) => complete(ResultResponseEntity(value))
                case Left(error) =>
                  error match {
                    case CustomerAlreadyPresent => complete(StatusCodes.BadRequest, ErrorResponseEntity(error))
                    case _ => complete(StatusCodes.InternalServerError, ErrorResponseEntity(error))
                  }
              }
            )
          },
          delete {
            route[CustomerDeregistrationEntity, DeregisterCustomer, EmptyResponse, Unit](
              server,
              e => DeregisterCustomer(e.email, e.password, _),
              r => handleCustomerAndPasswordResult(r.result)
            )
          },
          put {
            route[CustomerUpdateDataEntity, UpdateCustomerData, CustomerResponse, Customer](
              server,
              e => UpdateCustomerData(e.email, e.newEmail, e.newUsername, e.newFirstName, e.newLastName, _),
              r => handleCustomerResult(r.result)
            )
          }
        )
      },
      path("customer" / "login") {
        put {
          route[CustomerLoginEntity, LoginCustomer, CustomerResponse, Customer](
            server,
            e => LoginCustomer(e.email, e.password, _),
            _.result match {
              case Right(value) => complete(ResultResponseEntity(value))
              case Left(error) =>
                error match {
                  case CustomerNotFound | PasswordNotMatching =>
                    complete(StatusCodes.NotFound, ErrorResponseEntity(CustomerNotFound))
                  case _ => complete(StatusCodes.InternalServerError, ErrorResponseEntity(error))
                }
            }
          )
        }
      },
      path("customer" / "password") {
        put {
          route[CustomerUpdatePasswordEntity, UpdateCustomerPassword, EmptyResponse, Unit](
            server,
            e => UpdateCustomerPassword(e.email, e.password, e.newPassword, _),
            r => handleCustomerAndPasswordResult(r.result)
          )
        }
      }
    )
}
