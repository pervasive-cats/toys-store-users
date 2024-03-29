/*
 * Copyright © 2022-2023 by Pervasive Cats S.r.l.s.
 *
 * All Rights Reserved.
 */

package io.github.pervasivecats
package users.administration

import javax.sql.DataSource

import scala.Console.println
import scala.util.Failure
import scala.util.Success
import scala.util.Try

import io.github.pervasivecats.Validated
import io.github.pervasivecats.ValidationError

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigValueFactory
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import eu.timepit.refined.api.RefType.applyRef
import eu.timepit.refined.auto.autoUnwrap
import eu.timepit.refined.auto.given
import io.getquill.*
import org.postgresql.util.PSQLException

import users.user.valueobjects.{EncryptedPassword, PlainPassword, Username}
import users.RepositoryOperationFailed
import users.user.Repository
import users.administration.entities.Administration
import users.user.Repository as UserRepository
import AnyOps.*
import users.user.services.PasswordAlgorithm

trait Repository extends UserRepository[Administration] {

  def findByUsername(username: Username): Validated[Administration]

  def findPassword(administration: Administration): Validated[EncryptedPassword]

  def updatePassword(administration: Administration, encryptedPassword: EncryptedPassword): Validated[Unit]
}

object Repository {

  case object AdministrationNotFound extends ValidationError {

    override val message: String = "No user found for the username that was provided"
  }

  final private class PostgresRepository(ctx: PostgresJdbcContext[SnakeCase]) extends Repository {

    import ctx.*

    private case class Administrators(username: String, password: String)

    private def protectFromException[A](f: => Validated[A]): Validated[A] =
      Try(f).getOrElse(Left[ValidationError, A](RepositoryOperationFailed))

    private def queryByUsername(username: Username) = quote {
      query[Administrators].filter(_.username === lift[String](username.value))
    }

    override def findByUsername(username: Username): Validated[Administration] = protectFromException {
      ctx
        .run(queryByUsername(username))
        .map(a => Username(a.username))
        .map(_.map(u => Administration(u)))
        .headOption
        .getOrElse(Left[ValidationError, Administration](AdministrationNotFound))
    }

    override def findPassword(administration: Administration): Validated[EncryptedPassword] = protectFromException {
      ctx
        .run(
          query[Administrators].filter(_.username === lift[String](administration.username.value)).map(_.password)
        )
        .map(EncryptedPassword(_))
        .headOption
        .getOrElse(Left[ValidationError, EncryptedPassword](AdministrationNotFound))
    }

    override def updatePassword(administration: Administration, encryptedPassword: EncryptedPassword): Validated[Unit] = {
      if (
        ctx.run(
          query[Administrators]
            .filter(_.username === lift[String](administration.username.value))
            .update(_.password -> lift[String](encryptedPassword.value))
        )
        !==
        1L
      )
        Left[ValidationError, Unit](RepositoryOperationFailed)
      else
        Right[ValidationError, Unit](())
    }
  }

  def apply(dataSource: DataSource): Repository = PostgresRepository(PostgresJdbcContext[SnakeCase](SnakeCase, dataSource))
}
