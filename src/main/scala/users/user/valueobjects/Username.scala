/*
 * Copyright © 2022-2023 by Pervasive Cats S.r.l.s.
 *
 * All Rights Reserved.
 */

package io.github.pervasivecats
package users.user.valueobjects

import io.github.pervasivecats.Validated
import io.github.pervasivecats.ValidationError

import eu.timepit.refined.api.RefType.applyRef
import eu.timepit.refined.api.Refined
import eu.timepit.refined.auto.given
import eu.timepit.refined.string.MatchesRegex

type UsernameString = String Refined MatchesRegex["^\\w{1,100}$"]

trait Username {

  val value: UsernameString
}

object Username {

  final private case class UsernameImpl(value: UsernameString) extends Username

  case object WrongUsernameFormat extends ValidationError {

    override val message: String = "The username format is invalid"
  }

  def apply(value: String): Validated[Username] = applyRef[UsernameString](value) match {
    case Left(_) => Left[ValidationError, Username](WrongUsernameFormat)
    case Right(value) => Right[ValidationError, Username](UsernameImpl(value))
  }
}
