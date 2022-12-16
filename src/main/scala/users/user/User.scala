/*
 * Copyright © 2022-2023 by Pervasive Cats S.r.l.s.
 *
 * All Rights Reserved.
 */

package io.github.pervasivecats
package users.user

import users.ValidationError

trait User {

  val username: Username
}

object User {

  final private case class UserImpl(username: Username) extends User

  given UserOps[User] = (user, password) =>
    Right[ValidationError, EncryptedPassword](
      EncryptedPassword("$2a$12$S47E5x.8.khg8lmKzfWk3e6Ik9HzR5xalCIDVMGJBn5A0QeZyRo.u")
    ).map(PasswordAlgorithm.check(_, password))

  def apply(username: Username): User = UserImpl(username)
}
