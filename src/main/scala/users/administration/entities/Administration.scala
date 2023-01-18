/*
 * Copyright © 2022-2023 by Pervasive Cats S.r.l.s.
 *
 * All Rights Reserved.
 */

package io.github.pervasivecats
package users.administration.entities

import users.administration.entities.Administration
import users.user.entities.User
import users.user.valueobjects.Username.WrongUsernameFormat
import users.user.valueobjects.{PlainPassword, Username}
import users.{Validated, ValidationError}

import eu.timepit.refined.api.RefType.applyRef

trait Administration extends User

object Administration {

  final private case class AdministrationImpl(username: Username) extends Administration

  def apply(username: Username): Administration = AdministrationImpl(username)

}
