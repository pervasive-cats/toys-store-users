/*
 * Copyright © 2022-2023 by Pervasive Cats S.r.l.s.
 *
 * All Rights Reserved.
 */

package io.github.pervasivecats
package users.user

import users.{Validated, ValidationError}

trait Repository[A <: User] {

  def findPassword(user: A): Validated[EncryptedPassword]

  def updatePassword(user: A, password: PlainPassword): Validated[Unit]
}
