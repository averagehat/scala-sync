name := "bleh"

version := "1.0"

//scalaVersion := "2.12.0"
scalaVersion := "2.11.8" // needed for better-files

libraryDependencies += "com.github.samtools" % "htsjdk" % "2.7.0"

libraryDependencies += "com.taskadapter" % "redmine-java-api" % "3.0.0"

libraryDependencies += "org.scalafx" %% "scalafx" % "8.0.102-R11"

libraryDependencies += "com.nrinaudo" %% "kantan.csv" % "0.1.16"

// Automatic type class instances derivation.
libraryDependencies += "com.nrinaudo" %% "kantan.csv-generic" % "0.1.16"

libraryDependencies += "org.scalaz" %% "scalaz-core" % "7.2.8"


// yaml
libraryDependencies += "net.jcazevedo" %% "moultingyaml" % "0.4.0"

// i/o
libraryDependencies += "com.github.pathikrit" %% "better-files" % "2.16.0"

resolvers += Resolver.sonatypeRepo("releases")
libraryDependencies += "com.github.alexarchambault" %% "case-app" % "1.1.3"

