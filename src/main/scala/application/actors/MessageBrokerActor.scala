/*
 * Copyright © 2022-2023 by Pervasive Cats S.r.l.s.
 *
 * All Rights Reserved.
 */

package io.github.pervasivecats
package application.actors

import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

import scala.jdk.CollectionConverters.MapHasAsJava
import scala.util.Try

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.PostStop
import akka.actor.typed.scaladsl.Behaviors
import com.rabbitmq.client.AMQP
import com.rabbitmq.client.BuiltinExchangeType
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.MessageProperties
import com.rabbitmq.client.Return
import com.typesafe.config.Config
import spray.json.JsonWriter
import spray.json.enrichAny

import application.Serializers.given
import commands.RootCommand.Startup
import commands.MessageBrokerCommand.CustomerUnregistered
import users.customer.domainevents.CustomerUnregistered as CustomerUnregisteredEvent
import application.Serializers.given
import commands.{MessageBrokerCommand, RootCommand}

object MessageBrokerActor {

  def apply(root: ActorRef[RootCommand], messageBrokerConfig: Config): Behavior[MessageBrokerCommand] =
    Behaviors.setup[MessageBrokerCommand] { ctx =>
      Try {
        val factory: ConnectionFactory = ConnectionFactory()
        factory.setUsername(messageBrokerConfig.getString("username"))
        factory.setPassword(messageBrokerConfig.getString("password"))
        factory.setVirtualHost(messageBrokerConfig.getString("virtualHost"))
        factory.setHost(messageBrokerConfig.getString("hostName"))
        factory.setPort(messageBrokerConfig.getInt("portNumber"))
        factory.newConnection()
      }.flatMap { c =>
        val channel: Channel = c.createChannel()
        channel.addReturnListener((r: Return) => {
          println("RETURNED")
          println(String(r.getBody, StandardCharsets.UTF_8))
          ctx.system.deadLetters[String] ! String(r.getBody, StandardCharsets.UTF_8)
        })
        Try {
          channel.exchangeDeclare("users", BuiltinExchangeType.TOPIC, true)
          channel.queueDeclare("shopping_users", true, false, false, Map.empty.asJava)
          channel.queueBind("shopping_users", "users", "customer")
          channel.queueDeclare("carts_users", true, false, false, Map.empty.asJava)
          channel.queueBind("carts_users", "users", "customer")
          channel.queueDeclare("payments_users", true, false, false, Map.empty.asJava)
          channel.queueBind("payments_users", "users", "customer")
          (c, channel)
        }
      }.map { (co, ch) =>
        root ! Startup(true)
        Behaviors
          .receiveMessage[MessageBrokerCommand] {
            case CustomerUnregistered(e) =>
              ch.basicPublish(
                "users",
                "customer",
                true,
                AMQP
                  .BasicProperties
                  .Builder()
                  .contentType("application/json")
                  .deliveryMode(2)
                  .priority(0)
                  .build(),
                CustomerUnregisteredEvent(e).toJson.compactPrint.getBytes(StandardCharsets.UTF_8)
              )
              Behaviors.same[MessageBrokerCommand]
          }
          .receiveSignal {
            case (_, PostStop) =>
              ch.close()
              co.close()
              Behaviors.same[MessageBrokerCommand]
          }
      }.getOrElse {
        root ! Startup(false)
        Behaviors.stopped[MessageBrokerCommand]
      }
    }
}
