package com.typesafe.sbt

import sbt._
import Keys._

object SbtLicenseReport extends Plugin {
  val makeLicenseReport = TaskKey[license.LicenseReport]("makeLicenseReport", "Displays a report of used licenses in a project.")
  val dumpLicenseReport = TaskKey[Unit]("dumpLicenseReport", "Displays a report of used licenses in a project.")

  override def projectSettings: Seq[Setting[_]] =
    Seq(
      makeLicenseReport <<= (update, ivyModule, streams) map { (_, module, s) =>
        license.LicenseReport.makeReport(module, s.log)
      },
      dumpLicenseReport <<= makeLicenseReport map { report =>
        System.out.synchronized {
          license.LicenseReport.dumpReport(report, x => println(x))
        }
      })
}
