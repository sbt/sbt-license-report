import sbt._

object OutputPathCompat {
  // sbt 1.x has no `outputPath`; nothing to set.
  def settings: Seq[Setting[_]] =
Nil
}
