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

import users.customer.entities.Customer
import users.customer.valeuobjects.{Email, NameComponent}
import users.user.valueobjects.Username
import users.customer.entities.CustomerOps.updated

class CustomerTest extends AnyFunSpec {

  private val username: Username = Username("mar10").getOrElse(fail())
  private val email: Email = Email("mario@email.com").getOrElse(fail())
  private val firstName: NameComponent = NameComponent("Mario").getOrElse(fail())
  private val lastName: NameComponent = NameComponent("Rossi").getOrElse(fail())
  private val customer: Customer = Customer(firstName, lastName, email, username)

  describe("A customer") {
    describe("when created with a username, an email, a first and a last name") {
      it("should contain them") {
        customer.email shouldBe email
        customer.username shouldBe username
        customer.firstName shouldBe firstName
        customer.lastName shouldBe lastName
      }
    }

    describe("when updated with a new email") {
      it("should contain it") {
        val newEmail: Email = Email("luigi@mail.com").getOrElse(fail())

        customer.updated(email = newEmail).email shouldBe newEmail
      }
    }

    describe("when updated with a new username") {
      it("should contain it") {
        val newUsername: Username = Username("l0033gi").getOrElse(fail())

        customer.updated(username = newUsername).username shouldBe newUsername
      }
    }

    describe("when updated with a new first name") {
      it("should contain it") {
        val newFirstName: NameComponent = NameComponent("Luigi").getOrElse(fail())

        customer.updated(firstName = newFirstName).firstName shouldBe newFirstName
      }
    }

    describe("when updated with a new last name") {
      it("should contain it") {
        val newLastName: NameComponent = NameComponent("Bianchi").getOrElse(fail())

        customer.updated(lastName = newLastName).lastName shouldBe newLastName
      }
    }
  }
}
