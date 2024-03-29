/*
 * Copyright © 2022-2023 by Pervasive Cats S.r.l.s.
 *
 * All Rights Reserved.
 */

package io.github.pervasivecats
package users.administration.entities

import io.github.pervasivecats.Validated
import io.github.pervasivecats.ValidationError

import eu.timepit.refined.api.RefType.applyRef

import users.administration.entities.Administration
import users.user.entities.User
import users.user.valueobjects.Username.WrongUsernameFormat
import users.user.valueobjects.{PlainPassword, Username}
import AnyOps.===

trait Administration extends User

object Administration {

  final private case class AdministrationImpl(username: Username) extends Administration {

    override def equals(obj: Any): Boolean = obj match {
      case a: Administration => username === a.username
      case _ => false
    }

    override def hashCode(): Int = username.##
  }

  def apply(username: Username): Administration = AdministrationImpl(username)
}
