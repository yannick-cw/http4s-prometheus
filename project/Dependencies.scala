import sbt._

object Dependencies {
  val http4sVersion    = "0.18.0-M5"
  val scalatestVersion = "3.0.3"
  private val prometheusVersion = "0.1.0"

  lazy val scalaTest = "org.scalatest" %% "scalatest"  % scalatestVersion
  lazy val http4s    = "org.http4s"    %% "http4s-dsl" % http4sVersion
  lazy val prometheus = Seq(
    "io.prometheus" % "simpleclient" % prometheusVersion,
    "io.prometheus" % "simpleclient_common" % prometheusVersion
  )

}
