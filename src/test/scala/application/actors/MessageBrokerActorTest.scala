/*
 * Copyright © 2022-2023 by Pervasive Cats S.r.l.s.
 *
 * All Rights Reserved.
 */

package io.github.pervasivecats
package application.actors

import java.nio.charset.StandardCharsets
import java.util.concurrent.*

import scala.concurrent.duration.DurationInt
import scala.concurrent.duration.FiniteDuration
import scala.jdk.CollectionConverters.MapHasAsJava

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.testkit.typed.scaladsl.TestProbe
import akka.actor.typed.ActorRef
import com.dimafeng.testcontainers
import com.dimafeng.testcontainers.GenericContainer
import com.dimafeng.testcontainers.GenericContainer.DockerImage
import com.dimafeng.testcontainers.JdbcDatabaseContainer.CommonParams
import com.dimafeng.testcontainers.PostgreSQLContainer
import com.dimafeng.testcontainers.lifecycle.and
import com.dimafeng.testcontainers.scalatest.TestContainerForAll
import com.rabbitmq.client.*
import com.typesafe.config.*
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers.*
import org.testcontainers.containers.wait.strategy.*
import org.testcontainers.utility.DockerImageName
import spray.json.enrichAny
import spray.json.enrichString

import application.actors.commands.MessageBrokerCommand.CustomerUnregistered
import application.actors.commands.RootCommand.Startup
import application.routes.entities.Entity.{ErrorResponseEntity, ResultResponseEntity}
import application.Serializers.given
import application.actors.commands.{MessageBrokerCommand, RootCommand}
import users.customer.domainevents.CustomerUnregistered as CustomerUnregisteredEvent
import users.customer.valueobjects.Email
import users.customer.Repository.CustomerNotFound

class MessageBrokerActorTest extends AnyFunSpec with TestContainerForAll with BeforeAndAfterAll {

  override val containerDef: GenericContainer.Def[GenericContainer] = GenericContainer.Def(
    dockerImage = DockerImage(Left[String, Future[String]]("rabbitmq:3.11.7")),
    exposedPorts = Seq(5672),
    env = Map(
      "RABBITMQ_DEFAULT_USER" -> "test",
      "RABBITMQ_DEFAULT_PASS" -> "test"
    ),
    waitStrategy = LogMessageWaitStrategy().withRegEx("^.*?Server startup complete.*?$")
  )

  private val testKit: ActorTestKit = ActorTestKit()
  private val rootActorProbe: TestProbe[RootCommand] = testKit.createTestProbe[RootCommand]()

  @SuppressWarnings(Array("org.wartremover.warts.Var", "scalafix:DisableSyntax.var"))
  private var messageBroker: Option[ActorRef[MessageBrokerCommand]] = None

  @SuppressWarnings(Array("org.wartremover.warts.Var", "scalafix:DisableSyntax.var"))
  private var maybeChannel: Option[Channel] = None

  private val shoppingQueue: BlockingQueue[Map[String, String]] = LinkedBlockingDeque()
  private val paymentsQueue: BlockingQueue[Map[String, String]] = LinkedBlockingDeque()

  private val event = CustomerUnregisteredEvent(Email("mario@email.com").getOrElse(fail()))

  private def forwardToQueue(queue: BlockingQueue[Map[String, String]]): DeliverCallback =
    (_: String, message: Delivery) =>
      queue.put(
        Map(
          "exchange" -> message.getEnvelope.getExchange,
          "routingKey" -> message.getEnvelope.getRoutingKey,
          "body" -> String(message.getBody, StandardCharsets.UTF_8),
          "contentType" -> message.getProperties.getContentType,
          "correlationId" -> message.getProperties.getCorrelationId,
          "replyTo" -> message.getProperties.getReplyTo
        )
      )

  override def afterContainersStart(containers: Containers): Unit = {
    val messageBrokerConfig: Config =
      ConfigFactory
        .load()
        .getConfig("messageBroker")
        .withValue(
          "portNumber",
          ConfigValueFactory.fromAnyRef(containers.container.getFirstMappedPort.intValue())
        )
    messageBroker = Some(testKit.spawn(MessageBrokerActor(rootActorProbe.ref, messageBrokerConfig)))
    val factory: ConnectionFactory = ConnectionFactory()
    factory.setUsername(messageBrokerConfig.getString("username"))
    factory.setPassword(messageBrokerConfig.getString("password"))
    factory.setVirtualHost(messageBrokerConfig.getString("virtualHost"))
    factory.setHost(messageBrokerConfig.getString("hostName"))
    factory.setPort(messageBrokerConfig.getInt("portNumber"))
    val connection: Connection = factory.newConnection()
    val channel: Channel = connection.createChannel()
    val couples: Seq[(String, String)] = Seq(
      "users" -> "shopping",
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
    channel.basicConsume("users_shopping", true, forwardToQueue(shoppingQueue), (_: String) => {})
    channel.basicConsume("users_payments", true, forwardToQueue(paymentsQueue), (_: String) => {})
    maybeChannel = Some(channel)
  }

  override def afterAll(): Unit = testKit.shutdownTestKit()

  describe("A message broker actor") {
    describe("after being created") {
      it("should notify its root actor about it") {
        rootActorProbe.expectMessage(10.seconds, Startup(true))
      }
    }

    @SuppressWarnings(Array("org.wartremover.warts.Var", "scalafix:DisableSyntax.var"))
    var maybeCorrelationId: Option[String] = None

    describe("after being notified that a customer unregistered") {
      it("should notify the message broker") {
        messageBroker.getOrElse(fail()) ! CustomerUnregistered(event)
        val shoppingMessage: Map[String, String] = shoppingQueue.poll(10, TimeUnit.SECONDS)
        shoppingMessage("exchange") shouldBe "users"
        shoppingMessage("routingKey") shouldBe "shopping"
        shoppingMessage("contentType") shouldBe "application/json"
        shoppingMessage(
          "correlationId"
        ) should fullyMatch regex "[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}"
        shoppingMessage("replyTo") shouldBe "users"
        shoppingMessage("body").parseJson.convertTo[ResultResponseEntity[CustomerUnregisteredEvent]].result shouldBe event
        maybeCorrelationId = Some(shoppingMessage("correlationId"))
        val paymentsMessage: Map[String, String] = paymentsQueue.poll(10, TimeUnit.SECONDS)
        paymentsMessage("exchange") shouldBe "users"
        paymentsMessage("routingKey") shouldBe "payments"
        paymentsMessage("contentType") shouldBe "application/json"
        paymentsMessage(
          "correlationId"
        ) should fullyMatch regex "[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}"
        paymentsMessage("replyTo") shouldBe "users"
        paymentsMessage("body").parseJson.convertTo[ResultResponseEntity[CustomerUnregisteredEvent]].result shouldBe event
      }
    }

    describe("after receiving an error reply from the message broker") {
      it("should resend the message") {
        val channel: Channel = maybeChannel.getOrElse(fail())
        channel.basicPublish(
          "shopping",
          "users",
          AMQP
            .BasicProperties
            .Builder()
            .contentType("application/json")
            .deliveryMode(2)
            .priority(0)
            .replyTo("users")
            .correlationId(maybeCorrelationId.getOrElse(fail()))
            .build(),
          ErrorResponseEntity(CustomerNotFound).toJson.compactPrint.getBytes(StandardCharsets.UTF_8)
        )
        val shoppingMessage: Map[String, String] = shoppingQueue.poll(10, TimeUnit.SECONDS)
        shoppingMessage("exchange") shouldBe "users"
        shoppingMessage("routingKey") shouldBe "shopping"
        shoppingMessage("contentType") shouldBe "application/json"
        shoppingMessage(
          "correlationId"
        ) should fullyMatch regex "[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}"
        shoppingMessage("replyTo") shouldBe "users"
        shoppingMessage("body").parseJson.convertTo[ResultResponseEntity[CustomerUnregisteredEvent]].result shouldBe event
      }
    }
  }
}
