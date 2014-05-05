# sbt-license-report

This plugin will allow you to report the licenses used in your projects.  It requires
sbt 0.13.5+

## Installation

Create a file in your project called `project/license.sbt` with the following contents:

    addSbtPlugin("com.typesafe.sbt" % "sbt-license-report" % "0.1")

## Usage

     > dumpLicenseReport

This dumps a report of all the licenses used in a project, with an attempt to organize them.  These
are dumped, by default, to the `target/license-reports` directory.
# Releasing

A three step process


  > git tag -u <pgp key> v<version>
  > sbt
  sbt> publishSigned


# License

This software is under the [Apache 2.0 License](http://www.apache.org/licenses/LICENSE-2.0.html).
