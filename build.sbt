organization := "com.typesafe.sbt"

name := "sbt-license-report"

sbtPlugin := true

publishMavenStyle := false

scalariformSettings

versionWithGit

git.baseVersion := "1.0"

publishTo := Some(Resolver.url("sbt-plugin-releases", new URL("http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-releases/"))(Resolver.ivyStylePatterns))

publishMavenStyle := false
