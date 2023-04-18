name := "example"

licenseReportTitle := "lreport"

licenseConfigurations := Set(Compile)

licenseSelection := Seq(LicenseCategory.BSD)

licenseReportMakeHeader := { _ => "TestHeader\n" }

licenseReportTypes := Seq(MarkDown)

licenseReportNotes := { case _ => "Default Notes" }

// Adds a new custom report
licenseReportConfigurations +=
  LicenseReportConfiguration(
    "test-config",
    Seq(Html),
    language => language.header1("Testing the configuration"),
    dep => Option("Default notes"),
    category => category == LicenseCategory.BSD,
    licenseReportDir.value,
    None,
    Seq(Column.Category, Column.License, Column.Dependency)
  )

val check = taskKey[Unit]("check the license report.")

check := {
  val reportFile = licenseReportDir.value / "lreport.md"
  val report = IO.read(reportFile)
  // TODO - less lame tests
  try {
    assert(report contains "Default Notes", "Failed to set notes")
    assert(report contains "TestHeader", "Failed to set header")
  } catch {
    case t: Throwable =>
      System.err.println("-- Report --")
      System.err.println(report)
      throw t
  }
  ()
}
