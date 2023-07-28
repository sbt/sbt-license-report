name := "example"

libraryDependencies += "com.fasterxml.jackson.core" % "jackson-databind" % "2.5.4"
libraryDependencies += "junit"                      % "junit"            % "4.12" % "test"

licenseCheckAllow := Seq(LicenseCategory.Apache, LicenseCategory.BSD, LicenseCategory.EPL)
