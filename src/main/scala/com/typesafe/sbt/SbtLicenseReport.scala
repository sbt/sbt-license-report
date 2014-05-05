package com.typesafe.sbt

import sbt._
import Keys._
import license._



/** A plugin which enables reporting on licensing used within a project. */
object SbtLicenseReport extends AutoPlugin {
  override def requires: Plugins = plugins.IvyPlugin
  override def trigger = allRequirements
  
  object autoImport {
    val updateLicenses = taskKey[LicenseReport]("Construct a report of used licenses in a project.")
    val licenseReportConfigurations = taskKey[Seq[LicenseReportConfiguration]]("Configuration for each license report we're generating.")
    val dumpLicenseReport = taskKey[File]("Dumps a report file of the license report (using the target language).")
    val licenseReportDir = settingKey[File]("The location where we'll write the license reports.")
    val licenseReportTitle = settingKey[String]("The name of the license report.")
    val licenseConfigurations = settingKey[Set[String]]("The ivy configurations we wish a report of.")
    val licenseSelection = settingKey[Seq[LicenseCategory]]("A priority-order list mechanism we can use to select licenses for projects that have more than one.")
    val licenseReportMakeHeader = settingKey[TargetLanguage => String]("A mechanism of generating the header for the license report file.")
    val licenseReportTypes = settingKey[Seq[TargetLanguage]]("The license report files to generate.")
    val licenseReportNotes = settingKey[PartialFunction[DepModuleInfo, String]]("A partial functoin that will obtain license report notes based on module.")
    val licenseOverrides = settingKey[PartialFunction[DepModuleInfo, LicenseInfo]]("A list of license overrides for artifacts with bad infomration on maven.")
    val licenseFilter = settingKey[LicenseCategory => Boolean]("Configuration for what licenses to include in the report, by default.")
  }
  import autoImport._
  
  override def projectSettings: Seq[Setting[_]] =
    Seq(
      licenseSelection := LicenseCategory.all,
      licenseConfigurations := Set("compile", "test"),
      licenseReportTitle := s"${projectID.value}-licenses",
      // Here we use an empty partial function
      licenseReportNotes := PartialFunction.empty,
      licenseOverrides := PartialFunction.empty,
      licenseFilter := TypeFunctions.const(true),
      updateLicenses := {
        val ignore = update.value
        val overrides = licenseOverrides.value.lift
        license.LicenseReport.makeReport(ivyModule.value, licenseConfigurations.value, licenseSelection.value, overrides, streams.value.log)
      },
      // TODO - A default header.
      licenseReportMakeHeader := (language => language.header1(licenseReportTitle.value)),
      // TODO - Maybe we need a general purpose reporting directory
      licenseReportDir := target.value / "license-reports",
      licenseReportTypes := Seq(MarkDown, Html),
      licenseReportConfigurations := {
        val dir = licenseReportDir.value
        // TODO - Configurable language (markdown/html) rather than both always
        val reportTypes = licenseReportTypes.value
        val notesLookup = licenseReportNotes.value.lift
        val config = LicenseReportConfiguration(licenseReportTitle.value, reportTypes, licenseReportMakeHeader.value, notesLookup, licenseFilter.value, dir)
        Seq(config)
      },
      dumpLicenseReport := {
        val report = updateLicenses.value
        val dir = licenseReportDir.value
        for(config <- licenseReportConfigurations.value)
          LicenseReport.dumpLicenseReport(report, config)        
        dir
      }

    )
}
