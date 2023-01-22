/*
 * Copyright © 2022-2023 by Pervasive Cats S.r.l.s.
 *
 * All Rights Reserved.
 */

package io.github.pervasivecats
package users.administration

import io.github.pervasivecats.users.administration.entities.Administration
import org.mockito.Mockito.description
import org.mockito.Mockito.mock
import org.mockito.Mockito.when
import org.scalatest.funspec.AnyFunSpec
import users.user.valueobjects.{EncryptedPassword, PlainPassword, Username}

import org.scalatest.matchers.should.Matchers.shouldBe

import scala.language.postfixOps

class AdministrationTest extends AnyFunSpec {

  val username: Username = Username("Elena").getOrElse(fail())

  describe("An administration") {
    describe("when created with a username"){
      it("should contain them") {
        val administration: Administration = Administration(username)
        administration.username shouldBe username
      }
    }
  }
}
