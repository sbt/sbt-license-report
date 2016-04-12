package com.typesafe.sbt
package license

import java.io.File
import sbt.{ IO, IvyRetrieve, IvySbt, Logger, ResolveException, Using }
import org.apache.ivy.core.report.ResolveReport
import org.apache.ivy.core.resolve.IvyNode

case class DepModuleInfo(organization: String, name: String, version: String) {
  override def toString = s"${organization} # ${name} # ${version}"
}
case class DepLicense(module: DepModuleInfo, license: LicenseInfo, configs: Set[String]) {
  override def toString = s"$module on $license in ${configs.mkString("(", ",", ")")}"
}

case class LicenseReport(licenses: Seq[DepLicense], orig: ResolveReport) {
  override def toString = s"""|## License Report ##
                              |${licenses.mkString("\t", "\n\t", "\n")}
                              |""".stripMargin
}

object LicenseReport {

  def withPrintableFile(file: File)(f: (Any => Unit) => Unit): Unit = {
    IO.createDirectory(file.getParentFile)
    Using.fileWriter(java.nio.charset.Charset.defaultCharset, false)(file) { writer =>
      def println(msg: Any): Unit = {
        writer.write(msg.toString)
        //writer.newLine()
      }
      f(println _)
    }
  }

  def dumpLicenseReport(report: LicenseReport, config: LicenseReportConfiguration): Unit = {
    import config._
    val ordered = report.licenses.filter(l => licenseFilter(l.license.category)) sortWith {
      case (l, r) =>
        if (l.license.category != r.license.category) l.license.category.name < r.license.category.name
        else {
          if (l.license.name != r.license.name) l.license.name < r.license.name
          else {
            l.module.toString < r.module.toString
          }
        }
    }
    // TODO - Make one of these for every configuration?
    for (language <- languages) {
      val reportFile = new File(config.reportDir, s"${title}.${language.ext}")
      withPrintableFile(reportFile) { print =>
        print(language.documentStart(title, reportStyleRules))
        print(makeHeader(language))
        print(language.tableHeader("Category", "License", "Dependency", "Notes"))
        for (dep <- ordered) {
          val licenseLink = language.createHyperLink(dep.license.url, dep.license.name)
          print(language.tableRow(
            dep.license.category.name,
            licenseLink,
            dep.module.toString,
            notes(dep.module) getOrElse ""))
        }
        print(language.tableEnd)
        print(language.documentEnd)
      }
    }
  }
  def getModuleInfo(dep: IvyNode): DepModuleInfo = {
    // TODO - null handling...
    DepModuleInfo(dep.getModuleId.getOrganisation, dep.getModuleId.getName, dep.getModuleRevision.getId.getRevision)
  }

  def makeReport(module: IvySbt#Module, configs: Set[String], licenseSelection: Seq[LicenseCategory], overrides: DepModuleInfo => Option[LicenseInfo], log: Logger): LicenseReport = {
    val (report, err) = resolve(module, log)
    err foreach (x => throw x) // Bail on error
    makeReportImpl(report, configs, licenseSelection, overrides, log)
  }
  /**
   * given a set of categories and an array of ivy-resolved licenses, pick the first one from our list, or
   *  default to 'none specified'.
   */
  def pickLicense(categories: Seq[LicenseCategory])(licenses: Array[org.apache.ivy.core.module.descriptor.License]): LicenseInfo = {
    if (licenses.isEmpty) {
      return LicenseInfo(LicenseCategory.NoneSpecified, "", "")
    }
    // We look for a license matching the category in the order they are defined.
    // i.e. the user selects the licenses they prefer to use, in order, if an artifact is dual-licensed (or more)
    for (category <- categories) {
      for (license <- licenses) {
        if (category.unapply(license.getName)) {
          return LicenseInfo(category, license.getName, license.getUrl)
        }
      }
    }
    val license = licenses(0)
    LicenseInfo(LicenseCategory.Unrecognized, license.getName, license.getUrl)
  }
  /** Picks a single license (or none) for this dependency. */
  def pickLicenseForDep(dep: IvyNode, configs: Set[String], categories: Seq[LicenseCategory]): Option[DepLicense] =
    for {
      d <- Option(dep)
      cs = dep.getRootModuleConfigurations.toSet
      filteredConfigs = if (cs.isEmpty) cs else cs.filter(configs)
      if !filteredConfigs.isEmpty
      if !filteredConfigs.forall(d.isEvicted)
      desc <- Option(dep.getDescriptor)
      licenses = Option(desc.getLicenses).filterNot(_.isEmpty).getOrElse(Array(new org.apache.ivy.core.module.descriptor.License("none specified", "none specified")))
      // TODO - grab configurations.
    } yield DepLicense(getModuleInfo(dep), pickLicense(categories)(licenses), filteredConfigs)

  def getLicenses(report: ResolveReport, configs: Set[String] = Set.empty, categories: Seq[LicenseCategory] = LicenseCategory.all): Seq[DepLicense] = {
    import collection.JavaConverters._
    for {
      dep <- report.getDependencies.asInstanceOf[java.util.List[IvyNode]].asScala
      report <- pickLicenseForDep(dep, configs, categories)
    } yield report
  }

  def makeReportImpl(report: ResolveReport, configs: Set[String], categories: Seq[LicenseCategory], overrides: DepModuleInfo => Option[LicenseInfo], log: Logger): LicenseReport = {
    import collection.JavaConverters._
    val licenses = getLicenses(report, configs, categories) map { l =>
      overrides(l.module) match {
        case Some(o) => l.copy(license = o)
        case _ => l
      }
    }
    // TODO - Filter for a real report...
    LicenseReport(licenses, report)
  }

  // Hacky way to go re-lookup the report
  def resolve(module: IvySbt#Module, log: Logger): (ResolveReport, Option[ResolveException]) =
    module.withModule(log) { (ivy, desc, default) =>
      import org.apache.ivy.core.resolve.ResolveOptions
      val resolveOptions = new ResolveOptions
      val resolveId = ResolveOptions.getDefaultResolveId(desc)
      resolveOptions.setResolveId(resolveId)
      import org.apache.ivy.core.LogOptions.LOG_QUIET
      resolveOptions.setLog(LOG_QUIET)
      val resolveReport = ivy.resolve(desc, resolveOptions)
      val err =
        if (resolveReport.hasError) {
          val messages = resolveReport.getAllProblemMessages.toArray.map(_.toString).distinct
          val failed = resolveReport.getUnresolvedDependencies.map(node => IvyRetrieve.toModuleID(node.getId))
          Some(new ResolveException(messages, failed))
        } else None
      (resolveReport, err)
    }
}
