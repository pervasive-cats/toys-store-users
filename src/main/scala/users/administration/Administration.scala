/*
 * Copyright © 2022-2023 by Pervasive Cats S.r.l.s.
 *
 * All Rights Reserved.
 */

package io.github.pervasivecats
package users.administration

import eu.timepit.refined.api.RefType.applyRef

import users.{Validated, ValidationError}
import users.user.entities.User
import users.user.valueobjects.{PlainPassword, Username}
import users.user.valueobjects.Username.WrongUsernameFormat

trait Administration extends User

object Administration {

  final private case class AdministrationImpl(username: Username) extends Administration

  def apply(username: Username): Administration = AdministrationImpl(username)

}
