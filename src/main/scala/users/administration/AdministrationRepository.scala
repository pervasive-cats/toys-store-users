/*
 * Copyright © 2022-2023 by Pervasive Cats S.r.l.s.
 *
 * All Rights Reserved.
 */

package io.github.pervasivecats
package users.administration

import scala.util.Failure
import scala.util.Success
import scala.util.Try
import eu.timepit.refined.api.RefType.applyRef
import eu.timepit.refined.auto.autoUnwrap
import eu.timepit.refined.auto.given
import io.getquill.*
import org.postgresql.util.PSQLException
import users.user.valueobjects.{EncryptedPassword, PlainPassword, Username}
import users.{Validated, ValidationError}
import users.user.Repository
import users.administration.AdministrationRepositoryError.*

import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import users.administration.entities.Administration
import AnyOps.*

import users.user.services.PasswordAlgorithm

import scala.Console.println

trait AdministrationRepository[A <: Administration] {
  def findByUsername(username: Username): Validated[Administration]
  def findPassword(administration: Administration): Validated[EncryptedPassword]
  def updatePassword(administration: Administration, encryptedPassword: EncryptedPassword): Unit
}

object AdministrationRepository {
  final private class AdministrationRepositoryImpl(port: Int) extends AdministrationRepository[Administration]{

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

    case object OperationFailed extends ValidationError {

      override val message: String = "The operation on the given customer was not correctly performed"
    }

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
        .getOrElse(Left[ValidationError, Administration](UserNotFound))
    }

    /*{
      val r: Try[List[Administrators]] = Try(
        ctx.run(query[Administrators].filter(m => m.username.like(lift(username.value))))
      )

      r match {
        case Failure(_: PSQLException) => Left[ValidationError, Administration](PSQLError)
        case Failure(_) => Left[ValidationError, Administration](UnexpectedException)
        case Success(v) =>
          v.headOption match {
            case None => Left[ValidationError, Administration](UserNotFound)
            case Some(sm) =>
              for {
                username <- Username(sm.username)
              } yield Administration(username)
          }
      }
    }*/

    override def findPassword(administration: Administration): Validated[EncryptedPassword] = {
      ctx
        .run(
          query[Administrators]
            .filter(_.username === lift[String](administration.username.value))
            .map(_.password)
        )
        .map(EncryptedPassword(_))
        .headOption
        .toRight[ValidationError](UserNotFound)
    }

    override def updatePassword(administration: Administration, encryptedPassword: EncryptedPassword): Unit = {
      ctx
        .run(
          query[Administrators]
            .filter(_.username === lift[String](administration.username.value))
            .update(_.password -> lift(encryptedPassword.value))
        )

      /*summon[PasswordAlgorithm].encrypt(encryptedPassword) match {
        case Left(e) => Left[ValidationError, Unit](e)
        case Right(p) =>
          protectFromException {
            if (
              ctx.run(
                query[Customers]
                  .filter(_.email === lift[String](user.email.value))
                  .update(_.password -> lift[String](p.value))
              )
                !==
                1L
            )
              Left[ValidationError, Unit](OperationFailed)
            else
              Right[ValidationError, Unit](())
          }
      }*/
    }

  }

  @SuppressWarnings(Array("org.wartremover.warts.Var", "scalafix:DisableSyntax.var"))
  private var instance: Option[AdministrationRepositoryImpl] = None

  def getInstance(): Option[AdministrationRepository[Administration]] = instance

  def apply(port: Int): AdministrationRepository[Administration] = instance match {

    case Some(v) if v !=== port =>
      System.err.println("AdministrationRepository already initialized with different port")
      v

    case Some(v) => v
    case None =>
      val s = AdministrationRepositoryImpl(port)
      instance = Some(s)
      s
  }

}
