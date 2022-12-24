/*
 * Copyright © 2022-2023 by Pervasive Cats S.r.l.s.
 *
 * All Rights Reserved.
 */

package io.github.pervasivecats
package users.administration

import org.mockito.Mockito.description
import org.mockito.Mockito.mock
import org.mockito.Mockito.when
import org.scalatest.funspec.AnyFunSpec

import users.user.valueobjects.{EncryptedPassword, PlainPassword, Username}

class AdministrationTest extends AnyFunSpec {

  describe("A administration"){
    it("must be created correctly"){
      val administration: Administration = Administration(Username("John").getOrElse(fail()))
    }
  }

}
