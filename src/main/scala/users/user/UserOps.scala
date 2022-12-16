/*
 * Copyright © 2022-2023 by Pervasive Cats S.r.l.s.
 *
 * All Rights Reserved.
 */

package io.github.pervasivecats
package users.user

import users.Validated

trait UserOps[A <: User] {

  def verifyPassword(user: A, password: PlainPassword): Validated[Boolean]
}

object UserOps {

  extension [A <: User: UserOps](user: A) {

    def verifyPassword(password: PlainPassword): Validated[Boolean] = implicitly[UserOps[A]].verifyPassword(user, password)
  }
}
