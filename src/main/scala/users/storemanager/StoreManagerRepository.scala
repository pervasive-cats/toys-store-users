/*
 * Copyright © 2022-2023 by Pervasive Cats S.r.l.s.
 *
 * All Rights Reserved.
 */

package io.github.pervasivecats
package users.storemanager

import users.storemanager.entities.StoreManager
import users.user.valueobjects.{EncryptedPassword, Username}
import users.Validated
import users.storemanager.valueobjects.Store
import users.user.Repository

trait StoreManagerRepository[A <: StoreManager] extends Repository[A] {

  def findByUsername(username: Username): Validated[A]

  def register(storeManager: A, password: EncryptedPassword): Validated[Unit]

  def updateStore(storeManager: A, store: Store): Validated[Unit]

  def unregister(storeManager: A): Validated[Unit]

}
