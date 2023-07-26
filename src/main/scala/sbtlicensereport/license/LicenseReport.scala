package sbtlicensereport
package license

import org.apache.ivy.core.report.ResolveReport
import org.apache.ivy.core.resolve.IvyNode
import sbt._
import scala.util.control.Exception._
import sbtlicensereport.SbtCompat._

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

case class LicenseReport(licenses: Seq[DepLicense], orig: ResolveReport) {
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
        print(language.documentEnd)
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

  private def getModuleInfo(dep: IvyNode): DepModuleInfo = {
    // TODO - null handling...
    DepModuleInfo(dep.getModuleId.getOrganisation, dep.getModuleId.getName, dep.getModuleRevision.getId.getRevision)
  }

  def makeReport(
      module: IvySbt#Module,
      configs: Set[String],
      licenseSelection: Seq[LicenseCategory],
      overrides: DepModuleInfo => Option[LicenseInfo],
      exclusions: DepModuleInfo => Option[Boolean],
      originatingModule: DepModuleInfo,
      log: Logger
  ): LicenseReport = {
    val (report, err) = resolve(module, log)
    err foreach (x => throw x) // Bail on error
    makeReportImpl(report, configs, licenseSelection, overrides, exclusions, originatingModule, log)
  }

  /**
   * given a set of categories and an array of ivy-resolved licenses, pick the first one from our list, or default to
   * 'none specified'.
   */
  private def pickLicense(
      categories: Seq[LicenseCategory]
  )(licenses: Array[org.apache.ivy.core.module.descriptor.License]): LicenseInfo = {
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
  private def pickLicenseForDep(
      dep: IvyNode,
      configs: Set[String],
      categories: Seq[LicenseCategory],
      originatingModule: DepModuleInfo
  ): Option[DepLicense] =
    for {
      d <- Option(dep)
      cs = dep.getRootModuleConfigurations.toSet
      filteredConfigs = if (cs.isEmpty) cs else cs.filter(configs)
      if !filteredConfigs.isEmpty
      if !filteredConfigs.forall(d.isEvicted)
      desc <- Option(dep.getDescriptor)
      licenses = Option(desc.getLicenses)
        .filterNot(_.isEmpty)
        .getOrElse(Array(new org.apache.ivy.core.module.descriptor.License("none specified", "none specified")))
      homepage = Option
        .apply(desc.getHomePage)
        .flatMap(loc =>
          nonFatalCatch[Option[URL]]
            .withApply((_: Throwable) => Option.empty[URL])
            .apply(Some(url(loc)))
        )
      // TODO - grab configurations.
    } yield DepLicense(
      getModuleInfo(dep),
      pickLicense(categories)(licenses),
      homepage,
      filteredConfigs,
      originatingModule
    )

  private def getLicenses(
      report: ResolveReport,
      configs: Set[String] = Set.empty,
      categories: Seq[LicenseCategory] = LicenseCategory.all,
      originatingModule: DepModuleInfo
  ): Seq[DepLicense] = {
    import collection.JavaConverters._
    for {
      dep <- report.getDependencies.asInstanceOf[java.util.List[IvyNode]].asScala
      report <- pickLicenseForDep(dep, configs, categories, originatingModule)
    } yield report
  }

  private def makeReportImpl(
      report: ResolveReport,
      configs: Set[String],
      categories: Seq[LicenseCategory],
      overrides: DepModuleInfo => Option[LicenseInfo],
      exclusions: DepModuleInfo => Option[Boolean],
      originatingModule: DepModuleInfo,
      log: Logger
  ): LicenseReport = {
    val licenses = getLicenses(report, configs, categories, originatingModule) filterNot { dep =>
      exclusions(dep.module).getOrElse(false)
    } map { l =>
      overrides(l.module) match {
        case Some(o) => l.copy(license = o)
        case _       => l
      }
    }
    // TODO - Filter for a real report...
    LicenseReport(licenses, report)
  }

  // Hacky way to go re-lookup the report
  private def resolve(module: IvySbt#Module, log: Logger): (ResolveReport, Option[ResolveException]) =
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
