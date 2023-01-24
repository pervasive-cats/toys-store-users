/*
 * Copyright © 2022-2023 by Pervasive Cats S.r.l.s.
 *
 * All Rights Reserved.
 */

package io.github.pervasivecats
package application.routes

import users.customer.entities.Customer
import users.Validated

sealed trait Response[A] {

  val result: Validated[A]
}

object Response {

  final case class CustomerResponse(result: Validated[Customer]) extends Response[Customer]

  final case class EmptyResponse(result: Validated[Unit]) extends Response[Unit]
}
