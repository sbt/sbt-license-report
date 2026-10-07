name := "example"

resolvers += "caller-url-repo" at (baseDirectory.value / "repo").toURI.toString
libraryDependencies += "com.example" % "parent-lib" % "1.0"

excludeDependencies += "org.scala-lang"

TaskKey[Unit]("check") := {
  val contents = sbt.IO.read(target.value / "license-reports" / "example-licenses.md")
  val apacheUrl = "https://www.apache.org/licenses/LICENSE-2.0.txt"

  def expect(row: String, message: String): Unit =
    if (!contents.contains(row)) sys.error(s"$message: " + contents)

  expect(s"[Apache-2.0]($apacheUrl) | com.example # parent-lib # 1.0 |", "Expected parent-lib with its own url")
  expect(
    s"[Apache-2.0]($apacheUrl) | com.example # child-lib # 1.0 |",
    "Expected child-lib to inherit the url from parent-lib"
  )
  expect(
    s"[Apache-2.0]($apacheUrl) | com.example # grandchild-lib # 1.0 |",
    "Expected grandchild-lib to inherit the url from parent-lib through child-lib"
  )
  expect(
    "MIT | [MIT]() | com.example # mit-lib # 1.0 |",
    "Expected mit-lib to keep an empty url as no caller declares MIT"
  )
}
