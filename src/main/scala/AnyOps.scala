/*
 * Copyright © 2022-2023 by Pervasive Cats S.r.l.s.
 *
 * All Rights Reserved.
 */

package io.github.pervasivecats

@SuppressWarnings(Array("org.wartremover.warts.Equals", "scalafix:DisableSyntax.=="))
object AnyOps {

  extension [A](self: A) {

    def ===(other: A): Boolean = self == other

    def !===(other: A): Boolean = !(self === other)
  }

}
