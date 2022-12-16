/*
 * Copyright © 2022-2023 by Pervasive Cats S.r.l.s.
 *
 * All Rights Reserved.
 */

package io.github.pervasivecats
package users.user

import at.favre.lib.crypto.bcrypt.BCrypt
import at.favre.lib.crypto.bcrypt.LongPasswordStrategies
import at.favre.lib.crypto.bcrypt.LongPasswordStrategy
import eu.timepit.refined.auto.given

import users.Validated

trait PasswordAlgorithm {

  def check(expectedPassword: EncryptedPassword, actualPassword: PlainPassword): Boolean

  def encrypt(plainPassword: PlainPassword): EncryptedPassword
}

object PasswordAlgorithm {

  given PasswordAlgorithm with {

    private val cost: Int = 12
    private val version: BCrypt.Version = BCrypt.Version.VERSION_2A
    private val longPasswordStrategy: LongPasswordStrategy = LongPasswordStrategies.hashSha512(version)
    private val hashAlgorithm: BCrypt.Hasher = BCrypt.`with`(longPasswordStrategy)
    private val verifyAlgorithm: BCrypt.Verifyer = BCrypt.verifyer(version, longPasswordStrategy)

    def check(expectedPassword: EncryptedPassword, actualPassword: PlainPassword): Boolean =
      verifyAlgorithm.verify(actualPassword.value.toCharArray, expectedPassword.value).verified

    def encrypt(plainPassword: PlainPassword): EncryptedPassword =
      EncryptedPassword(hashAlgorithm.hashToString(cost, plainPassword.value.toCharArray))
  }
}
