package com.typesafe.sbt
package license

// TODO - What do we mean by viral?
case class LicenseCategory(name: String, viral: Boolean, synonyms: Seq[String] = Nil) {
  def unapply(license: License): Boolean = {
    val names = name +: synonyms
    names exists { n =>
      license.name.toLowerCase contains n.toLowerCase
    }
  }

}
object LicenseCategory {
  val BSD = LicenseCategory("BSD", false)
  val Apache = LicenseCategory("Apache", false, Seq("asf"))
  val LGPL = LicenseCategory("LGPL", false, Seq("lesser general public license"))
  object GPLClasspath extends LicenseCategory("GPL with Classpath Extension", false) {
    override def unapply(license: License): Boolean = {
      val name = license.name.toLowerCase
      ((name.contains("gpl") || name.contains("general public license")) &&
        name.contains("classpath"))
    }
  }
  val GPL = LicenseCategory("GPL", true, Seq("general public license"))
  val Mozilla = LicenseCategory("Mozilla", false, Seq("mpl"))
  val MIT = LicenseCategory("MIT", false)
  val CommonPublic = LicenseCategory("Common Public License", false)
  val PublicDomain = LicenseCategory("Public Domain", false)
  val NoneSpecified = LicenseCategory("none specified", true)

  val all: Seq[LicenseCategory] =
    Seq(BSD, Apache, LGPL, GPLClasspath, GPL, Mozilla, MIT, PublicDomain, CommonPublic, NoneSpecified)

  def find(l: License): LicenseCategory =
    all.find(_.unapply(l)).getOrElse(NoneSpecified)
}
