/*
 * Copyright © 2022-2023 by Pervasive Cats S.r.l.s.
 *
 * All Rights Reserved.
 */

package io.github.pervasivecats
package users.user

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers.*

class PasswordAlgorithmTest extends AnyFunSpec {

  describe("A password algorithm") {
    describe("when used to encode a password") {
      it("should provide the correct encoded password") {
        PlainPassword("Password1!") match {
          case Right(v) => PasswordAlgorithm.encrypt(v).value should fullyMatch regex "\\$2a\\$12\\$[a-zA-Z0-9\\./]{53}"
          case _ => fail()
        }
      }
    }

    describe("when used for checking a password against the correct one") {
      it("should return true") {
        PlainPassword("Password1!") match {
          case Right(v) => PasswordAlgorithm.check(PasswordAlgorithm.encrypt(v), v) shouldBe true
          case _ => fail()
        }
      }
    }

    describe("when used for checking a password against the wrong one") {
      it("should return false") {
        (PlainPassword("Password1!"), PlainPassword("Password2!")) match {
          case (Right(v1), Right(v2)) => PasswordAlgorithm.check(PasswordAlgorithm.encrypt(v1), v2) shouldBe false
          case _ => fail()
        }
      }
    }
  }
}
