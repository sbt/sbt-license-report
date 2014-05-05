organization := "com.typesafe.sbt"

name := "sbt-license-report"

sbtPlugin := true

publishMavenStyle := false

scalariformSettings

versionWithGit

git.baseVersion := "1.0"

publishTo := Some(Resolver.url("sbt-plugin-releases", new URL("http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-releases/"))(Resolver.ivyStylePatterns))

publishMavenStyle := false

licenses := Seq("Apache 2" -> url("http://www.apache.org/licenses/LICENSE-2.0.html"))

libraryDependencies += "org.apache.commons" % "commons-lang3" % "3.1"
