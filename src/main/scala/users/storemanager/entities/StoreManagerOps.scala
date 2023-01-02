/*
 * Copyright © 2022-2023 by Pervasive Cats S.r.l.s.
 *
 * All Rights Reserved.
 */

package io.github.pervasivecats
package users.storemanager.entities

import users.storemanager.valueobjects.Store
import users.user.entities.UserOps

trait StoreManagerOps[A <: StoreManager: UserOps] {

  def updateStore(storeManager: A, newStore: Store): A
}

object StoreManagerOps {

  extension [A <: StoreManager: StoreManagerOps](storeManager: A) {

    def updateStore(newStore: Store): A = implicitly[StoreManagerOps[A]].updateStore(storeManager, newStore)
  }

}
