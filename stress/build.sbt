name := "stress"

version := "1.0"

scalaVersion := "2.11.8"

libraryDependencies += "io.gatling.highcharts" % "gatling-charts-highcharts" % "2.1.7" % "test"
libraryDependencies += "io.gatling" % "gatling-test-framework" % "2.1.7" % "test"
libraryDependencies += "io.paradoxical" % "cassieq-client" % "0.11" % "test"