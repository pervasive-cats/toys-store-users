/*
 * Copyright © 2022-2023 by Pervasive Cats S.r.l.s.
 *
 * All Rights Reserved.
 */

package io.github.pervasivecats
package users.user.entities

import io.github.pervasivecats.ValidationError

import org.mockito.Mockito.*
import org.scalatest.EitherValues.given
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers.*

import users.user.services.PasswordAlgorithm.PasswordNotMatching
import users.user.entities.User
import users.user.services.PasswordAlgorithm
import users.user.valueobjects.{EncryptedPassword, PlainPassword}
import users.user.Repository

class UserTest extends AnyFunSpec {

  private val mockUser: User = mock(classOf[User])
  private val password: PlainPassword = PlainPassword("Password1!").getOrElse(fail())

  private given mockRepository: Repository[User] = mock(classOf[Repository[User]])

  when(mockRepository.findPassword(mockUser)).thenReturn(
    Right[ValidationError, EncryptedPassword](summon[PasswordAlgorithm].encrypt(password).getOrElse(fail()))
  )

  import users.user.entities.UserOps.verifyPassword

  describe("A user") {
    describe("when given the correct password to verify") {
      it("should return true") {
        mockUser.verifyPassword(password).value shouldBe ()
      }
    }

    describe("when given the wrong password to verify") {
      it("should return false") {
        mockUser.verifyPassword(PlainPassword("Password2!").getOrElse(fail())).left.value shouldBe PasswordNotMatching
      }
    }
  }
}
