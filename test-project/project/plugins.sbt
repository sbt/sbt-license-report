lazy val root = Project("plugins", file(".")) dependsOn (packager)

lazy val packager = RootProject(file("..").getAbsoluteFile.toURI)
