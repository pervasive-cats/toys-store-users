/*
 * Copyright © 2022-2023 by Pervasive Cats S.r.l.s.
 *
 * All Rights Reserved.
 */

package io.github.pervasivecats
package users.user

import eu.timepit.refined.api.Refined
import eu.timepit.refined.auto.given
import eu.timepit.refined.refineV
import eu.timepit.refined.string.MatchesRegex

import users.{Validated, ValidationError}

type UsernameString = String Refined MatchesRegex["^[_0-9A-Za-z][_ 0-9A-Za-z]*$"]

trait Username {

  val value: UsernameString
}

object Username {

  final private case class UsernameImpl(value: UsernameString) extends Username

  case object WrongUsernameFormat extends ValidationError {

    override val message: String = "The username format is invalid"
  }

  def apply(value: String): Validated[Username] = refineV[MatchesRegex["^[_0-9A-Za-z][_ 0-9A-Za-z]*$"]](value) match {
    case Left(_) => Left[ValidationError, Username](WrongUsernameFormat)
    case Right(value) => Right[ValidationError, Username](UsernameImpl(value))
  }

  given Conversion[Username, String] = _.value
}
