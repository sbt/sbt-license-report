name := "example"

libraryDependencies += "com.fasterxml.jackson.core" % "jackson-databind" % "2.5.4"
libraryDependencies += "junit"                      % "junit"            % "4.12" % "test"

licenseCheckAllow := Nil

TaskKey[Unit]("check") := {
  licenseCheck.result.value match {
    case Inc(inc: Incomplete) =>
      println("licenseCheck failed as expected")
    case Value(_) =>
      sys.error("Expect licenseCheck to fail")
  }
}
