/*
 * Copyright © 2022-2023 by Pervasive Cats S.r.l.s.
 *
 * All Rights Reserved.
 */

package io.github.pervasivecats
package application.actors.commands

import users.customer.domainevents.CustomerUnregistered as CustomerUnregisteredEvent

sealed trait MessageBrokerCommand

object MessageBrokerCommand {

  final case class CustomerUnregistered(event: CustomerUnregisteredEvent) extends MessageBrokerCommand
}
