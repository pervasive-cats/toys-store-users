/*
 * Copyright © 2022-2023 by Pervasive Cats S.r.l.s.
 *
 * All Rights Reserved.
 */

package io.github.pervasivecats
package users.customer

import scala.util.Try

import io.github.pervasivecats.Validated
import io.github.pervasivecats.ValidationError

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigValueFactory
import eu.timepit.refined.auto.given
import io.getquill.*

import users.RepositoryOperationFailed
import users.customer.entities.Customer
import users.customer.valueobjects.{Email, NameComponent}
import users.user.Repository as UserRepository
import users.user.valueobjects.*
import AnyOps.*
import users.user.services.PasswordAlgorithm

trait Repository extends UserRepository[Customer] {

  def findByEmail(email: Email): Validated[Customer]

  def register(customer: Customer, password: EncryptedPassword): Validated[Unit]

  def updateData(
    customer: Customer,
    firstName: NameComponent,
    lastName: NameComponent,
    email: Email,
    username: Username
  ): Validated[Unit]

  def deregister(customer: Customer): Validated[Unit]
}

object Repository {

  case object CustomerNotFound extends ValidationError {

    override val message: String = "The queried customer was not found"
  }

  case object CustomerAlreadyPresent extends ValidationError {

    override val message: String = "The customer was already registered"
  }

  private class PostgresRepository(ctx: PostgresJdbcContext[SnakeCase]) extends Repository {

    import ctx.*

    private case class CustomersWithoutPassword(email: String, username: String, firstName: String, lastName: String)

    private case class Customers(email: String, username: String, password: String, firstName: String, lastName: String)

    private def protectFromException[A](f: => Validated[A]): Validated[A] =
      Try(f).getOrElse(Left[ValidationError, A](RepositoryOperationFailed))

    private def queryByEmail(email: Email) = quote {
      querySchema[CustomersWithoutPassword](entity = "customers").filter(_.email === lift[String](email.value))
    }

    override def findByEmail(email: Email): Validated[Customer] = protectFromException {
      ctx
        .run(queryByEmail(email))
        .map(c =>
          for {
            e <- Email(c.email)
            u <- Username(c.username)
            f <- NameComponent(c.firstName)
            l <- NameComponent(c.lastName)
          } yield Customer(f, l, e, u)
        )
        .headOption
        .getOrElse(Left[ValidationError, Customer](CustomerNotFound))
    }

    override def findPassword(user: Customer): Validated[EncryptedPassword] = protectFromException {
      ctx
        .run(
          query[Customers].filter(_.email === lift[String](user.email.value)).map(_.password)
        )
        .map(EncryptedPassword(_))
        .headOption
        .getOrElse(Left[ValidationError, EncryptedPassword](CustomerNotFound))
    }

    override def register(customer: Customer, password: EncryptedPassword): Validated[Unit] = protectFromException {
      ctx.transaction {
        if (ctx.run(queryByEmail(customer.email).nonEmpty))
          Left[ValidationError, Unit](CustomerAlreadyPresent)
        else if (
          ctx.run(
            query[Customers]
              .insertValue(
                lift(
                  Customers(
                    customer.email.value,
                    customer.username.value,
                    password.value,
                    customer.firstName.value,
                    customer.lastName.value
                  )
                )
              )
          )
          !==
          1L
        )
          Left[ValidationError, Unit](RepositoryOperationFailed)
        else
          Right[ValidationError, Unit](())
      }
    }

    override def updateData(
      customer: Customer,
      firstName: NameComponent,
      lastName: NameComponent,
      email: Email,
      username: Username
    ): Validated[Unit] = protectFromException {
      if (
        ctx.run(
          querySchema[CustomersWithoutPassword](entity = "customers")
            .filter(_.email === lift[String](customer.email.value))
            .updateValue(lift(CustomersWithoutPassword(email.value, username.value, firstName.value, lastName.value)))
        )
        !==
        1L
      )
        Left[ValidationError, Unit](RepositoryOperationFailed)
      else
        Right[ValidationError, Unit](())
    }

    override def updatePassword(user: Customer, password: EncryptedPassword): Validated[Unit] = protectFromException {
      if (
        ctx.run(
          query[Customers].filter(_.email === lift[String](user.email.value)).update(_.password -> lift[String](password.value))
        )
        !==
        1L
      )
        Left[ValidationError, Unit](RepositoryOperationFailed)
      else
        Right[ValidationError, Unit](())
    }

    override def deregister(customer: Customer): Validated[Unit] = protectFromException {
      if (ctx.run(query[Customers].filter(_.email === lift[String](customer.email.value)).delete) !== 1L)
        Left[ValidationError, Unit](RepositoryOperationFailed)
      else
        Right[ValidationError, Unit](())
    }
  }

  def apply(config: Config): Repository = PostgresRepository(PostgresJdbcContext[SnakeCase](SnakeCase, config))
}
