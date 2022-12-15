/*
 * Copyright © 2022-2023 by Pervasive Cats S.r.l.s.
 *
 * All Rights Reserved.
 */

package io.github.pervasivecats
package users.user

import java.util.Locale

import eu.timepit.refined.auto.given
import org.scalatest.EitherValues.given
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers.*

import users.user.Username.WrongUsernameFormat

class UsernameTest extends AnyFunSpec {

  private val maxLength: Int = 100

  describe("A username") {
    describe("when created with a string containing spaces") {
      it("should not be valid") {
        Username(" jo hn ").left.value shouldBe WrongUsernameFormat
      }
    }

    describe("when created with a string more than 100 characters long") {
      it("should not be valid") {
        Username("a".repeat(maxLength + 1)).left.value shouldBe WrongUsernameFormat
      }
    }

    describe("when created with an empty string") {
      it("should not be valid") {
        Username("").left.value shouldBe WrongUsernameFormat
      }
    }

    describe("when created with a string containing printable non-word characters or tabs or newlines") {
      it("should not be valid") {
        "!\"#$%&'()*+,-./:;<=>?@[\\]^`{|}~\t\n".split("").toSeq.map(Username(_)).foreach(
          _.left.value shouldBe WrongUsernameFormat
        )
      }
    }

    describe("when created with only word characters") {
      it("should be valid") {
        val pangramString: String = "thequickbrownfoxjumpsoverthelazydog"
        val usernameString: String = pangramString + pangramString.toUpperCase(Locale.ITALY) + "0123456789_"

        (Username(usernameString).value.value: String) shouldBe usernameString
      }
    }

    describe("when created with a string exactly 100 characters long") {
      it("should be valid") {
        val usernameString: String = "a".repeat(maxLength)

        (Username(usernameString).value.value: String) shouldBe usernameString
      }
    }
  }
}
