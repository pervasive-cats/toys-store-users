/*
 * Copyright © 2022-2023 by Pervasive Cats S.r.l.s.
 *
 * All Rights Reserved.
 */

package io.github.pervasivecats
package users.storemanager.entities

import users.storemanager.valueobjects.Store
import StoreManager.given

trait StoreManagerOps {

  def updateStore(storeManager: StoreManager, newStore: Store): StoreManager
}

object StoreManagerOps {

  extension (storeManager: StoreManager) {

    def updateStore(newStore: Store): StoreManager = implicitly[StoreManagerOps].updateStore(storeManager, newStore)
  }

}
