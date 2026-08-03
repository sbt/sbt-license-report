name := "example"

val webJarsOrg = "org.webjars"
val swaggerUiArtifact = "swagger-ui"
val swaggerUiVersion = "5.30.2"
libraryDependencies += webJarsOrg % swaggerUiArtifact % swaggerUiVersion

excludeDependencies += "org.scala-lang"

TaskKey[Unit]("check") := {
  val contents = sbt.IO.read(target.value / "license-reports" / "example-licenses.md")

  if (
    !contents.contains(
      s"[Apache-2.0]() | [$webJarsOrg # $swaggerUiArtifact # $swaggerUiVersion](https://www.webjars.org)"
    )
  )
    sys.error(s"Expected report to contain $swaggerUiArtifact with Apache license: " + contents)
  if (!contents.contains(swaggerUiArtifact))
    sys.error(s"Expected report to contain $swaggerUiArtifact: " + contents)

  // Test whether exclusions are included.
  if (contents.contains("scala-library"))
    sys.error("Expected report to NOT contain scala-library: " + contents)
}
