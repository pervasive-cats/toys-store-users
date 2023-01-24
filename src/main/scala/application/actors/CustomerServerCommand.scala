/*
 * Copyright © 2022-2023 by Pervasive Cats S.r.l.s.
 *
 * All Rights Reserved.
 */

package io.github.pervasivecats
package application.actors

import akka.actor.typed.ActorRef

import application.routes.Response.{CustomerResponse, EmptyResponse}
import users.customer.entities.Customer
import users.customer.valueobjects.{Email, NameComponent}
import users.user.valueobjects.{PlainPassword, Username}

sealed trait CustomerServerCommand

object CustomerServerCommand {

  final case class RegisterCustomer(customer: Customer, password: PlainPassword, replyTo: ActorRef[CustomerResponse])
    extends CustomerServerCommand

  final case class DeregisterCustomer(email: Email, password: PlainPassword, replyTo: ActorRef[EmptyResponse])
    extends CustomerServerCommand

  final case class LoginCustomer(email: Email, password: PlainPassword, replyTo: ActorRef[CustomerResponse])
    extends CustomerServerCommand

  final case class UpdateCustomerData(
    email: Email,
    newEmail: Email,
    newUsername: Username,
    newFirstName: NameComponent,
    newLastName: NameComponent,
    replyTo: ActorRef[CustomerResponse]
  ) extends CustomerServerCommand

  final case class UpdateCustomerPassword(
    email: Email,
    password: PlainPassword,
    newPassword: PlainPassword,
    replyTo: ActorRef[EmptyResponse]
  ) extends CustomerServerCommand
}
