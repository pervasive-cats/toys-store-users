/*
 * Copyright © 2022-2023 by Pervasive Cats S.r.l.s.
 *
 * All Rights Reserved.
 */

package io.github.pervasivecats
package users.storemanager.valueobjects

import io.github.pervasivecats.Validated
import io.github.pervasivecats.ValidationError

import eu.timepit.refined.api.RefType.applyRef
import eu.timepit.refined.api.Refined
import eu.timepit.refined.auto.given
import eu.timepit.refined.numeric.NonNegative

type StoreId = Long Refined NonNegative

trait Store {

  val id: StoreId
}

object Store {

  final private case class StoreImpl(id: StoreId) extends Store

  case object WrongStoreIdFormat extends ValidationError {

    override val message: String = "The store id is a negative value"
  }

  def apply(value: Long): Validated[Store] = applyRef[StoreId](value) match {
    case Left(_) => Left[ValidationError, Store](WrongStoreIdFormat)
    case Right(value) => Right[ValidationError, Store](StoreImpl(value))
  }
}
