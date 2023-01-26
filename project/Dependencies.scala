import sbt._

object Dependencies {

  lazy val scalactic: ModuleID = "org.scalactic" %% "scalactic" % "3.2.14"

  lazy val scalatest: ModuleID = "org.scalatest" %% "scalatest" % "3.2.14" % Test

  lazy val mockito: ModuleID = "org.mockito" % "mockito-core" % "4.10.0" % Test

  lazy val refined: ModuleID = "eu.timepit" %% "refined" % "0.10.1"

  lazy val bcrypt: ModuleID = "at.favre.lib" % "bcrypt" % "0.9.0"

  lazy val postgresql: ModuleID = "org.postgresql" % "postgresql" % "42.5.1"

  lazy val quill: ModuleID = "io.getquill" %% "quill-jdbc" % "4.6.0"

  lazy val testContainers: ModuleID = "com.dimafeng" %% "testcontainers-scala-scalatest" % "0.40.12" % Test

  lazy val testContainersPostgresql: ModuleID = "com.dimafeng" %% "testcontainers-scala-postgresql" % "0.40.12" % Test

  lazy val akka: ModuleID = "com.typesafe.akka" %% "akka-actor-typed" % "2.7.0"

  lazy val akkaStream: ModuleID = "com.typesafe.akka" %% "akka-stream" % "2.7.0"

  lazy val akkaHttp: ModuleID = ("com.typesafe.akka" %% "akka-http" % "10.4.0").cross(CrossVersion.for3Use2_13)

  lazy val akkaHttpSprayJson: ModuleID =
    ("com.typesafe.akka" %% "akka-http-spray-json" % "10.4.0").cross(CrossVersion.for3Use2_13)

  lazy val akkaTestKit: ModuleID = "com.typesafe.akka" %% "akka-actor-testkit-typed" % "2.7.0" % Test

  lazy val rabbitMQ: ModuleID = "com.rabbitmq" % "amqp-client" % "5.16.0"

  lazy val akkaStreamTestkit: ModuleID = "com.typesafe.akka" %% "akka-stream-testkit" % "2.7.0" % Test

  lazy val akkaHttpTestkit: ModuleID =
    ("com.typesafe.akka" %% "akka-http-testkit" % "10.4.0" % Test).cross(CrossVersion.for3Use2_13)
}
