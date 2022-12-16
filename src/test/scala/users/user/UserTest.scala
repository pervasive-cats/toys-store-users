/*
 * Copyright © 2022-2023 by Pervasive Cats S.r.l.s.
 *
 * All Rights Reserved.
 */

package io.github.pervasivecats
package users.user

import org.scalatest.EitherValues.given
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers.*

class UserTest extends AnyFunSpec {

  describe("A user") {
    describe("when given a username") {
      it("should have that username") {
        Username("john") match {
          case Right(u) => User(u).username shouldBe u
          case _ => fail()
        }
      }
    }

    import UserOps.verifyPassword

    describe("when the password verification operation is done") {
      it("should have performed correctly") {
        (for {
          u <- Username("john")
          p <- PlainPassword("Password1!")
          r <- User(u).verifyPassword(p)
        } yield r).value shouldBe true
      }
    }
  }
}
