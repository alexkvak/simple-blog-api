//enablePlugins(JavaAppPackaging)

name         := "personal-blog-api"

organization := "com.aks-studio"

version      := "0.1"

scalaVersion := "2.11.7"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

libraryDependencies ++= {
  val akkaV       = "2.3.12"
  val akkaStreamV = "1.0"
  val scalaTestV  = "2.2.1"
  val slickV      = "3.1.0"
  Seq(
    "ch.qos.logback"    % "logback-classic"                    % "1.1.3",
    "com.github.nscala-time" %% "nscala-time"                  % "1.8.0",
    "com.typesafe.akka" %% "akka-actor"                        % akkaV,
    "com.typesafe.akka" %% "akka-stream-experimental"          % akkaStreamV,
    "com.typesafe.akka" %% "akka-http-core-experimental"       % akkaStreamV,
    "com.typesafe.akka" %% "akka-http-experimental"            % akkaStreamV,
    "com.typesafe.akka" %% "akka-http-spray-json-experimental" % akkaStreamV,
    "com.typesafe.akka" %% "akka-http-testkit-experimental"    % akkaStreamV,
    "com.typesafe.slick" %% "slick" % slickV,
    "com.typesafe.slick" %% "slick-hikaricp" % slickV,
    "org.xerial"        % "sqlite-jdbc"                        % "3.8.11.2"
  )
}

//Revolver.settings
