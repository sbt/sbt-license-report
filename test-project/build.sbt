import com.typesafe.sbt.license._
import com.typesafe.sbt.SbtLicenseReport.autoImport._

libraryDependencies += "com.typesafe.play" %% "play" % "2.2.2"

libraryDependencies ++= Seq(
  "com.typesafe.slick" %% "slick"     % "2.0.1",
  "org.slf4j"           % "slf4j-nop" % "1.6.4"
)

scalaVersion := "2.10.4"

licenseReportNotes := licenseReportNotes.value orElse {
  case DepModuleInfo(org, _, _) if org contains "com.typesafe" => "From Typesafe Reactive Platform."
}

licenseOverrides := licenseOverrides.value orElse { case DepModuleInfo("com.typesafe.play", _, _) =>
  LicenseInfo(LicenseCategory.Apache, "Apache 2", "http://www.apache.org/licenses/LICENSE-2.0")
}

// Test adding custom reports.
licenseReportConfigurations +=
  LicenseReportConfiguration(
    "test-config",
    Seq(MarkDown),
    language => language.header1("Testing the configuration"),
    dep => Option("Default notes"),
    category => category == LicenseCategory.BSD,
    licenseReportDir.value
  )
