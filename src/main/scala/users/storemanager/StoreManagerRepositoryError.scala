/*
 * Copyright © 2022-2023 by Pervasive Cats S.r.l.s.
 *
 * All Rights Reserved.
 */

package io.github.pervasivecats
package users.storemanager

import users.ValidationError

object StoreManagerRepositoryError {

  case object RepositoryError extends ValidationError {

    override val message: String = "The repository operation has failed"
  }

  case object UserNotFound extends ValidationError {

    override val message: String = "No user found for the username that was provided"
  }
}
