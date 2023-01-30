/*
 * Copyright © 2022-2023 by Pervasive Cats S.r.l.s.
 *
 * All Rights Reserved.
 */

package io.github.pervasivecats
package users.administration

import org.mockito.Mockito.*
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers.*

import users.administration.entities.Administration
import users.user.valueobjects.*

class AdministrationTest extends AnyFunSpec {

  private val username: Username = Username("Elena").getOrElse(fail())
  private val administration: Administration = Administration(username)

  describe("An administration account") {
    describe("when created with a username") {
      it("should contain it") {
        administration.username shouldBe username
      }
    }

    val secondAdministration: Administration = Administration(username)
    val thirdAdministration: Administration = Administration(username)

    describe("when compared with another identical customer") {
      it("should be equal following the symmetrical property") {
        administration shouldEqual secondAdministration
        secondAdministration shouldEqual administration
      }

      it("should be equal following the transitive property") {
        administration shouldEqual secondAdministration
        secondAdministration shouldEqual thirdAdministration
        administration shouldEqual thirdAdministration
      }

      it("should be equal following the reflexive property") {
        administration shouldEqual administration
      }

      it("should have the same hash code as the other") {
        administration.## shouldEqual secondAdministration.##
      }
    }

    describe("when compared with anything else") {
      it("should not be equal") {
        administration should not equal 1.0
      }
    }
  }
}
