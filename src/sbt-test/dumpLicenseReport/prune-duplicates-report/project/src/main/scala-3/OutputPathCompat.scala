import sbt._
import sbt.Keys._

object OutputPathCompat {
  def settings: Seq[Setting[_]] =
    List(outputPath := thisProject.value.id)
}
