package com.typesafe.sbt
package license

import sbt._
import org.apache.ivy.core.report.ResolveReport
import org.apache.ivy.core.resolve.IvyNode

case class License(name: String, url: String)(val deps: Seq[String]) {
  override def toString = name + " @ " + url
}
case class LicenseReport(licenses: Seq[License], orig: ResolveReport)

object LicenseReport {

  def withPrintableFile(file: File)(f: (Any => Unit) => Unit): Unit = {
    IO.createDirectory(file.getParentFile)
    Using.fileWriter(java.nio.charset.Charset.defaultCharset, false)(file) { writer =>
      def println(msg: Any): Unit = {
        System.out.println(msg)
        writer.write(msg.toString)
        writer.newLine()
      }
      f(println _)
    }
  }

  // Dumps the license report in csv form.
  def dumpCsv(report: LicenseReport, println: Any => Unit): Unit = {
    def dumpLine(cat: LicenseCategory, license: License, dep: String): Unit = {
      println("%s; %s; %s".format(cat.name, license.name, dep))
    }
    println("Cateogory;License;Dependency")
    val categories = report.licenses.groupBy(LicenseCategory.find)
    for {
      (cat, licenses) <- categories
      license <- licenses
      dep <- license.deps
    } dumpLine(cat, license, dep)
  }

  // Dumps a tree-form of the license report.
  def dumpReport(report: LicenseReport, println: Any => Unit): Unit = {
    // License grouping heuristics
    val categories = report.licenses.groupBy(LicenseCategory.find)

    val reverseCategories = categories.toSeq.flatMap { c =>
      c._2.map(_ -> c._1)
    }.toMap
    def dumpLicenses(toDump: Map[LicenseCategory, Seq[License]]): Unit =
      toDump.foreach {
        case (category, licenses) =>
          println("* " + category.name)
          licenses foreach { l =>
            println("  - " + l)
            l.deps foreach { d =>
              println("    + " + d)
            }
          }
      }

    // Now dump the report.
    println("# License Report")
    println("")
    println("The following license categories have been created:")
    println("")
    println("## Non-Viral licenses")
    println("")
    dumpLicenses(categories.filterNot(_._1.viral))
    println("")
    println("## Viral/Unknown licenses")
    println("")
    dumpLicenses(categories.filter(_._1.viral))
    println("")
  }

  def getArtifactNames(dep: IvyNode): Seq[String] = {
    val versionStringOpt =
      for {
        desc <- Option(dep.getDescriptor)
        v <- Option(desc.getRevision)
      } yield "#" + v
    val v = versionStringOpt.getOrElse("")
    val groupOpt =
      for {
        desc <- Option(dep.getDescriptor)
        m <- Option(dep.getModuleId)
      } yield m.getOrganisation + " # "
    val g = groupOpt.getOrElse("")
    dep.getAllArtifacts.map(g + _.getName + v)
  }

  def makeReport(module: IvySbt#Module, log: Logger): LicenseReport = {
    val (report, err) = resolve(module, log)
    err foreach (x => throw x) // Bail on error
    import collection.JavaConverters._

    val licenses =
      for {
        dep <- report.getDependencies.asInstanceOf[java.util.List[IvyNode]].asScala
        if dep != null && dep.getRootModuleConfigurations.contains("runtime")
        desc <- Option(dep.getDescriptor).toSeq
        license <- Option(desc.getLicenses).filterNot(_.isEmpty).getOrElse(Array(new org.apache.ivy.core.module.descriptor.License("none specified", "none specified")))
      } yield License(license.getName, license.getUrl)(getArtifactNames(dep))

    val grouped = for {
      (name, licenses) <- licenses.groupBy(_.name)
      l <- licenses.headOption.toSeq
    } yield License(l.name, l.url)(licenses flatMap (_.deps) distinct)

    LicenseReport(grouped.toSeq, report)
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
      //ResolutionCache.cleanModule(module.getModuleRevisionId, resolveId, ivy.getSettings.getResolutionCacheManager)
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
