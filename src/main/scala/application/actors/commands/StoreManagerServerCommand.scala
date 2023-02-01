/*
 * Copyright © 2022-2023 by Pervasive Cats S.r.l.s.
 *
 * All Rights Reserved.
 */

package io.github.pervasivecats
package application.actors.commands

import akka.actor.typed.ActorRef

import application.routes.entities.Response.{EmptyResponse, StoreManagerResponse}
import users.storemanager.entities.StoreManager
import users.storemanager.valueobjects.Store
import users.user.valueobjects.{PlainPassword, Username}

sealed trait StoreManagerServerCommand

object StoreManagerServerCommand {

  final case class RegisterStoreManager(
    storeManager: StoreManager,
    password: PlainPassword,
    replyTo: ActorRef[StoreManagerResponse]
  ) extends StoreManagerServerCommand

  final case class DeregisterStoreManager(username: Username, password: PlainPassword, replyTo: ActorRef[EmptyResponse])
    extends StoreManagerServerCommand

  final case class LoginStoreManager(username: Username, password: PlainPassword, replyTo: ActorRef[StoreManagerResponse])
    extends StoreManagerServerCommand

  final case class UpdateStoreManagerStore(
    username: Username,
    newStore: Store,
    replyTo: ActorRef[StoreManagerResponse]
  ) extends StoreManagerServerCommand

  final case class UpdateStoreManagerPassword(
    username: Username,
    password: PlainPassword,
    newPassword: PlainPassword,
    replyTo: ActorRef[EmptyResponse]
  ) extends StoreManagerServerCommand
}
