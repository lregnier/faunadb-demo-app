import Settings._

lazy val `faunadb-demo-app` =
  project
    .in(file("."))
    .settings(settings)
    .settings(libraryDependencies ++= Dependencies.faunaDbDemoApp)
