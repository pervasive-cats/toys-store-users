/*
 * Copyright © 2022-2023 by Pervasive Cats S.r.l.s.
 *
 * All Rights Reserved.
 */

package io.github.pervasivecats
package users.user

import eu.timepit.refined.auto.given
import org.scalatest.EitherValues.given
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers.*

import users.user.PlainPassword.WrongPasswordFormat

class PlainPasswordTest extends AnyFunSpec {

  describe("A plain password") {
    describe("when created without any lowercase letter") {
      it("should not be valid") {
        PlainPassword("PASSWORD1!").left.value shouldBe WrongPasswordFormat
      }
    }

    describe("when created without any uppercase letter") {
      it("should not be valid") {
        PlainPassword("password1!").left.value shouldBe WrongPasswordFormat
      }
    }

    describe("when created without any number") {
      it("should not be valid") {
        PlainPassword("Password!").left.value shouldBe WrongPasswordFormat
      }
    }

    describe("when created without any punctuation character") {
      it("should not be valid") {
        PlainPassword("Password1").left.value shouldBe WrongPasswordFormat
      }
    }

    describe("when created with less than 8 characters") {
      it("should not be valid") {
        PlainPassword("pass12!").left.value shouldBe WrongPasswordFormat
      }
    }

    describe("when created with one uppercase and lowercase letter, one number and one punctuation char with 8 chars total") {
      it("should be valid") {
        val passwordString: String = "Password1!"

        (PlainPassword(passwordString).value.value: String) shouldBe passwordString
      }
    }
  }
}
