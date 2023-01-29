/*
 * Copyright © 2022-2023 by Pervasive Cats S.r.l.s.
 *
 * All Rights Reserved.
 */

package io.github.pervasivecats
package application

import eu.timepit.refined.auto.given
import spray.json.DefaultJsonProtocol
import spray.json.JsObject
import spray.json.JsString
import spray.json.JsValue
import spray.json.JsonFormat
import spray.json.JsonWriter
import spray.json.RootJsonFormat
import spray.json.deserializationError
import spray.json.enrichAny
import spray.json.jsonReader

import users.customer.valueobjects.{Email, NameComponent}
import users.user.valueobjects.{PlainPassword, Username}
import users.Validated
import users.customer.entities.Customer
import users.customer.events.CustomerUnregistered

object Serializers extends DefaultJsonProtocol {

  private def stringSerializer[A](extractor: A => String, builder: String => Validated[A]): JsonFormat[A] = new JsonFormat[A] {

    override def read(json: JsValue): A = json match {
      case JsString(value) => builder(value).fold(e => deserializationError(e.message), identity)
      case _ => deserializationError(msg = "Json format is not valid")
    }

    override def write(obj: A): JsValue = extractor(obj).toJson
  }

  given JsonFormat[PlainPassword] = stringSerializer[PlainPassword](_.value, PlainPassword.apply)

  given JsonFormat[Username] = stringSerializer[Username](_.value, Username.apply)

  given JsonFormat[Email] = stringSerializer[Email](_.value, Email.apply)

  given JsonFormat[NameComponent] = stringSerializer[NameComponent](_.value, NameComponent.apply)

  given JsonFormat[Customer] with {

    override def read(json: JsValue): Customer = json.asJsObject.getFields("username", "email", "first_name", "last_name") match {
      case Seq(JsString(username), JsString(email), JsString(firstName), JsString(lastName)) =>
        (for {
          u <- Username(username)
          e <- Email(email)
          f <- NameComponent(firstName)
          l <- NameComponent(lastName)
        } yield Customer(f, l, e, u)).fold(e => deserializationError(e.message), identity)
      case _ => deserializationError(msg = "Json format is not valid")
    }

    override def write(customer: Customer): JsValue = JsObject(
      "username" -> customer.username.toJson,
      "email" -> customer.email.toJson,
      "first_name" -> customer.firstName.toJson,
      "last_name" -> customer.lastName.toJson
    )
  }

  given JsonFormat[CustomerUnregistered] with {

    override def read(json: JsValue): CustomerUnregistered = json.asJsObject.getFields("type", "email") match {
      case Seq(JsString("CustomerUnregistered"), JsString(email)) =>
        Email(email).fold(e => deserializationError(e.message), CustomerUnregistered.apply)
      case _ => deserializationError(msg = "Json format is not valid")
    }

    override def write(event: CustomerUnregistered): JsValue = JsObject(
      "type" -> "CustomerUnregistered".toJson,
      "email" -> event.email.toJson
    )
  }
}