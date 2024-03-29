/*
 * Copyright © 2022-2023 by Pervasive Cats S.r.l.s.
 *
 * All Rights Reserved.
 */

package io.github.pervasivecats
package users.storemanager

import javax.sql.DataSource

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

import users.storemanager.entities.StoreManager
import users.user.valueobjects.{EncryptedPassword, PlainPassword, Username}
import users.storemanager.valueobjects.Store
import users.user.Repository as UserRepository
import AnyOps.*
import users.RepositoryOperationFailed

trait Repository extends UserRepository[StoreManager] {

  def findByUsername(username: Username): Validated[StoreManager]

  def register(storeManager: StoreManager, password: EncryptedPassword): Validated[Unit]

  def updateStore(storeManager: StoreManager, store: Store): Validated[Unit]

  def unregister(storeManager: StoreManager): Validated[Unit]
}

object Repository {

  case object StoreManagerAlreadyPresent extends ValidationError {

    override val message: String = "The store manager was already registered"
  }

  case object StoreManagerNotFound extends ValidationError {

    override val message: String = "No store manager found for the username that was provided"
  }

  private class PostgresRepository(ctx: PostgresJdbcContext[SnakeCase]) extends Repository {

    import ctx.*

    private case class StoreManagers(username: String, password: String, store: Long)

    private def protectFromException[A](f: => Validated[A]): Validated[A] =
      Try(f).getOrElse(Left[ValidationError, A](RepositoryOperationFailed))

    private def queryByUsername(username: Username) = quote {
      querySchema[StoreManagers](entity = "store_managers").filter(_.username === lift[String](username.value))
    }

    override def findByUsername(username: Username): Validated[StoreManager] = protectFromException {
      ctx
        .run(queryByUsername(username))
        .map(sm =>
          for {
            username <- Username(sm.username)
            store <- Store(sm.store)
          } yield StoreManager(username, store)
        )
        .headOption
        .getOrElse(Left[ValidationError, StoreManager](StoreManagerNotFound))
    }

    override def register(storeManager: StoreManager, password: EncryptedPassword): Validated[Unit] = protectFromException {
      ctx.transaction {
        if (ctx.run(queryByUsername(storeManager.username).nonEmpty))
          Left[ValidationError, Unit](StoreManagerAlreadyPresent)
        else if (
          ctx.run(
            query[StoreManagers].insertValue(
              lift(StoreManagers(storeManager.username.value, password.value, storeManager.store.id))
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

    override def updateStore(storeManager: StoreManager, store: Store): Validated[Unit] = protectFromException {
      if (ctx.run(queryByUsername(storeManager.username).update(_.store -> lift(store.id.value))) !== 1L)
        Left[ValidationError, Unit](RepositoryOperationFailed)
      else
        Right[ValidationError, Unit](())
    }

    override def findPassword(user: StoreManager): Validated[EncryptedPassword] = protectFromException {
      ctx
        .run(queryByUsername(user.username).map(_.password))
        .map(EncryptedPassword(_))
        .headOption
        .getOrElse(Left[ValidationError, EncryptedPassword](StoreManagerNotFound))
    }

    override def updatePassword(user: StoreManager, password: EncryptedPassword): Validated[Unit] = protectFromException {
      if (ctx.run(queryByUsername(user.username).update(_.password -> lift[String](password.value))) !== 1L)
        Left[ValidationError, Unit](RepositoryOperationFailed)
      else
        Right[ValidationError, Unit](())
    }

    override def unregister(storeManager: StoreManager): Validated[Unit] = protectFromException {
      if (ctx.run(queryByUsername(storeManager.username).delete) !== 1L)
        Left[ValidationError, Unit](RepositoryOperationFailed)
      else
        Right[ValidationError, Unit](())
    }
  }

  def apply(dataSource: DataSource): Repository = PostgresRepository(PostgresJdbcContext[SnakeCase](SnakeCase, dataSource))
}
