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

trait StoreManagerRepository[A <: StoreManager] { // extends Repository[A] {

//  def findByUsername(username: Username): Validated[A]

  def register(storeManager: A, password: EncryptedPassword): Validated[Unit]

//  def updateStore(storeManager: A, store: Store): Validated[Unit]

//  def unregister(storeManager: A): Validated[Unit]

}

object StoreManagerRepository {

  case class StoreManagers(username: String, password: String, store: Long)

  case object PSQLError extends ValidationError {

    override val message: String = "The Postgresql operation failed"
  }

  case object UniqueViolation extends ValidationError {

    override val message: String = "Username already in use"
  }

  case object UnexpectedException extends ValidationError {

    override val message: String = "An unexpected exception has occurred"
  }

  given StoreManagerRepository[StoreManager] with {
    private val ctx = PostgresJdbcContext[SnakeCase](SnakeCase, "ctx")

    import ctx.*

//    override def findByUsername(username: Username): Validated[StoreManager] = ???

//    override def findPassword(user: StoreManager): Validated[EncryptedPassword] = ???

    override def register(storeManager: StoreManager, password: EncryptedPassword): Validated[Unit] = {
      val r = Try(
        ctx.run(
          query[StoreManagers]
            .insertValue(lift(StoreManagers(storeManager.username.value, password.value, storeManager.store.value)))
        )
      )

      r match {
        case Failure(e: PSQLException) if e.getSQLState.contentEquals("23505") => Left[ValidationError, Unit](UniqueViolation)
        case Failure(e: PSQLException) => Left[ValidationError, Unit](PSQLError)
        case Failure(e) => Left[ValidationError, Unit](UnexpectedException)
        case Success(_) => Right[ValidationError, Unit](println("Store manager inserted"))
      }
    }

//    override def updateStore(storeManager: StoreManager, store: Store): Validated[Unit] = ???

//    override def updatePassword(user: StoreManager, password: PlainPassword): Validated[Unit] = ???

//    override def unregister(storeManager: StoreManager): Validated[Unit] = ???
  }
}

@main
def testRegister(): Unit = {

  for {
    username <- Username("mario")
    store <- Store(1)
  } do {
    val r = summon[StoreManagerRepository[StoreManager]].register(StoreManager(username, store), EncryptedPassword("p1"))
    println(r)
  }

}
