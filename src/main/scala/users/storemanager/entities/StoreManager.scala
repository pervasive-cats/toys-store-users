/*
 * Copyright © 2022-2023 by Pervasive Cats S.r.l.s.
 *
 * All Rights Reserved.
 */

package io.github.pervasivecats
package users.storemanager.entities

import users.storemanager.valueobjects.Store
import users.user.entities.User
import users.user.valueobjects.Username
import AnyOps.===

trait StoreManager extends User {

  val store: Store
}

object StoreManager {

  final private case class StoreManagerImpl(username: Username, store: Store) extends StoreManager {

    override def equals(obj: Any): Boolean = obj match {
      case s: StoreManager => username === s.username
      case _ => false
    }

    override def hashCode(): Int = username.##
  }

  given StoreManagerOps[StoreManager] with {

    override def updateStore(storeManager: StoreManager, newStore: Store): StoreManager =
      StoreManager(storeManager.username, newStore)
  }

  def apply(username: Username, store: Store): StoreManager = StoreManagerImpl(username, store)
}
