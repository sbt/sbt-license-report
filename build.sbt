lazy val lang3 = "org.apache.commons" % "commons-text" % "1.14.0"
lazy val repoSlug = "sbt/sbt-license-report"

val scala212 = "2.12.20"
val scala3 = "3.7.3"

pluginCrossBuild / sbtVersion := {
  scalaBinaryVersion.value match {
    case "2.12" =>
      (pluginCrossBuild / sbtVersion).value
    case _ =>
      "2.0.0-RC4"
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
  if (scalaBinaryVersion.value == "3") {
    Def
      .sequential(
        Test / test,
        Def.task(
          // TODO enable test
          streams.value.log.warn("skip sbt 2.x scripted tests")
        )
      )
      .value
  } else {
    Def
      .sequential(
        Test / test,
        scripted.toTask("")
      )
      .value
  }
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
  if (insideCI.value) {
    val log = sLog.value
    log.info("Running in CI, enabling Scala2 optimizer")
    Seq(
      "-opt-inline-from:<sources>",
      "-opt:l:inline"
    )
  } else Nil
}

ThisBuild / githubWorkflowBuild := Seq(WorkflowStep.Sbt(List("+ testAll")))

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

ThisBuild / githubWorkflowJavaVersions := Seq(
  JavaSpec.temurin("8"),
  JavaSpec.temurin("11"),
  JavaSpec.temurin("17"),
  JavaSpec.temurin("21")
)

ThisBuild / githubWorkflowBuildMatrixExclusions += MatrixExclude(Map("java" -> "temurin@8", "os" -> "macos-latest"))
