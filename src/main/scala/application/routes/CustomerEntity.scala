/*
 * Copyright © 2022-2023 by Pervasive Cats S.r.l.s.
 *
 * All Rights Reserved.
 */

package io.github.pervasivecats
package application.routes

import spray.json.DefaultJsonProtocol.jsonFormat2
import spray.json.DefaultJsonProtocol.jsonFormat3
import spray.json.DefaultJsonProtocol.jsonFormat5
import spray.json.RootJsonFormat

import users.customer.valueobjects.{Email, NameComponent}
import users.user.valueobjects.{PlainPassword, Username}
import application.Serializers.given

sealed trait CustomerEntity extends Entity

object CustomerEntity {

  final case class CustomerRegistrationEntity(
    email: Email,
    username: Username,
    firstName: NameComponent,
    lastName: NameComponent,
    password: PlainPassword
  ) extends CustomerEntity

  given RootJsonFormat[CustomerRegistrationEntity] = jsonFormat5(CustomerRegistrationEntity.apply)

  final case class CustomerLoginEntity(
    email: Email,
    password: PlainPassword
  ) extends CustomerEntity

  given RootJsonFormat[CustomerLoginEntity] = jsonFormat2(CustomerLoginEntity.apply)

  final case class CustomerDeregistrationEntity(
    email: Email,
    password: PlainPassword
  ) extends CustomerEntity

  given RootJsonFormat[CustomerDeregistrationEntity] = jsonFormat2(CustomerDeregistrationEntity.apply)

  final case class CustomerUpdateDataEntity(
    email: Email,
    newEmail: Email,
    newUsername: Username,
    newFirstName: NameComponent,
    newLastName: NameComponent
  ) extends CustomerEntity

  given RootJsonFormat[CustomerUpdateDataEntity] = jsonFormat5(CustomerUpdateDataEntity.apply)

  final case class CustomerUpdatePasswordEntity(
    email: Email,
    password: PlainPassword,
    newPassword: PlainPassword
  ) extends CustomerEntity

  given RootJsonFormat[CustomerUpdatePasswordEntity] = jsonFormat3(CustomerUpdatePasswordEntity.apply)
}
