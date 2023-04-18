package sbtlicensereport.license

trait Column {
  def columnName: String

  def render(depLicense: DepLicense, language: TargetLanguage): String
}

object Column {
  case object Category extends Column {
    override val columnName: String = "Category"

    override def render(depLicense: DepLicense, language: TargetLanguage): String =
      depLicense.license.category.name
  }

  case object License extends Column {
    override val columnName: String = "License"

    override def render(depLicense: DepLicense, language: TargetLanguage): String =
      language.createHyperLink(depLicense.license.url, depLicense.license.name)
  }

  case object Dependency extends Column {
    override val columnName: String = "Dependency"

    override def render(depLicense: DepLicense, language: TargetLanguage): String =
      depLicense.homepage match {
        case None      => depLicense.module.toString
        case Some(url) => language.createHyperLink(url.toExternalForm, depLicense.module.toString)
      }
  }

  case object Configuration extends Column {
    override val columnName: String = "Maven/Ivy Configurations"

    override def render(depLicense: DepLicense, language: TargetLanguage): String = {
      depLicense.configs.mkString(",")
    }
  }

  case object OriginatingArtifactName extends Column {
    override val columnName: String = "Originating Artifact"

    override def render(depLicense: DepLicense, language: TargetLanguage): String =
      depLicense.originatingModule.name
  }
}
