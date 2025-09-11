name := "example"

resolvers += "Local Dummy Repo" at file("test-library").toURI.toString

libraryDependencies += "com.example" % "fake-lib" % "1.0.0"

TaskKey[Unit]("check") := {
  val contents = sbt.IO.read(target.value / "license-reports" / "example-licenses.md")
  if (contents.contains("${properties.license.name}") || contents.contains("${project.artifactId}"))
    sys.error("Expected report to NOT contain property reference: " + contents)
}
