lazy val lang3 = "org.apache.commons" % "commons-text" % "1.12.0"
lazy val repoSlug = "sbt/sbt-license-report"

val scala212 = "2.12.19"

ThisBuild / scalaVersion := scala212
ThisBuild / crossScalaVersions := Seq(scala212)
organization := "com.github.sbt"
name := "sbt-license-report"
enablePlugins(SbtPlugin)
libraryDependencies += lang3
scriptedLaunchOpts += s"-Dplugin.version=${version.value}"

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
  "-language:_",
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

ThisBuild / githubWorkflowBuild := Seq(WorkflowStep.Sbt(List("test", "scripted")))

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

// GitHub Actions macOS 13+ runner images do not come with sbt preinstalled anymore
ThisBuild / githubWorkflowBuildPreamble ++= Seq(
  WorkflowStep.Run(
    commands = List(
      "brew install sbt"
    ),
    cond = Some("matrix.os == 'macos-latest'"),
    name = Some("Install sbt")
  )
)
