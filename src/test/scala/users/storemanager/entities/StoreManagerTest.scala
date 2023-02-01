/*
 * Copyright © 2022-2023 by Pervasive Cats S.r.l.s.
 *
 * All Rights Reserved.
 */

package io.github.pervasivecats
package users.storemanager.entities

import eu.timepit.refined.auto.given
import org.scalatest.EitherValues.given
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers.*

import users.storemanager.entities.StoreManager
import users.storemanager.valueobjects.Store
import users.user.valueobjects.Username

class StoreManagerTest extends AnyFunSpec {

  private val username: Username = Username("user1").getOrElse(fail())
  private val store: Store = Store(1).getOrElse(fail())
  private val manager: StoreManager = StoreManager(username, store)

  describe("A store manager") {
    describe("when created with a valid username and store") {
      it("should be created successfully") {
        manager.username shouldBe username
        manager.store shouldBe store
      }
    }

    import users.storemanager.entities.StoreManagerOps.updateStore

    describe("when updated with a valid new store") {
      it("should correctly update") {
        val newStoreId: Long = 2L
        val updatedManager: StoreManager = manager.updateStore(Store(newStoreId).getOrElse(fail()))
        (updatedManager.store.id: Long) shouldBe newStoreId
        updatedManager.username shouldBe username
      }
    }

    val secondStoreManager: StoreManager = StoreManager(username, store)
    val thirdStoreManager: StoreManager = StoreManager(username, store)

    describe("when compared with another identical customer") {
      it("should be equal following the symmetrical property") {
        manager shouldEqual secondStoreManager
        secondStoreManager shouldEqual manager
      }

      it("should be equal following the transitive property") {
        manager shouldEqual secondStoreManager
        secondStoreManager shouldEqual thirdStoreManager
        manager shouldEqual thirdStoreManager
      }

      it("should be equal following the reflexive property") {
        manager shouldEqual manager
      }

      it("should have the same hash code as the other") {
        manager.## shouldEqual secondStoreManager.##
      }
    }

    describe("when compared with anything else") {
      it("should not be equal") {
        manager should not equal 1.0
      }
    }
  }
}
