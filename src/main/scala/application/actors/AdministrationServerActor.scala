/*
 * Copyright © 2022-2023 by Pervasive Cats S.r.l.s.
 *
 * All Rights Reserved.
 */

package io.github.pervasivecats
package application.actors

import java.util.concurrent.ForkJoinPool

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.Failure
import scala.util.Success

import io.github.pervasivecats.ValidationError
import io.github.pervasivecats.users.administration.entities.Administration

import akka.actor.ActorSystem
import akka.actor.typed.*
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import com.typesafe.config.Config

import application.actors.{AdministrationServerCommand, RootCommand}
import application.actors.RootCommand.Startup
import application.actors.AdministrationServerCommand.*
import application.routes.Response.{AdministrationResponse, EmptyResponse}
import application.RequestProcessingFailed
import users.customer.entities.Customer
import users.administration.Repository as AdministrationRepository
import users.user.services.PasswordAlgorithm

object AdministrationServerActor {

  def apply(root: ActorRef[RootCommand], repositoryConfig: Config): Behavior[AdministrationServerCommand] =
    Behaviors.setup[AdministrationServerCommand] { ctx =>
      val administrationRepository: AdministrationRepository = AdministrationRepository(repositoryConfig)
      given ExecutionContext = ExecutionContext.fromExecutor(ForkJoinPool.commonPool())
      root ! Startup(success = true)
      Behaviors
        .receiveMessage[AdministrationServerCommand] {
          case LoginAdministration(username, password, replyTo) =>
            Future(for {
              a <- administrationRepository.findByUsername(username)
              e <- administrationRepository.findPassword(a)
              _ <- summon[PasswordAlgorithm].check(e, password)
            } yield a).onComplete {
              case Failure(_) => replyTo ! AdministrationResponse(Left[ValidationError, Administration](RequestProcessingFailed))
              case Success(value) => replyTo ! AdministrationResponse(value)
            }
            Behaviors.same[AdministrationServerCommand]
          case UpdateAdministrationPassword(username, password, newPassword, replyTo) =>
            Future(for {
              a <- administrationRepository.findByUsername(username)
              e <- administrationRepository.findPassword(a)
              _ <- summon[PasswordAlgorithm].check(e, password)
              n <- summon[PasswordAlgorithm].encrypt(newPassword)
              _ <- administrationRepository.updatePassword(a, n)
            } yield ()).onComplete {
              case Failure(_) => replyTo ! EmptyResponse(Left[ValidationError, Unit](RequestProcessingFailed))
              case Success(value) => replyTo ! EmptyResponse(value)
            }
            Behaviors.same[AdministrationServerCommand]
        }
    }
}
