name := "example"

libraryDependencies += "com.fasterxml.jackson.core" % "jackson-databind" % "2.5.4"
libraryDependencies += "junit"                      % "junit"            % "4.12" % "test"

excludeDependencies += "org.scala-lang"

licenseDepExclusions := { case DepModuleInfo("junit", _, _) =>
  true
}

TaskKey[Unit]("check") := {
  val contents = sbt.IO.read(target.value / "license-reports" / "example-licenses.md")
  if (contents.contains("junit"))
    sys.error("Expected report to NOT contain junit directly: " + contents)
  if (!(contents.contains("hamcrest")))
    sys.error("Expected report to contain hamcrest which is a transitive dependency of junit: " + contents)
  // Test whether exclusions are included.
  if (contents.contains("scala-library"))
    sys.error("Expected report to NOT contain scala-library: " + contents)
}
