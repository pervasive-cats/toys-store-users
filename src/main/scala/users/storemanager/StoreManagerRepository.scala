/*
 * Copyright © 2022-2023 by Pervasive Cats S.r.l.s.
 *
 * All Rights Reserved.
 */

package io.github.pervasivecats
package users.storemanager

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

import users.storemanager.entities.StoreManager
import users.user.valueobjects.{EncryptedPassword, PlainPassword, Username}
import users.{Validated, ValidationError}
import users.storemanager.valueobjects.Store
import users.user.Repository
import users.storemanager.StoreManagerRepositoryError.*
import AnyOps.*

trait StoreManagerRepository[A <: StoreManager] { // extends Repository[A] {

  /** can yield UserNotFound validation error */
  def findByUsername(username: Username): Validated[A]

  def register(storeManager: A, password: EncryptedPassword): Validated[Unit]

//  def updateStore(storeManager: A, store: Store): Validated[Unit]

//  def unregister(storeManager: A): Validated[Unit]

}

object StoreManagerRepository {

  final private class StoreManagerRepositoryImpl(port: Int) extends StoreManagerRepository[StoreManager] {

    final private case class StoreManagers(username: String, password: String, store: Long)

    private val config = new HikariConfig()
    config.setDataSourceClassName("org.postgresql.ds.PGSimpleDataSource")
    config.addDataSourceProperty("user", "test")
    config.addDataSourceProperty("password", "test")
    config.addDataSourceProperty("databaseName", "users")
    config.addDataSourceProperty("portNumber", port)

    private val ds = new HikariDataSource(config)

    private val ctx = PostgresJdbcContext[SnakeCase](SnakeCase, ds)

    import ctx.*

    override def findByUsername(username: Username): Validated[StoreManager] = {
      val r: Try[List[StoreManagers]] = Try(
        ctx.run(query[StoreManagers].filter(m => m.username.like(lift(username.value))))
      )

      r match {
        case Failure(_) => Left[ValidationError, StoreManager](RepositoryError)
        case Success(v) =>
          v.headOption match {
            case None => Left[ValidationError, StoreManager](UserNotFound)
            case Some(sm) =>
              for {
                username <- Username(sm.username)
                store <- Store(sm.store)
              } yield StoreManager(username, store)
          }
      }
    }

    override def register(storeManager: StoreManager, password: EncryptedPassword): Validated[Unit] = {

      val r = Try(
        ctx.run(
          query[StoreManagers]
            .insertValue(lift(StoreManagers(storeManager.username.value, password.value, storeManager.store.value)))
        )
      )

      r match {
        case Failure(_) => Left[ValidationError, Unit](RepositoryError)
        case Success(_) => Right[ValidationError, Unit](println("Store manager inserted"))
      }
    }
  }

  @SuppressWarnings(Array("org.wartremover.warts.Var", "scalafix:DisableSyntax.var"))
  private var instance: Option[StoreManagerRepositoryImpl] = None

  def getInstance(): Option[StoreManagerRepository[StoreManager]] = instance

  def apply(port: Int): StoreManagerRepository[StoreManager] = instance match {
    case Some(v) if v !=== port => {
      System.err.println("StoreManagerRepository already initialized with different port")
      v
    }
    case Some(v) => v
    case None => {
      val s = StoreManagerRepositoryImpl(port)
      instance = Some(s)
      s
    }
  }
}
