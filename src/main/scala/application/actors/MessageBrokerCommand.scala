/*
 * Copyright © 2022-2023 by Pervasive Cats S.r.l.s.
 *
 * All Rights Reserved.
 */

package io.github.pervasivecats
package application.actors

import users.customer.valueobjects.Email

sealed trait MessageBrokerCommand

object MessageBrokerCommand {

  final case class CustomerUnregistered(email: Email) extends MessageBrokerCommand
}
