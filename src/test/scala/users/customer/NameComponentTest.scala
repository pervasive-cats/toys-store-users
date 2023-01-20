/*
 * Copyright © 2022-2023 by Pervasive Cats S.r.l.s.
 *
 * All Rights Reserved.
 */

package io.github.pervasivecats
package users.customer

import eu.timepit.refined.auto.given
import org.scalatest.EitherValues.given
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers.*

import users.customer.valueobjects.NameComponent
import users.customer.valueobjects.NameComponent.WrongNameComponentFormat

class NameComponentTest extends AnyFunSpec {

  describe("A name component") {
    describe("when it is empty") {
      it("should not be valid") {
        NameComponent("").left.value shouldBe WrongNameComponentFormat
      }
    }

    describe("when contains numbers") {
      it("should be invalid") {
        NameComponent("Mar10").left.value shouldBe WrongNameComponentFormat
      }
    }

    describe("when contains more than 100 characters") {
      it("should be invalid") {
        NameComponent("a".repeat(101)).left.value shouldBe WrongNameComponentFormat
      }
    }

    describe("when it does not start with an uppercase letter") {
      it("should be invalid") {
        NameComponent("mario").left.value shouldBe WrongNameComponentFormat
      }
    }

    describe("when it is correctly formatted") {
      it("should be valid") {
        val nameComponentString: String = "Mario"

        (NameComponent(nameComponentString).value.value: String) shouldBe nameComponentString
      }
    }
  }
}
