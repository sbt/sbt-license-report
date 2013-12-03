# sbt-license-report

This plugin will allow you to report the licenses used in your projects.

## Installation

Create a file in your project called `project/license.sbt` with the following contents:

    addSbtPlugin("com.typesafe.sbt" % "sbt-license-report" % "0.1")

## Usage

The license report can either dump its report directly via the sbt console or into files that you can read later:

    > dumpLicenseReport

This dumps a report of all the licenses used in a project, with an attempt to organize them based on known 'viral' licenses and not.  If a viral license is used,
you must ensure you use a compatible license for your own project and notify downstream consumers.  Here's an example from the [Akka project](http://github.com/akka/akka)

    akka > akka-actor/dumpLicenseReport
    [info] Updating {file:/home/jsuereth/projects/typesafe/akka/}akka-actor...
    [info] Done updating.
    # License Report
    
    The following license categories have been created:
    
    ## Non-Viral licenses
    
    * Apache
      - Apache License, Version 2.0 @ http://www.apache.org/licenses/LICENSE-2.0
        + com.typesafe # config#1.0.2
      - The Apache Software License, Version 2.0 @ http://www.apache.org/licenses/LICENSE-2.0.txt
        + org.fusesource.jansi # jansi#1.4
    * BSD
      - BSD-like @ http://www.scala-lang.org/downloads/license.html
        + org.scala-lang # scala-library#2.10.2
        + org.scala-lang # scala-compiler#2.10.2
        + org.scala-lang # scala-reflect#2.10.2
        + org.scala-lang # jline#2.10.2
      - The BSD License @ http://www.opensource.org/licenses/bsd-license.php
        + org.scala-lang # jline#2.10.2
    
    ## Viral/Unknown licenses
    
    
    [success] Total time: 0 s, completed Dec 3, 2013 10:43:17 AM

The other option is:

    > dumpLicenseReportCsv

This dumps csv files that report license usage for easier consumption in Excell/Spreadsheets.

# Releasing

A four step process

    > git tag -u <pgp key> v<version>
    > sbt
    sbt> publishSigned
    sbt> set scalaVersion in Global := "2.9.2"
    sbt> set sbtVersion in Global := "0.12.4"
    sbt> publishSigned


# License

This software is under the [Apache 2.0 License](http://www.apache.org/licenses/LICENSE-2.0.html).
