/*
 * Copyright © 2022-2023 by Pervasive Cats S.r.l.s.
 *
 * All Rights Reserved.
 */

package io.github.pervasivecats
package users.customer.events

import users.customer.valueobjects.Email

trait CustomerUnregistered {

  val email: Email
}

object CustomerUnregistered {

  private case class CustomerUnregisteredImpl(email: Email) extends CustomerUnregistered

  def apply(email: Email): CustomerUnregistered = CustomerUnregisteredImpl(email)
}
