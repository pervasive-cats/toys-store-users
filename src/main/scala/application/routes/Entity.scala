/*
 * Copyright © 2022-2023 by Pervasive Cats S.r.l.s.
 *
 * All Rights Reserved.
 */

package io.github.pervasivecats
package application.routes

import scala.reflect.ClassTag

import spray.json.DefaultJsonProtocol.jsonFormat2
import spray.json.DefaultJsonProtocol.jsonFormat3
import spray.json.DefaultJsonProtocol.jsonFormat5
import spray.json.JsArray
import spray.json.JsBoolean
import spray.json.JsNull
import spray.json.JsNumber
import spray.json.JsObject
import spray.json.JsString
import spray.json.JsValue
import spray.json.JsonFormat
import spray.json.JsonReader
import spray.json.JsonWriter
import spray.json.RootJsonFormat
import spray.json.RootJsonReader
import spray.json.RootJsonWriter
import spray.json.deserializationError
import spray.json.jsonReader

import application.Serializers.given
import application.actors.CustomerServerActor
import application.actors.CustomerServerActor.ProcessingFailed
import application.routes.Routes.{DeserializationFailed, RequestFailed}
import users.customer.valueobjects.{Email, NameComponent}
import users.user.valueobjects.*
import users.ValidationError
import users.customer.Repository
import users.customer.Repository.*
import users.customer.valueobjects.Email.WrongEmailFormat
import users.customer.valueobjects.NameComponent.WrongNameComponentFormat
import users.user.services.PasswordAlgorithm
import users.user.services.PasswordAlgorithm.PasswordNotMatching
import users.user.valueobjects.EncryptedPassword.WrongEncryptedPasswordFormat
import users.user.valueobjects.PlainPassword.WrongPlainPasswordFormat
import users.user.valueobjects.Username.WrongUsernameFormat

trait Entity

object Entity {

  case class CustomerRegistrationEntity(
    email: Email,
    username: Username,
    firstName: NameComponent,
    lastName: NameComponent,
    password: PlainPassword
  ) extends Entity

  given RootJsonFormat[CustomerRegistrationEntity] = jsonFormat5(CustomerRegistrationEntity.apply)

  case class CustomerLoginEntity(
    email: Email,
    password: PlainPassword
  ) extends Entity

  given RootJsonFormat[CustomerLoginEntity] = jsonFormat2(CustomerLoginEntity.apply)

  case class CustomerDeregistrationEntity(
    email: Email,
    password: PlainPassword
  ) extends Entity

  given RootJsonFormat[CustomerDeregistrationEntity] = jsonFormat2(CustomerDeregistrationEntity.apply)

  case class CustomerUpdateDataEntity(
    email: Email,
    newEmail: Email,
    newUsername: Username,
    newFirstName: NameComponent,
    newLastName: NameComponent
  ) extends Entity

  given RootJsonFormat[CustomerUpdateDataEntity] = jsonFormat5(CustomerUpdateDataEntity.apply)

  case class CustomerUpdatePasswordEntity(
    email: Email,
    password: PlainPassword,
    newPassword: PlainPassword
  ) extends Entity

  given RootJsonFormat[CustomerUpdatePasswordEntity] = jsonFormat3(CustomerUpdatePasswordEntity.apply)

  case class ResultResponseEntity[A](result: A) extends Entity

  given [A: JsonFormat]: RootJsonFormat[ResultResponseEntity[A]] with {

    override def read(json: JsValue): ResultResponseEntity[A] = json.asJsObject.getFields("result", "error") match {
      case Seq(result, JsNull) => ResultResponseEntity(summon[JsonReader[A]].read(result))
      case _ => deserializationError(msg = "Json format was not valid")
    }

    override def write(response: ResultResponseEntity[A]): JsValue = JsObject(
      "result" -> summon[JsonWriter[A]].write(response.result),
      "error" -> JsNull
    )
  }

  case class ErrorResponseEntity(error: ValidationError) extends Entity

  given RootJsonFormat[ErrorResponseEntity] with {

    override def read(json: JsValue): ErrorResponseEntity = json.asJsObject.getFields("result", "error") match {
      case Seq(JsNull, error) =>
        error.asJsObject.getFields("type", "message") match {
          case Seq(JsString(tpe), JsString(message)) =>
            println(tpe)
            ErrorResponseEntity(tpe match {
              case "CustomerAlreadyPresent" => CustomerAlreadyPresent
              case "WrongEncryptedPasswordFormat" => WrongEncryptedPasswordFormat
              case "WrongNameComponentFormat" => WrongNameComponentFormat
              case "WrongPlainPasswordFormat" => WrongPlainPasswordFormat
              case "OperationFailed" => OperationFailed
              case "CustomerNotFound" => CustomerNotFound
              case "ProcessingFailed" => ProcessingFailed
              case "WrongEmailFormat" => WrongEmailFormat
              case "PasswordNotMatching" => PasswordNotMatching
              case "WrongUsernameFormat" => WrongUsernameFormat
              case "RequestFailed" => RequestFailed
              case "DeserializationFailed" => DeserializationFailed(message)
              case _ => deserializationError(msg = "Json format was not valid")
            })
          case _ => deserializationError(msg = "Json format was not valid")
        }
      case _ => deserializationError(msg = "Json format was not valid")
    }

    override def write(response: ErrorResponseEntity): JsValue = JsObject(
      "result" -> JsNull,
      "error" -> JsObject(
        "type" -> JsString(response.error.getClass.getSimpleName.replace("$", "")),
        "message" -> JsString(response.error.message)
      )
    )
  }
}
