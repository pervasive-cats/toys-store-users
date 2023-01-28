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
  private val storeId: Long = 1

  describe("A store manager") {
    describe("when created with a valid username and store") {
      it("should be created successfully") {
        val username: Username = Username(usernameString).value
        val store: Store = Store(storeId).value

        val manager: StoreManager = StoreManager(username, store)
        (manager.username.value: String) shouldBe usernameString
        (manager.store.id: Long) shouldBe storeId
      }
    }

    import users.storemanager.entities.StoreManagerOps.updateStore

    describe("when updated with a valid new store") {
      it("should correctly update") {
        val username: Username = Username(usernameString).value
        val store: Store = Store(storeId).value
        val newStoreId: Long = 2
        val newStore: Store = Store(newStoreId).value
        val manager: StoreManager = StoreManager(username, store)

        val updatedManager: StoreManager = manager.updateStore(newStore)
        (updatedManager.store.id: Long) shouldBe newStoreId
        (updatedManager.username.value: String) shouldBe usernameString
      }
    }
  }
}
