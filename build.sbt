import sbt.Keys.libraryDependencies

ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.12"

resolvers += "Akka library repository".at("https://repo.akka.io/maven")

val scalaTestVersion = "3.2.17"
val typeSafeConfigVersion = "1.4.2"
val logbackVersion = "1.2.10"
val netBuddyVersion = "1.14.4"
val scalaParCollVersion = "1.0.4"
val akkaHttpVersion = "10.6.0"
val akkaVersion = "2.9.0"


lazy val commonDependencies = Seq(
  "org.scala-lang.modules" %% "scala-parallel-collections" % scalaParCollVersion,
  "org.scalatest" %% "scalatest" % scalaTestVersion % Test,
  "org.scalatestplus" %% "mockito-4-2" % "3.2.12.0-RC2" % Test,
  "com.typesafe" % "config" % typeSafeConfigVersion,
  "ch.qos.logback" % "logback-classic" % logbackVersion,
  "net.bytebuddy" % "byte-buddy" % netBuddyVersion
)

libraryDependencies ++= commonDependencies

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion
)

libraryDependencies += "org.yaml" % "snakeyaml" % "2.0"

libraryDependencies += "io.circe" %% "circe-parser" % "0.14.1"
libraryDependencies += "io.circe" %% "circe-core" % "0.14.1"
libraryDependencies += "io.circe" %% "circe-generic" % "0.14.1"

libraryDependencies += "org.apache.hadoop" % "hadoop-common" % "3.3.6"
libraryDependencies += "org.apache.hadoop" % "hadoop-mapreduce-client-core" % "3.3.6"
libraryDependencies += "org.apache.hadoop" % "hadoop-mapreduce-client-jobclient" % "3.3.6"

lazy val root = (project in file("."))
  .settings(
    name := "cc3"
  )

scalacOptions += "-Ytasty-reader"

compileOrder := CompileOrder.JavaThenScala
test / fork := true
run / fork := true

val jarName = "p3.jar"
assembly/assemblyJarName := jarName

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case x => MergeStrategy.first
}
