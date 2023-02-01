/*
 * Copyright © 2022-2023 by Pervasive Cats S.r.l.s.
 *
 * All Rights Reserved.
 */

package io.github.pervasivecats
package users.user

import io.github.pervasivecats.Validated
import io.github.pervasivecats.ValidationError

import entities.User
import valueobjects.EncryptedPassword
import valueobjects.PlainPassword

trait Repository[A <: User] {

  def findPassword(user: A): Validated[EncryptedPassword]

  def updatePassword(user: A, password: EncryptedPassword): Validated[Unit]
}
