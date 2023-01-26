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

import akka.actor.ActorSystem
import akka.actor.typed.*
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import com.typesafe.config.Config

import application.actors.{CustomerServerCommand, MessageBrokerCommand, RootCommand}
import application.actors.CustomerServerCommand.*
import application.actors.MessageBrokerCommand.CustomerUnregistered
import application.actors.RootCommand.Startup
import application.routes.Response.{CustomerResponse, EmptyResponse}
import users.customer.Repository as CustomerRepository
import users.ValidationError
import users.customer.entities.Customer
import users.customer.entities.CustomerOps.updated
import users.user.services.PasswordAlgorithm

object CustomerServerActor {

  case object ProcessingFailed extends ValidationError {

    override val message: String = "The request processing has failed"
  }

  def apply(
    root: ActorRef[RootCommand],
    repositoryConfig: Config,
    messageBrokerActor: ActorRef[MessageBrokerCommand]
  ): Behavior[CustomerServerCommand] =
    Behaviors.setup[CustomerServerCommand] { ctx =>
      val customerRepository: CustomerRepository = CustomerRepository(repositoryConfig)
      given ExecutionContext = ExecutionContext.fromExecutor(ForkJoinPool.commonPool())
      root ! Startup(success = true)
      Behaviors
        .receiveMessage[CustomerServerCommand] {
          case RegisterCustomer(customer, password, replyTo) =>
            Future(for {
              e <- summon[PasswordAlgorithm].encrypt(password)
              _ <- customerRepository.register(customer, e)
            } yield customer).onComplete {
              case Failure(_) => replyTo ! CustomerResponse(Left[ValidationError, Customer](ProcessingFailed))
              case Success(value) => replyTo ! CustomerResponse(value)
            }(ctx.executionContext)
            Behaviors.same[CustomerServerCommand]
          case DeregisterCustomer(email, password, replyTo) =>
            Future(for {
              c <- customerRepository.findByEmail(email)
              e <- customerRepository.findPassword(c)
              _ <- summon[PasswordAlgorithm].check(e, password)
              _ <- customerRepository.deregister(c)
            } yield ()).onComplete {
              case Failure(_) => replyTo ! EmptyResponse(Left[ValidationError, Unit](ProcessingFailed))
              case Success(value) =>
                value.foreach(_ => messageBrokerActor ! CustomerUnregistered(email))
                replyTo ! EmptyResponse(value)
            }(ctx.executionContext)
            Behaviors.same[CustomerServerCommand]
          case LoginCustomer(email, password, replyTo) =>
            Future(for {
              c <- customerRepository.findByEmail(email)
              e <- customerRepository.findPassword(c)
              _ <- summon[PasswordAlgorithm].check(e, password)
            } yield c).onComplete {
              case Failure(_) => replyTo ! CustomerResponse(Left[ValidationError, Customer](ProcessingFailed))
              case Success(value) => replyTo ! CustomerResponse(value)
            }
            Behaviors.same[CustomerServerCommand]
          case UpdateCustomerData(email, newEmail, newUsername, newFirstName, newLastName, replyTo) =>
            Future(for {
              c <- customerRepository.findByEmail(email)
              _ <- customerRepository.updateData(c, newFirstName, newLastName, newEmail, newUsername)
              n = c.updated(email = newEmail, username = newUsername, firstName = newFirstName, lastName = newLastName)
            } yield n).onComplete {
              case Failure(_) => replyTo ! CustomerResponse(Left[ValidationError, Customer](ProcessingFailed))
              case Success(value) => replyTo ! CustomerResponse(value)
            }
            Behaviors.same[CustomerServerCommand]
          case UpdateCustomerPassword(email, password, newPassword, replyTo) =>
            Future(for {
              c <- customerRepository.findByEmail(email)
              e <- customerRepository.findPassword(c)
              _ <- summon[PasswordAlgorithm].check(e, password)
              n <- summon[PasswordAlgorithm].encrypt(newPassword)
              _ <- customerRepository.updatePassword(c, n)
            } yield ()).onComplete {
              case Failure(_) => replyTo ! EmptyResponse(Left[ValidationError, Unit](ProcessingFailed))
              case Success(value) => replyTo ! EmptyResponse(value)
            }
            Behaviors.same[CustomerServerCommand]
        }
    }
}
