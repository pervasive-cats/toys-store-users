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
import io.github.pervasivecats.application.actors.StoreManagerServerCommand.DeregisterStoreManager
import io.github.pervasivecats.application.actors.StoreManagerServerCommand.LoginStoreManager
import io.github.pervasivecats.application.actors.StoreManagerServerCommand.RegisterStoreManager
import io.github.pervasivecats.application.actors.StoreManagerServerCommand.UpdateStoreManagerPassword
import io.github.pervasivecats.application.actors.StoreManagerServerCommand.UpdateStoreManagerStore

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
import application.routes.Entity.{ErrorResponseEntity, ResultResponseEntity}
import application.routes.Response.{EmptyResponse, StoreManagerResponse}
import application.Serializers.given
import application.routes.Routes.RequestFailed
import application.routes.StoreManagerEntity.*
import users.storemanager.Repository.{StoreManagerAlreadyPresent, StoreManagerNotFound}
import users.storemanager.entities.StoreManager
import users.user.services.PasswordAlgorithm.PasswordNotMatching
import users.user.valueobjects.{PlainPassword, Username}

private object StoreManagerRoutes extends SprayJsonSupport with DefaultJsonProtocol with Directives {

  private given Timeout = 30.seconds

  private def route[A: FromRequestUnmarshaller, B <: StoreManagerServerCommand, C <: Response[D], D: JsonWriter](
    server: ActorRef[StoreManagerServerCommand],
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

  private def handleStoreManagerAndPasswordResult(result: Validated[Unit]): Route = result match {
    case Right(value) => complete(ResultResponseEntity(value))
    case Left(error) =>
      error match {
        case StoreManagerNotFound => complete(StatusCodes.NotFound, ErrorResponseEntity(error))
        case PasswordNotMatching => complete(StatusCodes.BadRequest, ErrorResponseEntity(error))
        case _ => complete(StatusCodes.InternalServerError, ErrorResponseEntity(error))
      }
  }

  private def handleStoreManagerResult(result: Validated[StoreManager]): Route = result match {
    case Right(value) => complete(ResultResponseEntity(value))
    case Left(error) =>
      error match {
        case StoreManagerNotFound => complete(StatusCodes.NotFound, ErrorResponseEntity(error))
        case _ => complete(StatusCodes.InternalServerError, ErrorResponseEntity(error))
      }
  }

  def apply(server: ActorRef[StoreManagerServerCommand])(using ActorSystem[_]): Route =
    concat(
      path("store_manager") {
        concat(
          post {
            route[StoreManagerRegistrationEntity, RegisterStoreManager, StoreManagerResponse, StoreManager](
              server,
              e => RegisterStoreManager(StoreManager(e.username, e.store), e.password, _),
              _.result match {
                case Right(value) => complete(ResultResponseEntity(value))
                case Left(error) =>
                  error match {
                    case StoreManagerAlreadyPresent => complete(StatusCodes.BadRequest, ErrorResponseEntity(error))
                    case _ => complete(StatusCodes.InternalServerError, ErrorResponseEntity(error))
                  }
              }
            )
          },
          delete {
            route[StoreManagerDeregistrationEntity, DeregisterStoreManager, EmptyResponse, Unit](
              server,
              e => DeregisterStoreManager(e.username, e.password, _),
              r => handleStoreManagerAndPasswordResult(r.result)
            )
          },
          put {
            route[StoreManagerUpdateStoreEntity, UpdateStoreManagerStore, StoreManagerResponse, StoreManager](
              server,
              e => UpdateStoreManagerStore(e.username, e.newStore, _),
              r => handleStoreManagerResult(r.result)
            )
          }
        )
      },
      path("store_manager" / "login") {
        put {
          route[StoreManagerLoginEntity, LoginStoreManager, StoreManagerResponse, StoreManager](
            server,
            e => LoginStoreManager(e.username, e.password, _),
            _.result match {
              case Right(value) => complete(ResultResponseEntity(value))
              case Left(error) =>
                error match {
                  case StoreManagerNotFound | PasswordNotMatching =>
                    complete(StatusCodes.NotFound, ErrorResponseEntity(StoreManagerNotFound))
                  case _ => complete(StatusCodes.InternalServerError, ErrorResponseEntity(error))
                }
            }
          )
        }
      },
      path("store_manager" / "password") {
        put {
          route[StoreManagerUpdatePasswordEntity, UpdateStoreManagerPassword, EmptyResponse, Unit](
            server,
            e => UpdateStoreManagerPassword(e.username, e.password, e.newPassword, _),
            r => handleStoreManagerAndPasswordResult(r.result)
          )
        }
      }
    )
}
