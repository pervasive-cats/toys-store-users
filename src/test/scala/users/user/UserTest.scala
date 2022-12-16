/*
 * Copyright © 2022-2023 by Pervasive Cats S.r.l.s.
 *
 * All Rights Reserved.
 */

package io.github.pervasivecats
package users.user

import org.mockito.Mockito.*
import org.scalatest.EitherValues.given
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers.*

import users.{user, ValidationError}

class UserTest extends AnyFunSpec {

  private val mockUser: User = mock(classOf[User])

  private given mockRepository: Repository[User] = mock(classOf[Repository[User]])

  when(mockRepository.findPassword(mockUser)).thenReturn(
    Right[ValidationError, EncryptedPassword](EncryptedPassword("$2a$12$S47E5x.8.khg8lmKzfWk3e6Ik9HzR5xalCIDVMGJBn5A0QeZyRo.u"))
  )

  import UserOps.verifyPassword

  describe("A user") {
    describe("when given the correct password to verify") {
      it("should return true") {
        mockUser.verifyPassword(PlainPassword("Password1!").getOrElse(fail())).value shouldBe true
      }
    }

    describe("when given the wrong password to verify") {
      it("should return false") {
        mockUser.verifyPassword(PlainPassword("Password2!").getOrElse(fail())).value shouldBe false
      }
    }
  }
}
