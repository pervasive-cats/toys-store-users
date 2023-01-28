/*
 * Copyright © 2022-2023 by Pervasive Cats S.r.l.s.
 *
 * All Rights Reserved.
 */

package io.github.pervasivecats
package application.actors

import java.nio.charset.StandardCharsets
import java.util.concurrent.BlockingQueue
import java.util.concurrent.Future
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.TimeUnit

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
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName
import spray.json.enrichAny
import spray.json.enrichString

import application.actors.MessageBrokerCommand.CustomerUnregistered
import application.actors.RootCommand.Startup
import application.Serializers.given
import users.customer.events.CustomerUnregistered as CustomerUnregisteredEvent
import users.customer.valueobjects.Email

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

  private val shoppingQueue: BlockingQueue[Map[String, String]] = LinkedBlockingDeque()
  private val cartsQueue: BlockingQueue[Map[String, String]] = LinkedBlockingDeque()
  private val paymentsQueue: BlockingQueue[Map[String, String]] = LinkedBlockingDeque()

  private def forwardToQueue(queue: BlockingQueue[Map[String, String]]): DeliverCallback =
    (_: String, message: Delivery) =>
      queue.put(
        Map(
          "exchange" -> message.getEnvelope.getExchange,
          "routingKey" -> message.getEnvelope.getRoutingKey,
          "body" -> String(message.getBody, StandardCharsets.UTF_8)
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
    channel.exchangeDeclare("users", BuiltinExchangeType.TOPIC, true)
    channel.queueDeclare("shopping_users", true, false, false, Map.empty.asJava)
    channel.queueBind("shopping_users", "users", "customer")
    channel.queueDeclare("carts_users", true, false, false, Map.empty.asJava)
    channel.queueBind("carts_users", "users", "customer")
    channel.queueDeclare("payments_users", true, false, false, Map.empty.asJava)
    channel.queueBind("payments_users", "users", "customer")
    channel.basicConsume("shopping_users", true, forwardToQueue(shoppingQueue), (_: String) => {})
    channel.basicConsume("carts_users", true, forwardToQueue(cartsQueue), (_: String) => {})
    channel.basicConsume("payments_users", true, forwardToQueue(paymentsQueue), (_: String) => {})
  }

  override def afterAll(): Unit = testKit.shutdownTestKit()

  describe("A message broker actor") {
    describe("after being created") {
      it("should notify its root actor about it") {
        rootActorProbe.expectMessage(10.seconds, Startup(true))
      }
    }

    describe("after being notified that a customer unregistered") {
      it("should notify the message broker") {
        val email: Email = Email("mario@email.com").getOrElse(fail())
        messageBroker.getOrElse(fail()) ! CustomerUnregistered(email)
        val shoppingMessage: Map[String, String] = shoppingQueue.poll(10, TimeUnit.SECONDS)
        shoppingMessage("exchange") shouldBe "users"
        shoppingMessage("routingKey") shouldBe "customer"
        shoppingMessage("body").parseJson.convertTo[CustomerUnregisteredEvent].email shouldBe email
        val paymentsMessage: Map[String, String] = paymentsQueue.poll(10, TimeUnit.SECONDS)
        paymentsMessage("exchange") shouldBe "users"
        paymentsMessage("routingKey") shouldBe "customer"
        paymentsMessage("body").parseJson.convertTo[CustomerUnregisteredEvent].email shouldBe email
        val cartsMessage: Map[String, String] = cartsQueue.poll(10, TimeUnit.SECONDS)
        cartsMessage("exchange") shouldBe "users"
        cartsMessage("routingKey") shouldBe "customer"
        cartsMessage("body").parseJson.convertTo[CustomerUnregisteredEvent].email shouldBe email
      }
    }
  }
}
