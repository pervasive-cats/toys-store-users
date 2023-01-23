/*
 * Copyright © 2022-2023 by Pervasive Cats S.r.l.s.
 *
 * All Rights Reserved.
 */

package io.github.pervasivecats
package users.administration

import scala.language.postfixOps

import io.github.pervasivecats.users.administration.entities.Administration

import org.mockito.Mockito.description
import org.mockito.Mockito.mock
import org.mockito.Mockito.when
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers.shouldBe

import users.user.valueobjects.{EncryptedPassword, PlainPassword, Username}

class AdministrationTest extends AnyFunSpec {

  val username: Username = Username("Elena").getOrElse(fail())

  describe("An administration account") {
    describe("when created with a username") {
      it("should contain it") {
        Administration(username).username shouldBe username
      }
    }
  }
}
