/*
 * Copyright © 2022-2023 by Pervasive Cats S.r.l.s.
 *
 * All Rights Reserved.
 */

package io.github.pervasivecats
package users.administration

import users.ValidationError

object AdministrationRepositoryError {

  case object PSQLError extends ValidationError {
    override val message: String = "The Postgresql operation failed"
  }

  case object AdministrationNotFound extends ValidationError {
    override val message: String = "No user found for the username that was provided"
  }

  case object OperationFailed extends ValidationError {
    override val message: String = "The operation on the given customer was not correctly performed"
  }

  case object UniqueViolation extends ValidationError {
    override val message: String = "Username already in use"
  }

  case object UnexpectedException extends ValidationError {
    override val message: String = "An unexpected has occurred"
  }

}