/*
 * Copyright © 2022-2023 by Pervasive Cats S.r.l.s.
 *
 * All Rights Reserved.
 */

package io.github.pervasivecats
package users.user

import users.user.Username.WrongUsernameFormat

import eu.timepit.refined.api.RefType.refinedRefType.unwrap
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers.*
import org.scalatest.EitherValues.given
import eu.timepit.refined.auto.given

class UsernameTest extends AnyFunSpec {

  describe("A username") {
    describe("when created with an initial space") {
      it("should not be valid") {
        Username(" john").left.value shouldBe WrongUsernameFormat
      }
    }

    describe("when correct") {
      it("should be valid") {
        unwrap(Username("john").value.value) shouldBe "john"
      }
    }
  }
}
