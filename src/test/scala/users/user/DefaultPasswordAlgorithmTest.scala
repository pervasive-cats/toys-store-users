/*
 * Copyright © 2022-2023 by Pervasive Cats S.r.l.s.
 *
 * All Rights Reserved.
 */

package io.github.pervasivecats
package users.user

import io.github.pervasivecats.users.user.services.PasswordAlgorithm
import io.github.pervasivecats.users.user.valueobjects.PlainPassword

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers.*

class DefaultPasswordAlgorithmTest extends AnyFunSpec {

  private val defaultPasswordAlgorithm: PasswordAlgorithm = summon[PasswordAlgorithm]
  private val password: PlainPassword = PlainPassword("Password1!").getOrElse(fail())

  describe("A password algorithm") {
    describe("when used to encode a password") {
      it("should provide a password encoded in the correct format") {
        defaultPasswordAlgorithm.encrypt(password).value should fullyMatch regex "\\$2a\\$12\\$[a-zA-Z0-9\\./]{53}"
      }
    }

    describe("when used for checking a password against the correct one") {
      it("should return true") {
        defaultPasswordAlgorithm.check(defaultPasswordAlgorithm.encrypt(password), password) shouldBe true
      }
    }

    describe("when used for checking a password against the wrong one") {
      it("should return false") {
        val wrongPassword: PlainPassword = PlainPassword("Password2!").getOrElse(fail())

        defaultPasswordAlgorithm.check(defaultPasswordAlgorithm.encrypt(password), wrongPassword) shouldBe false
      }
    }
  }
}
