/*
 * Copyright © 2022-2023 by Pervasive Cats S.r.l.s.
 *
 * All Rights Reserved.
 */

package io.github.pervasivecats
package users.user.valueobjects

import eu.timepit.refined.api.RefType.applyRef
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.MatchesRegex

import users.{Validated, ValidationError}

type EncryptedPasswordString = String Refined MatchesRegex["^\\$2a\\$12\\$[a-zA-Z0-9\\./]{53}$"]

trait EncryptedPassword {

  val value: EncryptedPasswordString

}

object EncryptedPassword {

  final private case class EncryptedPasswordImpl(value: EncryptedPasswordString) extends EncryptedPassword

  case object WrongEncryptedPasswordFormat extends ValidationError {

    override val message: String = "The encrypted password format is invalid"
  }

  def apply(value: String): Validated[EncryptedPassword] = applyRef[EncryptedPasswordString](value) match {
    case Left(_) => Left[ValidationError, EncryptedPassword](WrongEncryptedPasswordFormat)
    case Right(value) => Right[ValidationError, EncryptedPassword](EncryptedPasswordImpl(value))
  }

}
