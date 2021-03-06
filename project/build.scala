import sbt._
import Keys._
import xml.Group

object HookupBuild extends Build {
  
  val projectSettings = Defaults.defaultSettings ++ Seq(
    organization := "io.backchat.hookup",
    name := "hookup",
    version := "0.2.3-SNAPSHOT",
    scalaVersion := "2.10.1",
    //scalaVersion := "2.10",
    //crossScalaVersions := Seq("2.9.1", "2.9.1-1", "2.9.2"),
    compileOrder := CompileOrder.ScalaThenJava,
    libraryDependencies ++= Seq(
      "io.netty" % "netty" % "3.5.0.Final",
      //"org.scala-tools.time" % "time_2.9.1" % "0.5",
      //"org.scalaj" % "scalaj-time_2.10.1" % "0.7-SNAPSHOT",
      "org.scalaj" % "scalaj-time_2.10.0-M7" % "0.6",
      // "org.scala-tools.time" %% "time" % "0.7-SNAPSHOT",
      //2.10.0-M7"
      "net.liftweb" % "lift-json_2.10" % "2.5-RC4" % "compile",
      "commons-io" % "commons-io" % "2.1",
      "com.typesafe.akka" %% "akka-actor" % "2.1.2" % "compile",
      "com.typesafe.akka" %% "akka-testkit" % "2.1.2" % "test",
      // "org.specs2" %% "specs2" % "1.14" % "test",
      "org.specs2" %% "specs2" % "1.12.3" % "test",
      "junit" % "junit" % "4.10" % "test",
      "joda-time" % "joda-time" % "2.1"
    ),
    //resolvers += "snapshots"     at "http://oss.sonatype.org/content/repositories/snapshots",
    resolvers ++= Seq(
      "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases",
      "scalatools-releases" at "http://scala-tools.org/repo-releases/",
      // "mvn central" at "http://mvnrepository.com/artifact/",
      "mvn repo" at "http://repo1.maven.org/maven2/", 
      // org/scalaj/scalaj-time_2.10.0-M7/0.6/scalaj-time_2.10.0-M7-0.6.jar
      "son-snapshots" at "http://oss.sonatype.org/content/repositories/snapshots",
      "son-releases"  at "http://oss.sonatype.org/content/repositories/releases"),
    scalacOptions ++= Seq(
      "-optimize",
      "-deprecation",
      "-unchecked",
      "-Xcheckinit",
      "-encoding", "utf8"),
    parallelExecution in Test := false,
    testOptions := Seq(Tests.Argument("console", "junitxml")),
    testOptions <+= (crossTarget, resourceDirectory in Test) map { (ct, rt) =>
      Tests.Setup { () =>
        System.setProperty("specs2.junit.outDir", new File(ct, "specs-reports").getAbsolutePath)
        System.setProperty("java.util.logging.config.file", new File(rt, "logging.properties").getAbsolutePath)
      }
    },
    javacOptions ++= Seq("-Xlint:unchecked", "-source", "1.6", "-target", "1.6"),
    // scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature", "-language:implicitConversions")
    scalacOptions ++= Seq("-language:implicitConversions")
    /*
    externalResolvers <<= resolvers map { rs =>
      Resolver.withDefaultResolvers(rs, mavenCentral = true, scalaTools = false)
    }*/
  )

  lazy val root =
    Project("hookup", file("."), settings = projectSettings)



}
