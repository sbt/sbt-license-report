addSbtPlugin("com.github.sbt" % "sbt-ci-release"     % "1.12.1")
addSbtPlugin("org.scalameta"  % "sbt-scalafmt"       % "2.6.2")
addSbtPlugin("com.github.sbt" % "sbt-github-actions" % "0.32.1")

// ssl-config-core 0.2.2 (via sbt-ci-release -> sbt-pgp) still asks for scala-parser-combinators 1.0.4 and config 1.2.0
// while pgp-library needs 1.1.2 and sbt-scalafmt needs config 1.4.x, pin the newer (binary compatible) versions
dependencyOverrides ++= Seq(
  "org.scala-lang.modules" %% "scala-parser-combinators" % "1.1.2",
  "com.typesafe"            % "config"                   % "1.4.9"
)
