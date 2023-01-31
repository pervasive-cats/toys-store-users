/*
 * Copyright © 2022-2023 by Pervasive Cats S.r.l.s.
 *
 * All Rights Reserved.
 */

package io.github.pervasivecats
package application.routes

import io.github.pervasivecats.Validated
import io.github.pervasivecats.users.administration.entities.Administration
import io.github.pervasivecats.users.storemanager.entities.StoreManager

import users.customer.entities.Customer

sealed trait Response[A] {

  val result: Validated[A]
}

object Response {

  final case class CustomerResponse(result: Validated[Customer]) extends Response[Customer]

  final case class StoreManagerResponse(result: Validated[StoreManager]) extends Response[StoreManager]

  final case class AdministrationResponse(result: Validated[Administration]) extends Response[Administration]

  final case class EmptyResponse(result: Validated[Unit]) extends Response[Unit]
}
