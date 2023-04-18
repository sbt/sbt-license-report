package sbtlicensereport

import sbt._
import Keys._
import license._

/** A plugin which enables reporting on licensing used within a project. */
object SbtLicenseReport extends AutoPlugin {
  override def requires: Plugins = plugins.IvyPlugin
  override def trigger = allRequirements

  object autoImportImpl {
    // Types and objects to auto-expose
    type LicenseCategory = sbtlicensereport.license.LicenseCategory
    def LicenseCategory = sbtlicensereport.license.LicenseCategory
    type TargetLanguage = sbtlicensereport.license.TargetLanguage
    type LicenseReportConfiguration = sbtlicensereport.license.LicenseReportConfiguration
    def LicenseReportConfiguration = sbtlicensereport.license.LicenseReportConfiguration
    def Html = sbtlicensereport.license.Html
    def MarkDown = sbtlicensereport.license.MarkDown
    def Csv = sbtlicensereport.license.Csv
    def ConfluenceWikiMarkup = sbtlicensereport.license.ConfluenceWikiMarkup

    // Keys
    val updateLicenses = taskKey[LicenseReport]("Construct a report of used licenses in a build.")
    val licenseReportConfigurations =
      taskKey[Seq[LicenseReportConfiguration]]("Configuration for each license report we're generating.")
    val dumpLicenseReport = taskKey[File]("Dumps a report file of the license report (using the target language).")
    val dumpLicenseReportAggregate = taskKey[File](
      "Dumps a report file against project aggregates of the license report (using the target language) and combines it into a single file."
    )
    val dumpLicenseReportAnyProject = taskKey[File](
      "Dumps a report file against all projects of the license report (using the target language) and combines it into a single file."
    )
    val licenseReportDir = settingKey[File]("The location where we'll write the license reports.")
    val licenseReportStyleRules = settingKey[Option[String]]("The style rules for license report styling.")
    val licenseReportTitle = settingKey[String]("The name of the license report.")
    val licenseConfigurations = settingKey[Set[String]]("The ivy configurations we wish a report of.")
    val licenseSelection = settingKey[Seq[LicenseCategory]](
      "A priority-order list mechanism we can use to select licenses for builds that have more than one."
    )
    val licenseReportMakeHeader =
      settingKey[TargetLanguage => String]("A mechanism of generating the header for the license report file.")
    val licenseReportTypes = settingKey[Seq[TargetLanguage]]("The license report files to generate.")
    val licenseReportNotes = settingKey[PartialFunction[DepModuleInfo, String]](
      "A partial function that will obtain license report notes based on module."
    )
    val licenseOverrides = settingKey[PartialFunction[DepModuleInfo, LicenseInfo]](
      "A list of license overrides for artifacts with bad information on maven."
    )
    val licenseFilter =
      settingKey[LicenseCategory => Boolean]("Configuration for what licenses to include in the report, by default.")
  }
  // Workaround for broken autoImport in sbt 0.13.5
  val autoImport = autoImportImpl
  import autoImport._

  private lazy val aggregateUpdateLicenses = Def.taskDyn {
    updateLicenses.all(ScopeFilter(inAggregates(thisProjectRef.value)))
  }

  private lazy val anyProjectUpdateLicenses = Def.taskDyn {
    updateLicenses.all(ScopeFilter(inAnyProject))
  }

  override def projectSettings: Seq[Setting[_]] =
    Seq(
      licenseSelection := LicenseCategory.all,
      licenseConfigurations := Set("compile", "test"),
      licenseReportTitle := s"${normalizedName.value}-licenses",
      // Here we use an empty partial function
      licenseReportNotes := PartialFunction.empty,
      licenseOverrides := PartialFunction.empty,
      licenseFilter := TypeFunctions.const(true),
      updateLicenses := {
        val ignore = update.value
        val overrides = licenseOverrides.value.lift
        license.LicenseReport.makeReport(
          ivyModule.value,
          licenseConfigurations.value,
          licenseSelection.value,
          overrides,
          streams.value.log
        )
      },
      // TODO - A default header.
      licenseReportMakeHeader := (language => language.header1(licenseReportTitle.value)),
      // TODO - Maybe we need a general purpose reporting directory
      licenseReportDir := target.value / "license-reports",
      licenseReportStyleRules := None,
      licenseReportTypes := Seq(MarkDown, Html, Csv),
      licenseReportConfigurations := {
        val dir = licenseReportDir.value
        val styleRules = licenseReportStyleRules.value
        // TODO - Configurable language (markdown/html/csv) rather than all always
        val reportTypes = licenseReportTypes.value
        val notesLookup = licenseReportNotes.value.lift
        val config = LicenseReportConfiguration(
          licenseReportTitle.value,
          reportTypes,
          licenseReportMakeHeader.value,
          notesLookup,
          licenseFilter.value,
          dir,
          styleRules
        )
        Seq(config)
      },
      dumpLicenseReport := {
        val report = updateLicenses.value
        val dir = licenseReportDir.value
        for (config <- licenseReportConfigurations.value)
          LicenseReport.dumpLicenseReport(report.licenses, config)
        dir
      },
      dumpLicenseReportAggregate := {
        val reports = aggregateUpdateLicenses.value
        val dir = licenseReportDir.value
        for (config <- licenseReportConfigurations.value)
          LicenseReport.dumpLicenseReport(reports.flatMap(_.licenses), config)
        dir
      },
      dumpLicenseReportAnyProject := {
        val reports = anyProjectUpdateLicenses.value
        val dir = licenseReportDir.value
        for (config <- licenseReportConfigurations.value)
          LicenseReport.dumpLicenseReport(reports.flatMap(_.licenses), config)
        dir
      }
    )
}
