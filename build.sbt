import Dependencies._

Global / onChangedBuildSource := ReloadOnSourceChanges

Global / excludeLintKeys := Set(idePackagePrefix)

Test / fork := true

ThisBuild / scalaVersion := "3.2.2"

ThisBuild / scalafixDependencies ++= Seq(
  "com.github.liancheng" %% "organize-imports" % "0.6.0",
  "io.github.ghostbuster91.scalafix-unified" %% "unified" % "0.0.8",
  "net.pixiv" %% "scalafix-pixiv-rule" % "4.1.0"
)

ThisBuild / idePackagePrefix := Some("io.github.pervasivecats")

Test / fork := true

lazy val root = project
  .in(file("."))
  .enablePlugins(AutomateHeaderPlugin)
  .settings(
    name := "toys-store-users",
    scalacOptions ++= Seq(
      "-deprecation",
      "-Xfatal-warnings"
    ),
    semanticdbEnabled := true,
    semanticdbVersion := scalafixSemanticdb.revision,
    libraryDependencies ++= Seq(
      scalactic,
      scalatest,
      mockito,
      refined,
      bcrypt,
      quill,
      postgresql,
      testContainers,
      testContainersPostgresql,
      akka,
      akkaStream,
      akkaHttp,
      akkaHttpSprayJson,
      akkaTestKit,
      rabbitMQ,
      akkaStreamTestkit,
      akkaHttpTestkit
    ),
    wartremoverErrors ++= Warts.allBut(Wart.ImplicitParameter),
    version := "1.0.4",
    coverageMinimumStmtTotal := 80,
    coverageMinimumBranchTotal := 80,
    headerLicense := Some(
      HeaderLicense.Custom(
        """|Copyright © 2022-2023 by Pervasive Cats S.r.l.s.
           |
           |All Rights Reserved.
           |""".stripMargin
      )
    ),
    assembly / assemblyJarName := "main.jar",
    assembly / mainClass := Some("io.github.pervasivecats.main"),
    assembly / assemblyMergeStrategy := {
      case PathList("io", "getquill", _*) => MergeStrategy.first
      case v => MergeStrategy.defaultMergeStrategy(v)
    }
  )
