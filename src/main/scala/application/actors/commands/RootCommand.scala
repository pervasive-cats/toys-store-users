/*
 * Copyright © 2022-2023 by Pervasive Cats S.r.l.s.
 *
 * All Rights Reserved.
 */

package io.github.pervasivecats
package application.actors.commands

import scala.reflect.ClassTag

import io.github.pervasivecats.Validated
import io.github.pervasivecats.ValidationError

import akka.actor.typed.ActorRef

import users.customer.entities.Customer
import users.customer.valueobjects.{Email, NameComponent}
import users.user.valueobjects.{PlainPassword, Username}

sealed trait RootCommand

object RootCommand {

  final case class Startup(success: Boolean) extends RootCommand
}
