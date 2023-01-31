/*
 * Copyright © 2022-2023 by Pervasive Cats S.r.l.s.
 *
 * All Rights Reserved.
 */

package io.github.pervasivecats
package users.user.entities

import io.github.pervasivecats.Validated

import users.user.services.PasswordAlgorithm
import users.user.*
import users.user.valueobjects.PlainPassword

trait UserOps[A <: User] {

  def verifyPassword(user: A, password: PlainPassword)(using PasswordAlgorithm, Repository[A]): Validated[Unit]
}

object UserOps {

  extension [A <: User: UserOps](user: A) {

    def verifyPassword(password: PlainPassword)(using PasswordAlgorithm, Repository[A]): Validated[Unit] =
      implicitly[UserOps[A]].verifyPassword(user, password)
  }
}
