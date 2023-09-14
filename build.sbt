lazy val lang3 = "org.apache.commons" % "commons-lang3" % "3.12.0"
lazy val repoSlug = "sbt/sbt-license-report"

val scala212 = "2.12.18"

scalaVersion := scala212
crossScalaVersions := Seq(scala212)
organization := "com.github.sbt"
name := "sbt-license-report"
enablePlugins(SbtPlugin)
libraryDependencies += lang3
scriptedLaunchOpts += s"-Dplugin.version=${version.value}"
pluginCrossBuild / sbtVersion := "1.9.5"

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
