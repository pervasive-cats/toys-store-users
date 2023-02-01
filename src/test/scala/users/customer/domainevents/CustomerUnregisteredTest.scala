/*
 * Copyright © 2022-2023 by Pervasive Cats S.r.l.s.
 *
 * All Rights Reserved.
 */

package io.github.pervasivecats
package users.customer.domainevents

import eu.timepit.refined.auto.given
import org.scalatest.EitherValues.given
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers.*

import users.customer.domainevents.CustomerUnregistered
import users.customer.valueobjects.Email

class CustomerUnregisteredTest extends AnyFunSpec {

  describe("A customer unregistered event") {
    describe("when created with a customer email") {
      it("should contain it") {
        val email: Email = Email("example@mail.com").getOrElse(fail())

        CustomerUnregistered(email).email shouldBe email
      }
    }
  }
}
