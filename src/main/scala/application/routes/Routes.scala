/*
 * Copyright © 2022-2023 by Pervasive Cats S.r.l.s.
 *
 * All Rights Reserved.
 */

package io.github.pervasivecats
package application.routes

import io.github.pervasivecats.ValidationError

import akka.actor.typed.ActorRef
import akka.actor.typed.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.*
import spray.json.DefaultJsonProtocol
import spray.json.DeserializationException

import application.actors.{CustomerServerCommand, StoreManagerServerCommand}
import application.routes.CustomerRoutes.complete
import application.routes.Entity.ErrorResponseEntity
import application.routes.Entity.given

object Routes extends Directives with SprayJsonSupport with DefaultJsonProtocol {

  case object RequestFailed extends ValidationError {

    override val message: String = "An error has occurred while processing the request"
  }

  case class DeserializationFailed(message: String) extends ValidationError

  private val rejectionHandler: RejectionHandler =
    RejectionHandler
      .newBuilder()
      .handle {
        case MalformedRequestContentRejection(msg, _) =>
          complete(StatusCodes.BadRequest, ErrorResponseEntity(DeserializationFailed(msg)))
      }
      .result()

  def apply(
    customerServer: ActorRef[CustomerServerCommand],
    storeManagerServer: ActorRef[StoreManagerServerCommand]
  )(
    using
    ActorSystem[_]
  ): Route = handleRejections(rejectionHandler) {
    concat(CustomerRoutes(customerServer), StoreManagerRoutes(storeManagerServer))
  }
}
