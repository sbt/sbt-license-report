package sbtlicensereport
package license

import sbt._

case class LicenseReportConfiguration(
    title: String,
    languages: Seq[TargetLanguage],
    makeHeader: TargetLanguage => String,
    notes: DepModuleInfo => Option[String],
    licenseFilter: LicenseCategory => Boolean,
    reportDir: File,
    reportStyleRules: Option[String] = None,
    licenseReportColumns: Seq[Column]
)
