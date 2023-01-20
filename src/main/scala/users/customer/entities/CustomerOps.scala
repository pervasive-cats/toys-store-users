/*
 * Copyright © 2022-2023 by Pervasive Cats S.r.l.s.
 *
 * All Rights Reserved.
 */

package io.github.pervasivecats
package users.customer.entities

import users.customer.valueobjects.{Email, NameComponent}
import users.user.valueobjects.Username

trait CustomerOps[A <: Customer] {

  def updated(customer: A, email: Email, firstName: NameComponent, lastName: NameComponent, username: Username): A
}

object CustomerOps {

  extension [A <: Customer: CustomerOps](customer: A) {

    @SuppressWarnings(Array("org.wartremover.warts.DefaultArguments", "scalafix:DisableSyntax.defaultArgs"))
    def updated(
      email: Email = customer.email,
      firstName: NameComponent = customer.firstName,
      lastName: NameComponent = customer.lastName,
      username: Username = customer.username
    ): A =
      implicitly[CustomerOps[A]].updated(customer, email, firstName, lastName, username)
  }
}
