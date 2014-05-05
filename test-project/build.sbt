import com.typesafe.sbt.license.DepModuleInfo
import com.typesafe.sbt.SbtLicenseReport.autoImport.licenseReportNotes

libraryDependencies += "com.typesafe.play" %% "play" % "2.2.2"

libraryDependencies ++= Seq(
  "com.typesafe.slick" %% "slick" % "2.0.1",
  "org.slf4j" % "slf4j-nop" % "1.6.4"
)

scalaVersion := "2.10.4"

licenseReportNotes := licenseReportNotes.value orElse {
  case DepModuleInfo(org, _, _) if org contains "com.typesafe" => "From Typesafe Reactive Platform."
}