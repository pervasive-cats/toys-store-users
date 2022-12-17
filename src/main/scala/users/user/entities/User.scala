/*
 * Copyright © 2022-2023 by Pervasive Cats S.r.l.s.
 *
 * All Rights Reserved.
 */

package io.github.pervasivecats
package users.user.entities

import users.{Validated, ValidationError}
import users.user.*
import users.user.services.PasswordAlgorithm
import users.user.valueobjects.{PlainPassword, Username}

trait User {

  val username: Username
}

object User {

  given [A <: User]: UserOps[A] with {

    override def verifyPassword(
      user: A,
      password: PlainPassword
    )(
      using
      PasswordAlgorithm,
      Repository[A]
    ): Validated[Boolean] = for {
      p <- summon[Repository[A]].findPassword(user)
      r = summon[PasswordAlgorithm].check(p, password)
    } yield r
  }
}
