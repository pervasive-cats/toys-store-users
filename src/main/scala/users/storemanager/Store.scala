/*
 * Copyright © 2022-2023 by Pervasive Cats S.r.l.s.
 *
 * All Rights Reserved.
 */

package io.github.pervasivecats
package users.storemanager

import eu.timepit.refined.api.RefType.applyRef
import eu.timepit.refined.api.Refined
import eu.timepit.refined.auto.given
import eu.timepit.refined.numeric.NonNegative

import users.{Validated, ValidationError}

type StoreID = Long Refined NonNegative

trait Store {

  val value: StoreID
}

object Store {

  final private case class StoreImpl(value: StoreID) extends Store

  case object WrongStoreFormat extends ValidationError {

    override val message: String = "The store id is a negative value"
  }

  def apply(value: Long): Validated[Store] = applyRef[StoreID](value) match {
    case Left(_) => Left[ValidationError, Store](WrongStoreFormat)
    case Right(value) => Right[ValidationError, Store](StoreImpl(value))
  }
}
