/*
 * Copyright © 2022-2023 by Pervasive Cats S.r.l.s.
 *
 * All Rights Reserved.
 */

package io.github.pervasivecats
package users.user.valueobjects

import eu.timepit.refined.auto.given
import org.scalatest.EitherValues.given
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers.*

import users.user.services.PasswordAlgorithm
import users.user.valueobjects.{EncryptedPassword, PlainPassword}
import users.user.valueobjects.EncryptedPassword.WrongEncryptedPasswordFormat

class EncryptedPasswordTest extends AnyFunSpec {

  describe("An encrypted password") {
    describe("when created with a correctly encrypted password") {
      it("should be valid") {
        val passwordString: String =
          summon[PasswordAlgorithm].encrypt(PlainPassword("Password!1").getOrElse(fail())).getOrElse(fail()).value

        (EncryptedPassword(passwordString).value.value: String) shouldBe passwordString
      }
    }

    describe("when created with an incorrectly encrypted password") {
      it("should not be valid") {
        EncryptedPassword("Password!1").left.value shouldBe WrongEncryptedPasswordFormat
      }
    }
  }
}
