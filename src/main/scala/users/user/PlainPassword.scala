/*
 * Copyright © 2022-2023 by Pervasive Cats S.r.l.s.
 *
 * All Rights Reserved.
 */

package io.github.pervasivecats
package users.user

import eu.timepit.refined.api.RefType.applyRef
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.MatchesRegex

import users.{Validated, ValidationError}

type PlainPasswordString = String Refined MatchesRegex["^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[\\p{Punct}])(?=.{8,}).*$"]

trait PlainPassword {

  val value: PlainPasswordString
}

object PlainPassword {

  final private case class PlainPasswordImpl(value: PlainPasswordString) extends PlainPassword

  case object WrongPasswordFormat extends ValidationError {

    override val message: String = "The password format is invalid"
  }

  def apply(value: String): Validated[PlainPassword] = applyRef[PlainPasswordString](value) match {
    case Left(_) => Left[ValidationError, PlainPassword](WrongPasswordFormat)
    case Right(value) => Right[ValidationError, PlainPassword](PlainPasswordImpl(value))
  }
}
