name := "example"

libraryDependencies += "com.fasterxml.jackson.core" % "jackson-databind" % "2.5.4"

excludeDependencies += SbtExclusionRule(organization = "org.scala-lang")

TaskKey[Unit]("check") := {
  val contents = sbt.IO.read(target.value / "license-reports" / "example-licenses.md")
  if (!contents.contains("jackson-databind"))
    sys.error("Expected report to contain jackson-databind: " + contents)
  // Test whether exclusions are included.
  if (contents.contains("scala-library"))
    sys.error("Expected report to NOT contain scala-library: " + contents)
}
