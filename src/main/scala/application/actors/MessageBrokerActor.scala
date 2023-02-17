/*
 * Copyright © 2022-2023 by Pervasive Cats S.r.l.s.
 *
 * All Rights Reserved.
 */

package io.github.pervasivecats
package application.actors

import application.actors.commands.MessageBrokerCommand.CustomerUnregistered
import application.actors.commands.RootCommand.Startup
import application.Serializers.given
import application.actors.commands.{MessageBrokerCommand, RootCommand}
import application.routes.entities.Entity.{ErrorResponseEntity, ResultResponseEntity}
import application.routes.entities.Entity
import users.customer.domainevents.CustomerUnregistered as CustomerUnregisteredEvent

import akka.actor.typed.*
import akka.actor.typed.scaladsl.Behaviors
import com.rabbitmq.client.*
import com.typesafe.config.Config
import spray.json.{enrichAny, JsonFormat, JsonWriter}

import java.nio.charset.{Charset, StandardCharsets}
import java.util.UUID
import scala.jdk.CollectionConverters.MapHasAsJava
import scala.util.Try

object MessageBrokerActor {

  private def publish[A <: Entity: JsonFormat](channel: Channel, response: A, routingKey: String, correlationId: String): Unit =
    channel.basicPublish(
      "users",
      routingKey,
      AMQP
        .BasicProperties
        .Builder()
        .contentType("application/json")
        .deliveryMode(2)
        .priority(0)
        .replyTo("users")
        .correlationId(correlationId)
        .build(),
      response.toJson.compactPrint.getBytes(StandardCharsets.UTF_8)
    )

  @SuppressWarnings(Array("org.wartremover.warts.ToString"))
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
          ctx.system.deadLetters[String] ! String(r.getBody, StandardCharsets.UTF_8)
          channel.basicPublish(
            "dead_letters",
            "dead_letters",
            AMQP
              .BasicProperties
              .Builder()
              .contentType("application/json")
              .deliveryMode(2)
              .priority(0)
              .build(),
            r.getBody
          )
        })
        Try {
          channel.exchangeDeclare("dead_letters", BuiltinExchangeType.FANOUT, true)
          channel.queueDeclare("dead_letters", true, false, false, Map.empty.asJava)
          channel.queueBind("dead_letters", "dead_letters", "")
          val couples: Seq[(String, String)] = Seq(
            "users" -> "shopping",
            "users" -> "carts",
            "users" -> "payments"
          )
          val queueArgs: Map[String, String] = Map("x-dead-letter-exchange" -> "dead_letters")
          couples.flatMap(Seq(_, _)).distinct.foreach(e => channel.exchangeDeclare(e, BuiltinExchangeType.TOPIC, true))
          couples
            .flatMap((b1, b2) => Seq(b1 + "_" + b2, b2 + "_" + b1))
            .foreach(q => channel.queueDeclare(q, true, false, false, queueArgs.asJava))
          couples
            .flatMap((b1, b2) => Seq((b1, b1 + "_" + b2, b2), (b2, b2 + "_" + b1, b1)))
            .foreach((e, q, r) => channel.queueBind(q, e, r))
          (c, channel)
        }
      }.map { (co, ch) =>
        root ! Startup(true)
        Behaviors
          .receiveMessage[MessageBrokerCommand] {
            case CustomerUnregistered(e) =>
              publish(ch, ResultResponseEntity(CustomerUnregisteredEvent(e)), routingKey = "shopping", UUID.randomUUID().toString)
              publish(ch, ResultResponseEntity(CustomerUnregisteredEvent(e)), routingKey = "carts", UUID.randomUUID().toString)
              publish(ch, ResultResponseEntity(CustomerUnregisteredEvent(e)), routingKey = "payments", UUID.randomUUID().toString)
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
