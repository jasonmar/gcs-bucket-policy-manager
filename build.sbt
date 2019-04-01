name := "gcs-bucket-policy-manager"

scalaVersion := "2.11.8"

organization := "com.google.cloud"

version := "0.1.0-SNAPSHOT"

val exGuava = ExclusionRule(organization = "com.google.guava")

libraryDependencies ++= Seq(
  "com.google.cloud" % "google-cloud-storage" % "1.66.0" excludeAll exGuava,
  "com.google.guava" % "guava" % "27.0.1-jre",
  "com.fasterxml.jackson.dataformat" % "jackson-dataformat-xml" % "2.9.8",
  "com.github.scopt" %% "scopt" % "3.7.1",
  "org.scalatest" %% "scalatest" % "3.0.5" % Test
) 

mainClass in assembly := Some("com.google.cloud.util.bpm.BPM")

assemblyJarName in assembly := "bpm.jar"

// Don't run tests during assembly
test in assembly := Seq()
