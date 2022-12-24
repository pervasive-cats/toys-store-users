/*
 * Copyright © 2022-2023 by Pervasive Cats S.r.l.s.
 *
 * All Rights Reserved.
 */

package io.github.pervasivecats
package users.storemanager.entities

import users.user.entities.User
import users.storemanager.valueobjects.Store
import users.user.valueobjects.Username

trait StoreManager extends User {

  val store: Store
}

object StoreManager {

  final private case class StoreManagerImpl(username: Username, store: Store) extends StoreManager

  given StoreManagerOps with {

    override def updateStore(storeManager: StoreManager, newStore: Store): StoreManager =
      StoreManager(storeManager.username, newStore)
  }

  def apply(username: Username, store: Store): StoreManager = StoreManagerImpl(username, store)
}
