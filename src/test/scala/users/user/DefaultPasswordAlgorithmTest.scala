/*
 * Copyright © 2022-2023 by Pervasive Cats S.r.l.s.
 *
 * All Rights Reserved.
 */

package io.github.pervasivecats
package users.user

import org.scalatest.funspec.AnyFunSpec
import eu.timepit.refined.auto.given
import org.scalatest.EitherValues.given
import org.scalatest.matchers.should.Matchers.*

import users.user.services.PasswordAlgorithm
import users.user.valueobjects.PlainPassword

class DefaultPasswordAlgorithmTest extends AnyFunSpec {

  private val defaultPasswordAlgorithm: PasswordAlgorithm = summon[PasswordAlgorithm]
  private val password: PlainPassword = PlainPassword("Password1!").getOrElse(fail())

  describe("A password algorithm") {
    describe("when used to encode a password") {
      it("should provide a password encoded in the correct format") {
        defaultPasswordAlgorithm.encrypt(password).value.value should fullyMatch regex "\\$2a\\$12\\$[a-zA-Z0-9\\./]{53}"
      }
    }

    describe("when used for checking a password against the correct one") {
      it("should return true") {
        defaultPasswordAlgorithm.check(defaultPasswordAlgorithm.encrypt(password).value, password) shouldBe true
      }
    }

    describe("when used for checking a password against the wrong one") {
      it("should return false") {
        val wrongPassword: PlainPassword = PlainPassword("Password2!").getOrElse(fail())

        defaultPasswordAlgorithm.check(defaultPasswordAlgorithm.encrypt(password).value, wrongPassword) shouldBe false
      }
    }
  }
}
