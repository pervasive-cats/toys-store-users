/*
 * Copyright © 2022-2023 by Pervasive Cats S.r.l.s.
 *
 * All Rights Reserved.
 */

package io.github.pervasivecats
package application.actors

import java.util.concurrent.ForkJoinPool

import scala.annotation.tailrec
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import akka.actor.typed.ActorRef
import akka.actor.typed.ActorSystem
import akka.actor.typed.Behavior
import akka.actor.typed.PostStop
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import com.typesafe.config.Config

import application.actors.RootCommand.Startup
import application.actors.MessageBrokerCommand
import application.actors.CustomerServerCommand
import application.routes.Routes

object RootActor {

  def apply(config: Config): Behavior[RootCommand] =
    Behaviors.setup { ctx =>
      val messageBrokerActor: ActorRef[MessageBrokerCommand] =
        ctx.spawn(MessageBrokerActor(ctx.self, config.getConfig("messageBroker")), name = "message_broker_actor")
      Behaviors.receiveMessage {
        case Startup(true) =>
          awaitServers(
            ctx
              .spawn(CustomerServerActor(ctx.self, config.getConfig("repository"), messageBrokerActor), name = "customer_server"),
            config.getConfig("server"),
            count = 0
          )
        case Startup(false) => Behaviors.stopped[RootCommand]
        case _ => Behaviors.unhandled[RootCommand]
      }
    }

  @SuppressWarnings(Array("org.wartremover.warts.Recursion"))
  private def awaitServers(
    customerServer: ActorRef[CustomerServerCommand],
    serverConfig: Config,
    count: Int
  ): Behavior[RootCommand] = Behaviors.receive { (ctx, msg) =>
    msg match {
      case Startup(true) if count < 0 => awaitServers(customerServer, serverConfig, count + 1)
      case Startup(true) =>
        given ActorSystem[_] = ctx.system
        val httpServer: Future[Http.ServerBinding] =
          Http()
            .newServerAt(serverConfig.getString("hostName"), serverConfig.getInt("portNumber"))
            .bind(Routes(customerServer))
        Behaviors.receiveSignal {
          case (_, PostStop) =>
            given ExecutionContext = ExecutionContext.fromExecutor(ForkJoinPool.commonPool())
            httpServer.flatMap(_.unbind()).onComplete(_ => println("Server has stopped"))
            Behaviors.same[RootCommand]
        }
      case Startup(false) => Behaviors.stopped[RootCommand]
      case _ => Behaviors.unhandled[RootCommand]
    }
  }
}