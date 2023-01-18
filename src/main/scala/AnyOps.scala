/*
 * Copyright © 2022-2023 by Pervasive Cats S.r.l.s.
 *
 * All Rights Reserved.
 */

package io.github.pervasivecats

import scala.annotation.targetName

@SuppressWarnings(Array("org.wartremover.warts.Equals", "scalafix:DisableSyntax.=="))
object AnyOps {

  extension [A](self: A) {

    @targetName("equals")
    @SuppressWarnings(Array("org.wartremover.warts.Equals", "scalafix:DisableSyntax.=="))
    def ===(other: A): Boolean = self == other

    @targetName("notEquals")
    def !==(other: A): Boolean = !(self === other)
  }

}
