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

trait EncryptedPassword {

  val value: String
}

object EncryptedPassword {

  final private case class EncryptedPasswordImpl(value: String) extends EncryptedPassword

  private[user] def apply(value: String): EncryptedPassword = EncryptedPasswordImpl(value)
}
