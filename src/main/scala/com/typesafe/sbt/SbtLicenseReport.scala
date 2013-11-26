package com.typesafe.sbt

import sbt._
import Keys._

object SbtLicenseReport extends Plugin {
  val makeLicenseReport = TaskKey[license.LicenseReport]("makeLicenseReport", "Displays a report of used licenses in a project.")
  val dumpLicenseReport = TaskKey[Unit]("dumpLicenseReport", "Displays a report of used licenses in a project.")
  val licenseReportCsv = SettingKey[File]("licenseReportCsv", "The location where we'll write the license report.")
  val dumpLicenseReportCsv = TaskKey[File]("dumpLicenseReportCsv", "Dumps a csv file of the license report.")
  val licenseConfigurations = SettingKey[Seq[String]]("licenseConfigurations", "The configurations we wish a report of.")

  override def projectSettings: Seq[Setting[_]] =
    Seq(
      makeLicenseReport <<= (update, ivyModule, licenseConfigurations, streams) map { (_, module, configs, s) =>
        license.LicenseReport.makeReport(module, configs, s.log)
      },
      licenseReportCsv <<= target apply (_ / "licenseReport.csv"),
      dumpLicenseReport <<= makeLicenseReport map { report =>
        System.out.synchronized {
          license.LicenseReport.dumpReport(report, x => println(x))
        }
      },
      dumpLicenseReportCsv <<= (makeLicenseReport, licenseReportCsv) map { (report, file) =>
        license.LicenseReport.withPrintableFile(file) { println =>
          license.LicenseReport.dumpCsv(report, println)
        }
        file
      },
      licenseConfigurations := Seq.empty
    )
}
