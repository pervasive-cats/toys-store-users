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
import application.routes.Entity.{ErrorResponseEntity, ResultResponseEntity}
import application.routes.Response.{AdministrationResponse, EmptyResponse}
import application.Serializers.given
import application.routes.Routes.RequestFailed
import application.routes.AdministrationEntity.*
import users.user.services.PasswordAlgorithm.PasswordNotMatching
import users.user.valueobjects.{PlainPassword, Username}
import application.actors.AdministrationServerCommand
import application.actors.AdministrationServerCommand.{LoginAdministration, UpdateAdministrationPassword}
import application.routes.AdministrationEntity.{AdministrationLoginEntity, AdministrationUpdatePasswordEntity}
import users.administration.entities.Administration
import users.administration.Repository.AdministrationNotFound

private object AdministrationRoutes extends SprayJsonSupport with DefaultJsonProtocol with Directives {

  private given Timeout = 30.seconds

  private def route[A: FromRequestUnmarshaller, B <: AdministrationServerCommand, C <: Response[D], D: JsonWriter](
    server: ActorRef[AdministrationServerCommand],
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

  def apply(server: ActorRef[AdministrationServerCommand])(using ActorSystem[_]): Route =
    concat(
      path("administration" / "login") {
        put {
          route[AdministrationLoginEntity, LoginAdministration, AdministrationResponse, Administration](
            server,
            e => LoginAdministration(e.username, e.password, _),
            _.result match {
              case Right(value) => complete(ResultResponseEntity(value))
              case Left(error) =>
                error match {
                  case AdministrationNotFound | PasswordNotMatching =>
                    complete(StatusCodes.NotFound, ErrorResponseEntity(AdministrationNotFound))
                  case _ => complete(StatusCodes.InternalServerError, ErrorResponseEntity(error))
                }
            }
          )
        }
      },
      path("administration" / "password") {
        put {
          route[AdministrationUpdatePasswordEntity, UpdateAdministrationPassword, EmptyResponse, Unit](
            server,
            e => UpdateAdministrationPassword(e.username, e.password, e.newPassword, _),
            _.result match {
              case Right(value) => complete(ResultResponseEntity(value))
              case Left(error) =>
                error match {
                  case AdministrationNotFound => complete(StatusCodes.NotFound, ErrorResponseEntity(error))
                  case PasswordNotMatching => complete(StatusCodes.BadRequest, ErrorResponseEntity(error))
                  case _ => complete(StatusCodes.InternalServerError, ErrorResponseEntity(error))
                }
            }
          )
        }
      }
    )
}
