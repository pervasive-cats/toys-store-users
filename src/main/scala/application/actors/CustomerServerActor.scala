/*
 * Copyright © 2022-2023 by Pervasive Cats S.r.l.s.
 *
 * All Rights Reserved.
 */

package io.github.pervasivecats
package application.actors

import java.util.concurrent.ForkJoinPool
import javax.sql.DataSource

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

import application.RequestProcessingFailed
import commands.CustomerServerCommand.*
import commands.MessageBrokerCommand.CustomerUnregistered
import commands.RootCommand.Startup
import application.routes.entities.Response.{CustomerResponse, EmptyResponse}
import users.customer.Repository as CustomerRepository
import users.customer.entities.Customer
import users.customer.entities.CustomerOps.updated
import users.user.services.PasswordAlgorithm
import commands.{CustomerServerCommand, MessageBrokerCommand, RootCommand}
import users.customer.domainevents.CustomerUnregistered as CustomerUnregisteredEvent

object CustomerServerActor {

  def apply(
    root: ActorRef[RootCommand],
    dataSource: DataSource,
    messageBrokerActor: ActorRef[MessageBrokerCommand]
  ): Behavior[CustomerServerCommand] =
    Behaviors.setup[CustomerServerCommand] { ctx =>
      val customerRepository: CustomerRepository = CustomerRepository(dataSource)
      given ExecutionContext = ExecutionContext.fromExecutor(ForkJoinPool.commonPool())
      root ! Startup(success = true)
      Behaviors
        .receiveMessage[CustomerServerCommand] {
          case RegisterCustomer(customer, password, replyTo) =>
            Future(for {
              e <- summon[PasswordAlgorithm].encrypt(password)
              _ <- customerRepository.register(customer, e)
            } yield customer).onComplete {
              case Failure(_) => replyTo ! CustomerResponse(Left[ValidationError, Customer](RequestProcessingFailed))
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
              case Failure(_) => replyTo ! EmptyResponse(Left[ValidationError, Unit](RequestProcessingFailed))
              case Success(value) =>
                value.foreach(_ => messageBrokerActor ! CustomerUnregistered(CustomerUnregisteredEvent(email)))
                replyTo ! EmptyResponse(value)
            }(ctx.executionContext)
            Behaviors.same[CustomerServerCommand]
          case LoginCustomer(email, password, replyTo) =>
            Future(for {
              c <- customerRepository.findByEmail(email)
              e <- customerRepository.findPassword(c)
              _ <- summon[PasswordAlgorithm].check(e, password)
            } yield c).onComplete {
              case Failure(_) => replyTo ! CustomerResponse(Left[ValidationError, Customer](RequestProcessingFailed))
              case Success(value) => replyTo ! CustomerResponse(value)
            }(ctx.executionContext)
            Behaviors.same[CustomerServerCommand]
          case UpdateCustomerData(email, newEmail, newUsername, newFirstName, newLastName, replyTo) =>
            Future(for {
              c <- customerRepository.findByEmail(email)
              _ <- customerRepository.updateData(c, newFirstName, newLastName, newEmail, newUsername)
              n = c.updated(email = newEmail, username = newUsername, firstName = newFirstName, lastName = newLastName)
            } yield n).onComplete {
              case Failure(_) => replyTo ! CustomerResponse(Left[ValidationError, Customer](RequestProcessingFailed))
              case Success(value) => replyTo ! CustomerResponse(value)
            }(ctx.executionContext)
            Behaviors.same[CustomerServerCommand]
          case UpdateCustomerPassword(email, password, newPassword, replyTo) =>
            Future(for {
              c <- customerRepository.findByEmail(email)
              e <- customerRepository.findPassword(c)
              _ <- summon[PasswordAlgorithm].check(e, password)
              n <- summon[PasswordAlgorithm].encrypt(newPassword)
              _ <- customerRepository.updatePassword(c, n)
            } yield ()).onComplete {
              case Failure(_) => replyTo ! EmptyResponse(Left[ValidationError, Unit](RequestProcessingFailed))
              case Success(value) => replyTo ! EmptyResponse(value)
            }(ctx.executionContext)
            Behaviors.same[CustomerServerCommand]
        }
    }
}
