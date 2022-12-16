/*
 * Copyright © 2022-2023 by Pervasive Cats S.r.l.s.
 *
 * All Rights Reserved.
 */

package io.github.pervasivecats
package users.user

import io.github.pervasivecats.users.user.valueobjects.EncryptedPassword

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers.*

class EncryptedPasswordTest extends AnyFunSpec {

  describe("An encrypted password") {
    describe("when created with a password") {
      it("should contain it") {
        val passwordString: String = "password"

        EncryptedPassword(passwordString).value shouldBe passwordString
      }
    }
  }
}
