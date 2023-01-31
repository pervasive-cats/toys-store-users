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

import users.storemanager.valueobjects.Store
import users.user.valueobjects.{PlainPassword, Username}
import application.Serializers.given

sealed trait StoreManagerEntity extends Entity

object StoreManagerEntity {

  final case class StoreManagerRegistrationEntity(
    username: Username,
    store: Store,
    password: PlainPassword
  ) extends StoreManagerEntity

  given RootJsonFormat[StoreManagerRegistrationEntity] = jsonFormat3(StoreManagerRegistrationEntity.apply)

  final case class StoreManagerLoginEntity(
    username: Username,
    password: PlainPassword
  ) extends StoreManagerEntity

  given RootJsonFormat[StoreManagerLoginEntity] = jsonFormat2(StoreManagerLoginEntity.apply)

  final case class StoreManagerDeregistrationEntity(
    username: Username,
    password: PlainPassword
  ) extends StoreManagerEntity

  given RootJsonFormat[StoreManagerDeregistrationEntity] = jsonFormat2(StoreManagerDeregistrationEntity.apply)

  final case class StoreManagerUpdateStoreEntity(
    username: Username,
    newStore: Store
  ) extends StoreManagerEntity

  given RootJsonFormat[StoreManagerUpdateStoreEntity] = jsonFormat2(StoreManagerUpdateStoreEntity.apply)

  final case class StoreManagerUpdatePasswordEntity(
    username: Username,
    password: PlainPassword,
    newPassword: PlainPassword
  ) extends StoreManagerEntity

  given RootJsonFormat[StoreManagerUpdatePasswordEntity] = jsonFormat3(StoreManagerUpdatePasswordEntity.apply)
}
