package com.typesafe.sbt

import sbt._
import Keys._
import license._



/** A plugin which enables reporting on licensing used within a project. */
object SbtLicenseReport extends AutoPlugin {
  override def requires: Plugins = plugins.IvyPlugin
  override def trigger = allRequirements
  
  object autoImports {
    val makeLicenseReport = taskKey[LicenseReport]("Displays a report of used licenses in a project.")
    val dumpLicenseReport = taskKey[File]("Dumps a report file of the license report (using the target language).")
    val licenseReportDir = settingKey[File]("The location where we'll write the license reports.")
    val licenseReportTitle = settingKey[String]("The name of the license report.")
    val licenseConfigurations = settingKey[Set[String]]("The ivy configurations we wish a report of.")
    val licenseSelection = settingKey[Seq[LicenseCategory]]("A priority-order list mechanism we can use to select licenses for projects that have more than one.")
    val licenseReportMakeHeader = settingKey[TargetLanguage => String]("A mechanism of generating the header for the license report file.")
    val licenseReportTypes = settingKey[Seq[TargetLanguage]]("The license report files to generate.")
  }
  import autoImports._  
  
  override def projectSettings: Seq[Setting[_]] =
    Seq(
      licenseSelection := LicenseCategory.all,
      licenseConfigurations := Set("compile", "test"),
      licenseReportTitle := s"License Report for - ${projectID.value}",
      makeLicenseReport := {
        val ignore = update.value
        license.LicenseReport.makeReport(ivyModule.value, licenseConfigurations.value, licenseSelection.value, streams.value.log)
      },
      // TODO - A default header.
      licenseReportMakeHeader := (language => ""),
      // TODO - Maybe we need a general purpose reporting directory
      licenseReportDir := target.value / "license-reports",
      licenseReportTypes := Seq(MarkDown, Html),
      dumpLicenseReport := {
        val report = makeLicenseReport.value
        val dir = licenseReportDir.value
        // TODO - Configurable language (markdown/html) rather than both always
        val reportTypes = licenseReportTypes.value
        val config = LicenseReportConfiguration(licenseReportTitle.value, reportTypes, licenseReportMakeHeader.value, dir)
        LicenseReport.dumpLicenseReport(report, config)
        // Now let's just report on one of them.
        
        dir
      }

    )
}
