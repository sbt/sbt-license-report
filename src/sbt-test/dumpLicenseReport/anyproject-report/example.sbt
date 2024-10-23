name := "example"

ThisBuild / excludeDependencies += "org.scala-lang"

lazy val one = project
  .in(file("one"))
  .settings(
    List(
      libraryDependencies += "com.fasterxml.jackson.core" % "jackson-databind" % "2.5.4",
      libraryDependencies += "junit"                      % "junit"            % "4.12" % "test"
    )
  )

lazy val two = project
  .in(file("two"))
  .settings(
    List(
      libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.4.6"
    )
  )

TaskKey[Unit]("check") := {
  val contents = sbt.IO.read(target.value / "license-reports" / "example-licenses.md")
  if (
    !contents.contains(
      "[The Apache Software License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt) | [com.fasterxml.jackson.core # jackson-databind # 2.5.4](http://github.com/FasterXML/jackson)"
    )
  )
    sys.error("Expected report to contain jackson-databind with Apache license: " + contents)
  if (!contents.contains("jackson-databind"))
    sys.error("Expected report to contain jackson-databind: " + contents)
  if (!contents.contains("logback-classic"))
    sys.error("Expected report to contain logback-classic:" + contents)
  if (
    !contents.contains(
      "[Eclipse Public License 1.0](http://www.eclipse.org/legal/epl-v10.html) | [junit # junit # 4.12](http://junit.org)"
    )
  )
    sys.error("Expected report to contain junit with EPL license: " + contents)
  // Test whether exclusions are included.
  if (contents.contains("scala-library"))
    sys.error("Expected report to NOT contain scala-library: " + contents)
}
