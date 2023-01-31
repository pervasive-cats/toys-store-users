/*
 * Copyright © 2022-2023 by Pervasive Cats S.r.l.s.
 *
 * All Rights Reserved.
 */

package io.github.pervasivecats
package application.routes

import spray.json.DefaultJsonProtocol.jsonFormat2
import spray.json.DefaultJsonProtocol.jsonFormat3
import spray.json.RootJsonFormat

import users.user.valueobjects.{PlainPassword, Username}
import application.Serializers.given

sealed trait AdministrationEntity extends Entity

object AdministrationEntity {

  final case class AdministrationLoginEntity(
    username: Username,
    password: PlainPassword
  ) extends AdministrationEntity

  given RootJsonFormat[AdministrationLoginEntity] = jsonFormat2(AdministrationLoginEntity.apply)

  final case class AdministrationUpdatePasswordEntity(
    username: Username,
    password: PlainPassword,
    newPassword: PlainPassword
  ) extends AdministrationEntity

  given RootJsonFormat[AdministrationUpdatePasswordEntity] = jsonFormat3(AdministrationUpdatePasswordEntity.apply)
}
