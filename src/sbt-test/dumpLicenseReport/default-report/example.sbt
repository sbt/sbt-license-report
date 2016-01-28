name := "example"

libraryDependencies += "com.fasterxml.jackson.core" % "jackson-databind" % "2.5.4"

excludeDependencies += SbtExclusionRule(organization = "org.scala-lang")

TaskKey[Unit]("check") := {
  val contents = sbt.IO.read(target.value / "license-reports" / "example-licenses.md")
  if (!contents.contains("[The Apache Software License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) | com.fasterxml.jackson.core # jackson-databind # 2.5.4"))
    sys.error("Expected report to contain jackson-databind with Apache license: " + contents)
  // Test whether exclusions are included.
  if (contents.contains("scala-library"))
    sys.error("Expected report to NOT contain scala-library: " + contents)
}
