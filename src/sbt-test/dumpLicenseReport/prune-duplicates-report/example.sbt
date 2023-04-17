name := "example"

lazy val one = project
  .in(file("one"))
  .settings(
    List(
      libraryDependencies += "com.fasterxml.jackson.core" % "jackson-core" % "2.5.4"
    )
  )

lazy val two = project
  .in(file("two"))
  .settings(
    List(
      libraryDependencies += "com.fasterxml.jackson.core" % "jackson-core" % "2.5.4" % Test
    )
  )

def countOccurrences(src: String, tgt: String): Int =
  src.sliding(tgt.length).count(window => window == tgt)

TaskKey[Unit]("check") := {
  val contents = sbt.IO.read(target.value / "license-reports" / "example-licenses.md")
  val count = countOccurrences(contents, "com.fasterxml.jackson.core")
  count match {
    case 0           => sys.error("Expected a single occurance of jackson")
    case n if n >= 2 => sys.error("Expected to only have one occurance of jackson" + contents)
    case _           => ()
  }
}
