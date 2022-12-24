/*
 * Copyright © 2022-2023 by Pervasive Cats S.r.l.s.
 *
 * All Rights Reserved.
 */

package io.github.pervasivecats
package users.storemanager

import eu.timepit.refined.auto.given
import org.scalatest.EitherValues.given
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers.*

import users.storemanager.entities.StoreManager
import users.storemanager.valueobjects.Store
import users.user.valueobjects.Username

class StoreManagerTest extends AnyFunSpec {

  private val usernameString: String = "user1"
  private val storeID: Long = 1

  describe("A store manager") {
    describe("when created with a valid username and store") {
      it("should be created successfully") {
        val username: Username = Username(usernameString).value
        val store: Store = Store(storeID).value

        val manager: StoreManager = StoreManager(username, store)
        (manager.username.value: String) shouldBe usernameString
        (manager.store.value: Long) shouldBe storeID
      }
    }

    import users.storemanager.entities.StoreManagerOps.updateStore

    describe("when updated with a valid new store") {
      it("should correctly update") {
        val username: Username = Username(usernameString).value
        val store: Store = Store(storeID).value
        val newStoreID: Long = 2
        val newStore: Store = Store(newStoreID).value
        val manager: StoreManager = StoreManager(username, store)

        val updatedManager: StoreManager = manager.updateStore(newStore)
        (updatedManager.store.value: Long) shouldBe newStoreID
        (updatedManager.username.value: String) shouldBe usernameString
      }
    }
  }
}
