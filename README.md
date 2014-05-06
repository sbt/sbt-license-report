# sbt-license-report

This plugin will allow you to report the licenses used in your projects.  It requires
sbt 0.13.5+

## Installation

Create a file in your project called `project/license.sbt` with the following contents:

  addSbtPlugin("com.typesafe.sbt" % "sbt-license-report" % "1.0.0")

## Usage

     > dumpLicenseReport

This dumps a report of all the licenses used in a project, with an attempt to organize them.  These are dumped, by default, to the `target/license-reports` directory.

## Configuration

The license report plugin can be configured to dump any number of reports, but the default report
can be controlled via the following keys:

  // Used to name the report file, and in the HTML/Markdown as the
  // title.
  licenseReportTitle := "Example Report"

  // The ivy configurations we'd like to grab licenses for.
  licenseConfigurations := Set("compile", "provided")

  // The order in which we find/choose licenses.  You can add your own license
  // detection here
  licenseSelection := Seq(LicenseCategory.BSD, LicenseCategory.Apache)

  // Attach notes to modules
  licenseReportNotes := {
    case DepModuleInfo(group, id, version) if group == "example" => "Made up artifact"
  }

  // Override the license information from ivy, if it's non-existent or
  // wrong
  licenseOverrides := {
    case DepModuleInfo("com.jsuereth", _, _) => LicenseCategory.BSD
  }
    
# Releasing

A three step process


  > git tag -u <pgp key> v<version>
  > sbt
  sbt> publishSigned


# License

This software is under the [Apache 2.0 License](http://www.apache.org/licenses/LICENSE-2.0.html).
