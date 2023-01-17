/*
 * Copyright © 2022-2023 by Pervasive Cats S.r.l.s.
 *
 * All Rights Reserved.
 */

package io.github.pervasivecats
package users.customer.entities

import io.github.pervasivecats.users.Validated

import users.customer.Repository as CustomerRepository
import users.customer.valeuobjects.{Email, NameComponent}
import users.user.Repository as UserRepository
import users.user.entities.User
import users.user.valueobjects.{EncryptedPassword, Username}

trait Customer extends User {

  val firstName: NameComponent

  val lastName: NameComponent

  val email: Email
}

object Customer {

  private case class CustomerImpl(firstName: NameComponent, lastName: NameComponent, email: Email, username: Username)
    extends Customer

  given CustomerOps[Customer] with {

    override def updated(
      customer: Customer,
      email: Email,
      firstName: NameComponent,
      lastName: NameComponent,
      username: Username
    ): Customer =
      CustomerImpl(firstName, lastName, email, username)
  }

  def apply(firstName: NameComponent, lastName: NameComponent, email: Email, username: Username): Customer =
    CustomerImpl(firstName, lastName, email, username)
}
