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

import akka.actor.ActorSystem
import akka.actor.typed.*
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import com.typesafe.config.Config

import application.actors.{RootCommand, StoreManagerServerCommand}
import application.actors.RootCommand.Startup
import application.actors.StoreManagerServerCommand.*
import application.routes.Response.{EmptyResponse, StoreManagerResponse}
import application.RequestProcessingFailed
import users.customer.entities.Customer
import users.storemanager.Repository as StoreManagerRepository
import users.storemanager.entities.StoreManager
import users.storemanager.entities.StoreManagerOps.updateStore
import users.user.services.PasswordAlgorithm

object StoreManagerServerActor {

  def apply(root: ActorRef[RootCommand], repositoryConfig: Config): Behavior[StoreManagerServerCommand] =
    Behaviors.setup[StoreManagerServerCommand] { ctx =>
      val storeManagerRepository: StoreManagerRepository = StoreManagerRepository(repositoryConfig)
      given ExecutionContext = ExecutionContext.fromExecutor(ForkJoinPool.commonPool())
      root ! Startup(success = true)
      Behaviors
        .receiveMessage[StoreManagerServerCommand] {
          case RegisterStoreManager(storeManager, password, replyTo) =>
            Future(for {
              e <- summon[PasswordAlgorithm].encrypt(password)
              _ <- storeManagerRepository.register(storeManager, e)
            } yield storeManager).onComplete {
              case Failure(_) => replyTo ! StoreManagerResponse(Left[ValidationError, StoreManager](RequestProcessingFailed))
              case Success(value) => replyTo ! StoreManagerResponse(value)
            }(ctx.executionContext)
            Behaviors.same[StoreManagerServerCommand]
          case DeregisterStoreManager(username, password, replyTo) =>
            Future(for {
              m <- storeManagerRepository.findByUsername(username)
              e <- storeManagerRepository.findPassword(m)
              _ <- summon[PasswordAlgorithm].check(e, password)
              _ <- storeManagerRepository.unregister(m)
            } yield ()).onComplete {
              case Failure(_) => replyTo ! EmptyResponse(Left[ValidationError, Unit](RequestProcessingFailed))
              case Success(value) => replyTo ! EmptyResponse(value)
            }(ctx.executionContext)
            Behaviors.same[StoreManagerServerCommand]
          case LoginStoreManager(username, password, replyTo) =>
            Future(for {
              m <- storeManagerRepository.findByUsername(username)
              e <- storeManagerRepository.findPassword(m)
              _ <- summon[PasswordAlgorithm].check(e, password)
            } yield m).onComplete {
              case Failure(_) => replyTo ! StoreManagerResponse(Left[ValidationError, StoreManager](RequestProcessingFailed))
              case Success(value) => replyTo ! StoreManagerResponse(value)
            }
            Behaviors.same[StoreManagerServerCommand]
          case UpdateStoreManagerStore(username, newStore, replyTo) =>
            Future(for {
              m <- storeManagerRepository.findByUsername(username)
              _ <- storeManagerRepository.updateStore(m, newStore)
              n = m.updateStore(newStore)
            } yield n).onComplete {
              case Failure(_) => replyTo ! StoreManagerResponse(Left[ValidationError, StoreManager](RequestProcessingFailed))
              case Success(value) => replyTo ! StoreManagerResponse(value)
            }
            Behaviors.same[StoreManagerServerCommand]
          case UpdateStoreManagerPassword(username, password, newPassword, replyTo) =>
            Future(for {
              m <- storeManagerRepository.findByUsername(username)
              e <- storeManagerRepository.findPassword(m)
              _ <- summon[PasswordAlgorithm].check(e, password)
              n <- summon[PasswordAlgorithm].encrypt(newPassword)
              _ <- storeManagerRepository.updatePassword(m, n)
            } yield ()).onComplete {
              case Failure(_) => replyTo ! EmptyResponse(Left[ValidationError, Unit](RequestProcessingFailed))
              case Success(value) => replyTo ! EmptyResponse(value)
            }
            Behaviors.same[StoreManagerServerCommand]
        }
    }
}
