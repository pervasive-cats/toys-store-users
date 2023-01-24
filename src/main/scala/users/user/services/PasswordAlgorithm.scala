/*
 * Copyright © 2022-2023 by Pervasive Cats S.r.l.s.
 *
 * All Rights Reserved.
 */

package io.github.pervasivecats
package users.user.services

import at.favre.lib.crypto.bcrypt.*
import eu.timepit.refined.auto.given

import users.{Validated, ValidationError}
import users.user.valueobjects.{EncryptedPassword, PlainPassword}

trait PasswordAlgorithm {

  def check(expectedPassword: EncryptedPassword, actualPassword: PlainPassword): Validated[Unit]

  def encrypt(plainPassword: PlainPassword): Validated[EncryptedPassword]
}

object PasswordAlgorithm {

  case object PasswordNotMatching extends ValidationError {

    override val message: String = "The expected password and the actual password did not match"
  }

  given PasswordAlgorithm with {

    private val cost: Int = 12
    private val version: BCrypt.Version = BCrypt.Version.VERSION_2A
    private val longPasswordStrategy: LongPasswordStrategy = LongPasswordStrategies.hashSha512(version)
    private val hashAlgorithm: BCrypt.Hasher = BCrypt.`with`(longPasswordStrategy)
    private val verifyAlgorithm: BCrypt.Verifyer = BCrypt.verifyer(version, longPasswordStrategy)

    def check(expectedPassword: EncryptedPassword, actualPassword: PlainPassword): Validated[Unit] =
      if (verifyAlgorithm.verify(actualPassword.value.toCharArray, expectedPassword.value).verified)
        Right[ValidationError, Unit](())
      else
        Left[ValidationError, Unit](PasswordNotMatching)

    def encrypt(plainPassword: PlainPassword): Validated[EncryptedPassword] =
      EncryptedPassword(hashAlgorithm.hashToString(cost, plainPassword.value.toCharArray))
  }
}
