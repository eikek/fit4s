import sbt._
import sbt.Keys._
import fit4s.build.{ProfileReader, TypesGenerator}
import src.main.scala.fit4s.build.MessagesGenerator

object ProfileGeneratorPlugin extends AutoPlugin {

  object autoImport {

    val profileFile = settingKey[File]("The Profile.xslx file from the SDK")
    val packageName = settingKey[String]("The package for the generated sources")

    val generateProfile = taskKey[Seq[File]]("Generate global profile from the xlsx file")
  }

  import autoImport._

  override def projectSettings = Seq(
    profileFile := (LocalRootProject / baseDirectory).value / "project" / "profile" / "Profile.xslx",
    packageName := "fit4s.profile",
    generateProfile := {
      val profileXlsx = profileFile.value
      val pkg = packageName.value
      val target = (Compile / sourceManaged).value / "profileGen"
      val logger = streams.value.log
      generateProfileSources(profileXlsx, pkg, target, logger)
    }
  )

  def generateProfileSources(
      input: File,
      pkg: String,
      target: File,
      logger: Logger
  ): Seq[File] = {

    logger.info(s"Generating profile sources to $target")
    IO.deleteFilesEmptyDirs(List(target))

    val (typeDefs, messageDefs) = ProfileReader.readFile(input)(logger).get
    val typeSources = TypesGenerator.generate(pkg, typeDefs)
    val msgSources = MessagesGenerator.generate(pkg, messageDefs, typeDefs)

    logger.info(s"Generating ${typeSources.size + msgSources.size} files …")
    (typeSources ++ msgSources).map { src =>
      val file = target / src.filename
      IO.write(file, src.contents)
      file
    }
  }
}
