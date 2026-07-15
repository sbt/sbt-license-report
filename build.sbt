lazy val lang3 = "org.apache.commons" % "commons-text" % "1.14.0"
lazy val repoSlug = "sbt/sbt-license-report"

val scala212 = "2.12.21"
val scala3 = "3.8.4"

pluginCrossBuild / sbtVersion := {
  scalaBinaryVersion.value match {
    case "2.12" =>
      (pluginCrossBuild / sbtVersion).value
    case _ =>
      "2.0.2"
  }
}

ThisBuild / scalaVersion := scala212
ThisBuild / crossScalaVersions := Seq(scala212, scala3)
organization := "com.github.sbt"
name := "sbt-license-report"
enablePlugins(SbtPlugin)
libraryDependencies += lang3
scriptedLaunchOpts += s"-Dplugin.version=${version.value}"

ThisBuild / githubWorkflowScalaVersions := Seq(scalaVersion.value)

TaskKey[Unit]("testAll") := {
  Def
    .sequential(
      Test / test,
      scripted.toTask("")
    )
    .value
}

// publishing info
licenses := Seq("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.html"))
scmInfo := Some(
  ScmInfo(
    url(s"https://github.com/$repoSlug"),
    s"scm:git@github.com:sbt/$repoSlug.git"
  )
)
developers := List(
  Developer(
    id = "jsuereth",
    name = "Josh Suereth",
    email = "@jsuereth",
    url = url("http://jsuereth.com/")
  )
)
description := "An sbt plugin to report on licenses used in a project."
homepage := Some(url(s"https://github.com/$repoSlug"))
pomIncludeRepository := { _ =>
  false
}
publishMavenStyle := true

// compile settings
scalacOptions ++= List(
  "-unchecked",
  "-deprecation",
  "-encoding",
  "UTF-8"
)

scalacOptions ++= {
  if (insideCI.value && scalaBinaryVersion.value == "2.12") {
    val log = sLog.value
    log.info("Running in CI, enabling Scala2 optimizer")
    Seq(
      "-Xsource:3",
      "-release:8",
      "-opt-inline-from:<sources>",
      "-opt:l:inline"
    )
  } else Nil
}

ThisBuild / githubWorkflowBuild := Seq(
  WorkflowStep.Sbt(
    List("testAll"),
    name = Some("Build project (only sbt 1.x)"),
    cond = Some("matrix.java == 'zulu@8' || matrix.java == 'zulu@11'")
  ),
  WorkflowStep.Sbt(
    List("+ testAll"),
    name = Some("Build project (cross build on sbt 1.x and 2.x)"),
    cond = Some("matrix.java != 'zulu@8' && matrix.java != 'zulu@11'")
  )
)

ThisBuild / githubWorkflowTargetTags ++= Seq("v*")
ThisBuild / githubWorkflowPublishTargetBranches :=
  Seq(
    RefPredicate.StartsWith(Ref.Tag("v")),
    RefPredicate.Equals(Ref.Branch("main"))
  )
ThisBuild / githubWorkflowPublish := Seq(
  WorkflowStep.Sbt(
    commands = List("ci-release"),
    name = Some("Publish project"),
    env = Map(
      "PGP_PASSPHRASE" -> "${{ secrets.PGP_PASSPHRASE }}",
      "PGP_SECRET" -> "${{ secrets.PGP_SECRET }}",
      "SONATYPE_PASSWORD" -> "${{ secrets.SONATYPE_PASSWORD }}",
      "SONATYPE_USERNAME" -> "${{ secrets.SONATYPE_USERNAME }}"
    )
  )
)

ThisBuild / githubWorkflowOSes := Seq("ubuntu-latest", "macos-latest", "windows-latest")

ThisBuild / githubWorkflowPublishJavaVersion := JavaSpec.temurin("17")

ThisBuild / githubWorkflowJavaVersions := Seq(
  JavaSpec.temurin("17"),
  JavaSpec.temurin("21"),
  JavaSpec.temurin("25"),
  JavaSpec.zulu("8"), // only for SBT 1.x
  JavaSpec.zulu("11") // only for SBT 1.x
)

ThisBuild / githubWorkflowBuildMatrixExclusions += MatrixExclude(Map("java" -> "zulu@8", "os" -> "macos-latest"))
