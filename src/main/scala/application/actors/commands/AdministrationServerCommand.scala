/*
 * Copyright © 2022-2023 by Pervasive Cats S.r.l.s.
 *
 * All Rights Reserved.
 */

package io.github.pervasivecats
package application.actors.commands

import akka.actor.typed.ActorRef

import application.routes.entities.Response.{AdministrationResponse, EmptyResponse}
import users.user.valueobjects.{PlainPassword, Username}

sealed trait AdministrationServerCommand

object AdministrationServerCommand {

  final case class LoginAdministration(username: Username, password: PlainPassword, replyTo: ActorRef[AdministrationResponse])
    extends AdministrationServerCommand

  final case class UpdateAdministrationPassword(
    username: Username,
    password: PlainPassword,
    newPassword: PlainPassword,
    replyTo: ActorRef[EmptyResponse]
  ) extends AdministrationServerCommand
}
