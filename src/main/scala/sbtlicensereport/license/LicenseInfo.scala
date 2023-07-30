package sbtlicensereport
package license

/**
 * Container for license category, name, and source url
 */
case class LicenseInfo(category: LicenseCategory, name: String, url: String) {
  override def toString = category.name
}

/** Companion object containing several common usage licenses. */
object LicenseInfo {
  val GPL2 = LicenseInfo(
    LicenseCategory.GPL,
    "GNU General Public License (GPL), Version 2.0",
    "http://opensource.org/licenses/GPL-2.0"
  )
  val GPL3 = LicenseInfo(
    LicenseCategory.GPL,
    "GNU General Public License (GPL), Version 3.0",
    "http://opensource.org/licenses/GPL-3.0"
  )
  val LGPL2 = LicenseInfo(
    LicenseCategory.LGPL,
    "GNU Library or \"Lesser\" General Public License, Version 2.1 (LGPL-2.1)",
    "http://opensource.org/licenses/LGPL-2.1"
  )
  val LGPL3 = LicenseInfo(
    LicenseCategory.LGPL,
    "GNU Library or \"Lesser\" General Public License, Version 3.0 (LGPL-3.0)",
    "http://opensource.org/licenses/LGPL-3.0"
  )
  val CDDL = LicenseInfo(
    LicenseCategory.CDDL,
    "Common Development and Distribution License (CDDL-1.0)",
    "http://opensource.org/licenses/CDDL-1.0"
  )
  val CDDL_GPL = LicenseInfo(
    LicenseCategory.CDDL,
    "CDDL + GPLv2 License",
    "https://glassfish.dev.java.net/nonav/public/CDDL+GPL.html"
  )
  val APACHE2 = LicenseInfo(
    LicenseCategory.Apache,
    "The Apache Software License, Version 2.0",
    "http://www.apache.org/licenses/LICENSE-2.0.txt"
  )
  val BSD2 = LicenseInfo(LicenseCategory.BSD, "BSD 2-Clause", "http://opensource.org/licenses/BSD-2-Clause")
  val BSD3 = LicenseInfo(LicenseCategory.BSD, "BSD 3-Clause", "http://opensource.org/licenses/BSD-3-Clause")
  val MIT = LicenseInfo(LicenseCategory.MIT, "MIT License", "http://opensource.org/licenses/MIT")
  val EPL = LicenseInfo(LicenseCategory.EPL, "Eclipse Public License", "https://www.eclipse.org/legal/epl-v10.html")
  val EDL = LicenseInfo(
    LicenseCategory.BSD,
    "Eclipse Distribution License 1.0",
    "http://www.eclipse.org/org/documents/edl-v10.php"
  )
  val MPL = LicenseInfo(LicenseCategory.Mozilla, "Mozilla Public License 2.0", "https://www.mozilla.org/MPL/2.0/")
  val BouncyCastle =
    LicenseInfo(LicenseCategory.BouncyCastle, "Bouncy Castle Licence", "https://www.bouncycastle.org/license.html")
  val JSON =
    LicenseInfo(LicenseCategory.JSON, "The JSON License", "https://json.org/license.html")
  val Go =
    LicenseInfo(LicenseCategory.BSD, "The Go License", "https://golang.org/LICENSE")
  val IBM_IPLA = LicenseInfo(
    LicenseCategory.IBM_IPLA,
    "IBM International Program License Agreement",
    "https://www.ibm.com/support/customer/csol/terms/?id=i125-3301&lc=en#detail-document"
  )
  val Unicode =
    LicenseInfo(
      LicenseCategory.Unicode,
      "Unicode/ICU License",
      "https://raw.githubusercontent.com/unicode-org/icu/master/LICENSE"
    )
}
