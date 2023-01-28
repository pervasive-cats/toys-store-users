/*
 * Copyright © 2022-2023 by Pervasive Cats S.r.l.s.
 *
 * All Rights Reserved.
 */

package io.github.pervasivecats
package application

import akka.actor.typed.ActorSystem
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory

import application.actors.*

@main
def main(): Unit = ActorSystem(RootActor(ConfigFactory.load()), name = "root_actor")
