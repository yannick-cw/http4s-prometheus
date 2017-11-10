sonatypeProfileName := "io.github.yannick-cw"

publishMavenStyle := true

publishArtifact in Test := false

pomIncludeRepository := { _ =>
  false
}

publishTo := Some(
  if (isSnapshot.value)
    Opts.resolver.sonatypeSnapshots
  else
    Opts.resolver.sonatypeStaging
)

pomExtra in Global := {
  <url>https://github.com/yannick-cw/http4s-prometheus</url>
    <licenses>
      <license>
        <name>MIT</name>
        <url>https://opensource.org/licenses/MIT</url>
      </license>
    </licenses>
    <scm>
      <connection>scm:git:github.com/yannick-cw/http4s-prometheus</connection>
      <developerConnection>scm:git:git@github.com:yannick-cw/http4s-prometheus</developerConnection>
      <url>github.com/yannick-cw/http4s-prometheus</url>
    </scm>
    <developers>
      <developer>
        <id>7374</id>
        <name>Yannick Gladow</name>
        <url>https://github.com/yannick-cw</url>
      </developer>
    </developers>
}
