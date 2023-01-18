/*
 * Copyright © 2022-2023 by Pervasive Cats S.r.l.s.
 *
 * All Rights Reserved.
 */

package io.github.pervasivecats
package users.administration

import scala.Console.println
import scala.util.Failure
import scala.util.Success
import scala.util.Try

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import eu.timepit.refined.api.RefType.applyRef
import eu.timepit.refined.auto.autoUnwrap
import eu.timepit.refined.auto.given
import io.getquill.*
import org.postgresql.util.PSQLException

import users.user.valueobjects.{EncryptedPassword, PlainPassword, Username}
import users.{Validated, ValidationError}
import users.user.Repository
import users.administration.AdministrationRepositoryError.*
import users.administration.entities.Administration
import AnyOps.*
import users.user.services.PasswordAlgorithm

trait AdministrationRepository[A <: Administration] {
  def findByUsername(username: Username): Validated[Administration]
  def findPassword(administration: Administration): Validated[EncryptedPassword]
  def updatePassword(administration: Administration, encryptedPassword: EncryptedPassword): Validated[Unit]
}

object AdministrationRepository {

  final private class AdministrationRepositoryImpl(port: Int) extends AdministrationRepository[Administration] {

    final case class Administrators(username: String, password: String)

    private val config = new HikariConfig()
    config.setDataSourceClassName("org.postgresql.ds.PGSimpleDataSource")
    config.addDataSourceProperty("user", "ismam")
    config.addDataSourceProperty("password", "ismam")
    config.addDataSourceProperty("databaseName", "users")
    config.addDataSourceProperty("portNumber", port)

    private val ds = new HikariDataSource(config)

    private val ctx = PostgresJdbcContext[SnakeCase](SnakeCase, ds)

    import ctx.*

    private def protectFromException[A](f: => Validated[A]): Validated[A] =
      Try(f).getOrElse(Left[ValidationError, A](OperationFailed))

    private def queryByUsername(username: Username) = quote {
      querySchema[Administrators](entity = "administrators").filter(_.username === lift[String](username.value))
    }

    override def findByUsername(username: Username): Validated[Administration] = protectFromException {
      ctx
        .run(queryByUsername(username))
        .map(c =>
          for {
            u <- Username(c.username)
          } yield Administration(u)
        )
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
        .toRight[ValidationError](AdministrationNotFound)
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
        Left[ValidationError, Unit](OperationFailed)
      else
        Right[ValidationError, Unit](())
    }

  }

  @SuppressWarnings(Array("org.wartremover.warts.Var", "scalafix:DisableSyntax.var"))
  private var instance: Option[AdministrationRepositoryImpl] = None

  def getInstance(): Option[AdministrationRepository[Administration]] = instance

  def apply(port: Int): AdministrationRepository[Administration] = instance match {

    case Some(v) if v !== port =>
      System.err.println("AdministrationRepository already initialized with different port")
      v

    case Some(v) => v
    case None =>
      val s = AdministrationRepositoryImpl(port)
      instance = Some(s)
      s
  }

}
