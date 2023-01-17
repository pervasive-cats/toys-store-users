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

import users.customer.valeuobjects.Email
import users.customer.valeuobjects.Email.WrongEmailFormat

class EmailTest extends AnyFunSpec {

  describe("An email") {
    describe("when created with an initial dot") {
      it("should not be valid") {
        Email(".addr_3ss!@email.com").left.value shouldBe WrongEmailFormat
      }
    }

    describe("when created with an uppercase local part") {
      it("should not be valid") {
        Email("Addr_3ss!@email.com").left.value shouldBe WrongEmailFormat
      }
    }

    describe("when created without an at-sign") {
      it("should not be valid") {
        Email("addr_3ss!email.com").left.value shouldBe WrongEmailFormat
      }
    }

    describe("when created without a domain") {
      it("should not be valid") {
        Email("addr_3ss!@.com").left.value shouldBe WrongEmailFormat
      }
    }

    describe("when created without a top level domain") {
      it("should not be valid") {
        Email("addr_3ss!@email").left.value shouldBe WrongEmailFormat
      }
    }

    describe("when created with an illegal domain") {
      it("should not be valid") {
        Email("addr_3ss!@EM_ail.com").left.value shouldBe WrongEmailFormat
      }
    }

    describe("when created with an illegal top level domain") {
      it("should not be valid") {
        Email("addr_3ss!@email.C_O_M").left.value shouldBe WrongEmailFormat
      }
    }

    describe("when created following the correct format") {
      it("should be valid") {
        val emailString: String = "addr_3ss!@email.com"

        (Email(emailString).value.value: String) shouldBe emailString
      }
    }

    describe("when created using multiple dots, but following the correct format") {
      it("should be valid") {
        val emailString: String = "addr.3ss.!@email.test.com"

        (Email(emailString).value.value: String) shouldBe emailString
      }
    }
  }

}
