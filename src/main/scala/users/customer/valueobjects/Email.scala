/*
 * Copyright © 2022-2023 by Pervasive Cats S.r.l.s.
 *
 * All Rights Reserved.
 */

package io.github.pervasivecats
package users.customer.valueobjects

import io.github.pervasivecats.Validated
import io.github.pervasivecats.ValidationError

import eu.timepit.refined.api.RefType.applyRef
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.MatchesRegex

type EmailString = String Refined
  MatchesRegex[
    "[a-z0-9!#$%&'*+/=?^_`{|}~\\-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~\\-]+)*@(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?"
  ]

trait Email {

  val value: EmailString
}

object Email {

  private case class EmailImpl(value: EmailString) extends Email

  case object WrongEmailFormat extends ValidationError {

    override val message: String = "The email format is invalid"
  }

  def apply(value: String): Validated[Email] = applyRef[EmailString](value) match {
    case Left(_) => Left[ValidationError, Email](WrongEmailFormat)
    case Right(value) => Right[ValidationError, Email](EmailImpl(value))
  }
}
