package sbtlicensereport
package license

// TODO - What do we mean by viral?
case class LicenseCategory(name: String, synonyms: Seq[String] = Nil) {
  def unapply(license: String): Boolean = {
    val names = name +: synonyms
    names exists { n =>
      license.toLowerCase contains n.toLowerCase
    }
  }

}
object LicenseCategory {
  val BSD = LicenseCategory("BSD")
  val Apache = LicenseCategory("Apache", Seq("asf", "ALv2"))
  val LGPL = LicenseCategory("LGPL", Seq("lesser general public license"))
  object GPLClasspath extends LicenseCategory("GPL with Classpath Extension") {
    override def unapply(license: String): Boolean = {
      val name = license.toLowerCase
      (name.contains("gpl") || name.contains("general public license")) &&
      name.contains("classpath")
    }
  }
  val GPL = LicenseCategory("GPL", Seq("general public license"))
  val Mozilla = LicenseCategory("Mozilla", Seq("mpl"))
  val MIT = LicenseCategory("MIT")
  val CommonPublic = LicenseCategory("Common Public License", Seq("cpl", "common public"))
  val PublicDomain = LicenseCategory("Public Domain")
  val CC0 = LicenseCategory("CC0", Seq("Creative Commons Zero"))
  val EPL = LicenseCategory("EPL", Seq("Eclipse Public License"))
  val CDDL = LicenseCategory("CDDL", Seq("Common Development and Distribution"))
  val BouncyCastle = LicenseCategory("Bouncy Castle License", Seq("Bouncy Castle"))
  val Proprietary = LicenseCategory("Proprietary")
  val NoneSpecified = LicenseCategory("none specified")
  val Unrecognized = LicenseCategory("unrecognized")

  val all: Seq[LicenseCategory] =
    Seq(
      PublicDomain,
      CommonPublic,
      CC0,
      Mozilla,
      MIT,
      BSD,
      Apache,
      LGPL,
      GPLClasspath,
      GPL,
      EPL,
      CDDL,
      BouncyCastle,
      Proprietary
    )

  def find(licenses: Seq[LicenseCategory])(licenseName: String): Option[LicenseCategory] =
    licenses.find(_.unapply(licenseName))
}
