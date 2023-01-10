/*
 * Copyright © 2022-2023 by Pervasive Cats S.r.l.s.
 *
 * All Rights Reserved.
 */

package io.github.pervasivecats
package users.storemanager

import users.ValidationError

object StoreManagerRepositoryError {

  case object PSQLError extends ValidationError {

    override val message: String = "The Postgresql operation failed"
  }

  case object UserNotFound extends ValidationError {

    override val message: String = "No user found for the username that was provided"
  }

  case object UniqueViolation extends ValidationError {

    override val message: String = "Username already in use"
  }

  case object UnexpectedException extends ValidationError {

    override val message: String = "An unexpected exception has occurred"
  }
}
