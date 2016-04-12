package com.typesafe.sbt
package license

import java.io.File

case class LicenseReportConfiguration(
  title: String,
  languages: Seq[TargetLanguage],
  makeHeader: TargetLanguage => String,
  notes: DepModuleInfo => Option[String],
  licenseFilter: LicenseCategory => Boolean,
  reportDir: File,
  reportStyleRules: Option[String] = None)
