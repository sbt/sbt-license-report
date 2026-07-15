name := "example"

libraryDependencies += "com.fasterxml.jackson.core" % "jackson-databind" % "2.5.4"
libraryDependencies += "junit"                      % "junit"            % "4.12" % "test"

licenseCheckAllow := Nil

import sbtcompat.PluginCompat.*

TaskKey[Unit]("check") := Def.uncached {
  licenseCheck.result.value.toEither match {
    case Left(_: Incomplete) =>
      println("licenseCheck failed as expected")
    case Right(_) =>
      sys.error("Expect licenseCheck to fail")
  }
}
