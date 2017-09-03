package com.typesafe.sbt
package license

object SbtCompat {
  val Using = sbt.io.Using
  val IvyRetrieve = sbt.internal.librarymanagement.IvyRetrieve
  type IvySbt = sbt.internal.librarymanagement.IvySbt
  type ResolveException = sbt.librarymanagement.ResolveException
}
