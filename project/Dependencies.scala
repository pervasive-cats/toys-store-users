import sbt._

object Dependencies {
  lazy val scalactic: ModuleID = "org.scalactic" %% "scalactic" % "3.2.14"
  lazy val scalatest: ModuleID = "org.scalatest" %% "scalatest" % "3.2.14" % Test
  lazy val refined: ModuleID = "eu.timepit" %% "refined" % "0.10.1"
  lazy val bcrypt: ModuleID = "at.favre.lib" % "bcrypt" % "0.9.0"
}
