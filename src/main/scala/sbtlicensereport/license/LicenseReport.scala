package sbtlicensereport
package license

import java.net.{ MalformedURLException, URISyntaxException }
import java.io.File
import scala.util.control.NonFatal
import scala.xml.{ Elem, XML }
import sbt._
import sbt.io.Using

case class DepModuleInfo(organization: String, name: String, version: String) {
  override def toString = s"${organization} # ${name} # ${version}"
}
case class DepLicense(
    module: DepModuleInfo,
    license: LicenseInfo,
    homepage: Option[URL],
    configs: Set[String],
    originatingModule: DepModuleInfo
) {
  override def toString =
    s"$module ${homepage.map(url => s" from $url")} on $license in ${configs.mkString("(", ",", ")")}"
}

object DepLicense {
  implicit val ordering: Ordering[DepLicense] = Ordering.fromLessThan { case (l, r) =>
    if (l.license.category != r.license.category) l.license.category.name < r.license.category.name
    else {
      if (l.license.name != r.license.name) l.license.name < r.license.name
      else {
        l.module.toString < r.module.toString
      }
    }
  }
}

case class LicenseReport(licenses: Seq[DepLicense], orig: UpdateReport) {
  override def toString = s"""|## License Report ##
                              |${licenses.mkString("\t", "\n\t", "\n")}
                              |""".stripMargin
}

object LicenseReport {

  private def withPrintableFile(file: File)(f: (Any => Unit) => Unit): Unit = {
    IO.createDirectory(file.getParentFile)
    Using.fileWriter(java.nio.charset.Charset.defaultCharset, false)(file) { writer =>
      def println(msg: Any): Unit = {
        writer.write(msg.toString)
        // writer.newLine()
      }
      f(println _)
    }
  }

  def dumpLicenseReport(
      reportLicenses: Seq[DepLicense],
      config: LicenseReportConfiguration
  ): Unit = {
    import config._
    val ordered = reportLicenses.filter(l => licenseFilter(l.license.category)).sorted
    // TODO - Make one of these for every configuration?
    for (language <- languages) {
      val reportFile = new File(config.reportDir, s"${title}.${language.ext}")
      withPrintableFile(reportFile) { print =>
        print(language.documentStart(title, reportStyleRules))
        print(makeHeader(language))
        print(language.tableHeader("Notes", config.licenseReportColumns.map(_.columnName): _*))
        val rendered = (ordered map { dep =>
          val notesRendered = notes(dep.module) getOrElse ""
          (
            notesRendered,
            config.licenseReportColumns map (_.render(dep, language))
          )
        }).distinct

        for ((notes, rest) <- rendered) {
          print(language.tableRow(notes, rest: _*))
        }
        print(language.tableEnd)
        print(language.documentEnd())
      }
    }
  }

  def checkLicenses(
      reportLicenses: Seq[DepLicense],
      exclude: PartialFunction[DepModuleInfo, Boolean],
      allowed: Seq[LicenseCategory],
      log: Logger
  ): Unit = {
    val violators =
      reportLicenses.filterNot(dl => exclude.applyOrElse(dl.module, (_: DepModuleInfo) => false)).collect {
        case dep if !allowed.contains(dep.license.category) => dep
      }

    if (violators.nonEmpty) {
      log.error(
        violators.sorted
          .map(v => (v.license, v.module))
          .distinct
          .map { case (license, module) => s"${license.category.name}: ${module.toString}" }
          .mkString("Found non-allowed licenses among the dependencies:\n", "\n", "")
      )
      throw new sbt.MessageOnlyException(s"Found non-allowed licenses!")
    } else {
      log.info("Found only allowed licenses among the dependencies!")
    }
  }

  private def getModuleInfo(dep: ModuleReport): DepModuleInfo = {
    // TODO - null handling...
    DepModuleInfo(dep.module.organization, dep.module.name, dep.module.revision)
  }

  def makeReport(
      updateReport: UpdateReport,
      configs: Set[String],
      licenseSelection: Seq[LicenseCategory],
      overrides: DepModuleInfo => Option[LicenseInfo],
      exclusions: DepModuleInfo => Option[Boolean],
      originatingModule: DepModuleInfo,
      log: Logger
  ): LicenseReport = {
    makeReportImpl(updateReport, configs, licenseSelection, overrides, exclusions, originatingModule, log)
  }

  /**
   * given a set of categories and an array of ivy-resolved licenses, pick the first one from our list, or default to
   * 'none specified'.
   */
  private def pickLicense(
      categories: Seq[LicenseCategory]
  )(licenses: Vector[(String, Option[String])]): LicenseInfo = {
    // Even though the url is optional this field seems to always exist
    val licensesWithUrls = licenses.collect { case (name, Some(url)) => (name, url) }
    if (licensesWithUrls.isEmpty) {
      LicenseInfo(LicenseCategory.NoneSpecified, "", "")
    } else {
      // We look for a license matching the category in the order they are defined.
      // i.e. the user selects the licenses they prefer to use, in order, if an artifact is dual-licensed (or more)
      categories
        .flatMap(category =>
          licensesWithUrls.collectFirst {
            case (name, url) if category.unapply(name) =>
              LicenseInfo(category, name, url)
          }
        )
        .headOption
        .getOrElse {
          val license = licensesWithUrls(0)
          LicenseInfo(LicenseCategory.Unrecognized, license._1, license._2)
        }
    }
  }

  /**
   * Tries to replace property references if present in the pom/ivy.xml files.
   *
   * Example: `https://example.org/\${project.artifactId}`
   *
   * If there are replecements in the files, the reference wil be replaced.
   *
   * `https://example.org/replacement`
   *
   * Otherwise, an empty string will be put in its place:
   *
   * `https://example.org/`
   *
   * @param dep
   *   ModuleReport object that represent the dependency to be checked for property references
   * @param log
   *   logger for errors
   * @return
   *   a ModuleReport object with replaced property references
   */
  private def replacePropertyReferences(dep: ModuleReport, log: Logger): ModuleReport = {

    /**
     * Looks for suitable replacements for the given property references.
     *
     * @param artifactFile
     *   file of the dependency we want to find replacements for
     * @param keys
     *   set of property references
     * @return
     *   Map where each entry has a property reference as key and its replacement as value. Example:
     *   `"\${project.artifactId}" -> "replacement"`
     */
    def findReplacements(artifactFile: File, keys: Set[String]): Map[String, String] = {
      val xmlFile: Option[File] = {
        val pom = new File(artifactFile.getPath.replace(".jar", ".pom"))
        val originals =
          artifactFile.getParentFile.getParentFile.listFiles((_, name) => name.endsWith(".original")).toSeq
        val xmls = artifactFile.getParentFile.getParentFile.listFiles((_, name) => name.endsWith(".xml")).toSeq
        Seq(pom) ++ originals ++ xmls
      }.find(_.exists())

      xmlFile match {
        case Some(xmlFile) =>
          val xml = XML.loadFile(xmlFile)

          def extractValue(xml: Elem, key: String): String = {
            val parts = key.split('.')
            val rootMatches = xml \\ parts.head
            val finalNode = parts.tail.foldLeft(rootMatches.headOption) {
              case (Some(node), label) => (node \ label).headOption
              case _ => {
                log.warn(
                  s"sbt-license-report: unable to find the value for property $key [${dep.module}]"
                )
                None
              }
            }

            finalNode
              .map(_.text.trim)
              .filter(_.nonEmpty)
              .getOrElse {
                log.warn(
                  s"sbt-license-report: unable to find the value for property $key [${dep.module}]"
                )
                ""
              }
          }

          keys.map { key =>
            val value = extractValue(xml, key)
            key -> value
          }.toMap
        case None => Map.empty
      }
    }

    val pattern = "\\$\\{(.*?)}".r
    val licenses = dep.licenses
    val homepage = dep.homepage
    try {
      val hasReferences =
        pattern.findFirstIn(licenses.toString()).isDefined || pattern.findFirstIn(homepage.getOrElse("")).isDefined

      if (hasReferences) {
        dep.artifacts.iterator
          .flatMap { case (_, artifactFile) =>
            val licenseReferences = pattern.findAllMatchIn(licenses.toString()).map(_.group(1)).toSet
            val homepageReferences = pattern.findAllMatchIn(homepage.getOrElse("")).map(_.group(1)).toSet
            val replacements = findReplacements(artifactFile, licenseReferences ++ homepageReferences)

            // Gets a copy of licenses with replaced property references
            val resolvedLicenses = licenses.map(l => {
              def replace(str: String): String =
                pattern.replaceAllIn(str, m => replacements.getOrElse(m.group(1), m.matched))

              l.copy(_1 = replace(l._1), _2 = l._2.map(replace))
            })
            // Gets a copy of homepage with replaced property references
            val resolvedHomepage =
              homepage.map(h => pattern.replaceAllIn(h, m => replacements.getOrElse(m.group(1), m.matched)))

            Some(dep.withLicenses(resolvedLicenses).withHomepage(resolvedHomepage))
          }
          .toSeq
          .headOption
          .getOrElse(dep)
      } else {
        dep
      }
    } catch {
      case NonFatal(_) =>
        log.warn(
          s"sbt-license-report: something went wrong in replacing property references for dependency [${dep.module}]"
        )
        dep
    }
  }

  /** Picks a single license (or none) for this dependency. */
  private def pickLicenseForDep(
      dep: ModuleReport,
      configs: Set[String],
      categories: Seq[LicenseCategory],
      originatingModule: DepModuleInfo,
      log: Logger
  ): Option[DepLicense] = {
    val cs = dep.configurations
    val filteredConfigs = if (cs.isEmpty) cs else cs.filter(configs.map(ConfigRef.apply))

    if (dep.evicted || filteredConfigs.isEmpty)
      None
    else {
      val licenses = dep.licenses
      val homepage = dep.homepage.flatMap(string => {
        try {
          Some(new URI(string).toURL)
        } catch {
          case _: URISyntaxException =>
            log.warn(s"sbt-license-report: dependency [${dep.module}] has malformed homepage url [$string]")
            None
          case _: MalformedURLException =>
            log.warn(s"sbt-license-report: dependency [${dep.module}] has malformed homepage url [$string]")
            None
          case NonFatal(_) =>
            log.warn(s"sbt-license-report: error in extracting homepage url [$string] for dependency [${dep.module}]")
            None
        }
      })
      Some(
        DepLicense(
          getModuleInfo(dep),
          pickLicense(categories)(licenses),
          homepage,
          filteredConfigs.map(_.name).toSet,
          originatingModule
        )
      )
    }
  }

  private def getLicenses(
      report: UpdateReport,
      configs: Set[String] = Set.empty,
      categories: Seq[LicenseCategory] = LicenseCategory.all,
      originatingModule: DepModuleInfo,
      log: Logger
  ): Seq[DepLicense] = {
    for {
      dep <- report.allModuleReports
      report <- pickLicenseForDep(dep, configs, categories, originatingModule, log)
    } yield report
  }

  private def makeReportImpl(
      report: UpdateReport,
      configs: Set[String],
      categories: Seq[LicenseCategory],
      overrides: DepModuleInfo => Option[LicenseInfo],
      exclusions: DepModuleInfo => Option[Boolean],
      originatingModule: DepModuleInfo,
      log: Logger
  ): LicenseReport = {
    val reportWithReplacedPropertyReferences = report.withConfigurations {
      report.configurations.map { c =>
        val newDetails = c.details.map { d =>
          d.withModules(d.modules.map(replacePropertyReferences(_, log)))
        }

        val newModules =
          if (newDetails.nonEmpty) newDetails.flatMap(_.modules)
          else c.modules.map(replacePropertyReferences(_, log))

        c.withDetails(newDetails).withModules(newModules)
      }
    }

    val licenses =
      getLicenses(reportWithReplacedPropertyReferences, configs, categories, originatingModule, log) filterNot { dep =>
        exclusions(dep.module).getOrElse(false)
      } map { depLicense =>
        overrides(depLicense.module) match {
          case Some(licenseInfo) => depLicense.copy(license = licenseInfo)
          case _                 => depLicense
        }
      }
    // TODO - Filter for a real report...
    LicenseReport(licenses, reportWithReplacedPropertyReferences)
  }
}
