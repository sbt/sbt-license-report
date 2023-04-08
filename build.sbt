organization := "com.github.sbt"
name := "sbt-license-report"
licenses := Seq("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.html"))

enablePlugins(SbtPlugin)
sbtPlugin := true
libraryDependencies += "org.apache.commons" % "commons-lang3" % "3.12.0"
scriptedLaunchOpts += s"-Dplugin.version=${version.value}"
