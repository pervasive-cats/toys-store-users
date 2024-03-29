/*
 * Copyright © 2022-2023 by Pervasive Cats S.r.l.s.
 *
 * All Rights Reserved.
 */

package io.github.pervasivecats
package users.customer.valueobjects

import io.github.pervasivecats.Validated
import io.github.pervasivecats.ValidationError

import eu.timepit.refined.api.RefType.applyRef
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.MatchesRegex

type NameComponentString = String Refined MatchesRegex["^[A-Z][a-z]{1,99}$"]

trait NameComponent {

  val value: NameComponentString
}

object NameComponent {

  private case class NameComponentImpl(value: NameComponentString) extends NameComponent

  case object WrongNameComponentFormat extends ValidationError {

    override val message: String = "The name component format is invalid"
  }

  def apply(value: String): Validated[NameComponent] = applyRef[NameComponentString](value) match {
    case Left(_) => Left[ValidationError, NameComponent](WrongNameComponentFormat)
    case Right(value) => Right[ValidationError, NameComponent](NameComponentImpl(value))
  }
}
