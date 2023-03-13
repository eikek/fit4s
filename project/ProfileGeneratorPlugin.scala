import sbt._
import sbt.Keys._
import fit4s.build.{ProfileReader, TypesGenerator}

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

    val (typeDefs, messageDefs) = ProfileReader.readFile(input)(logger).get
    logger.info(s"Got typedefs: ${typeDefs.map(_.toString).mkString("\n")}")
    // logger.info(s"Got typedefs: ${messageDefs.map(_.toString).mkString("\n")}")
    val typeSource = TypesGenerator.generate(pkg, typeDefs)
    val typeFile = target / "ProfileTypes.scala"
    // IO.write(typeFile, typeSource)
    // Seq(typeFile)

    Seq.empty
  }
}
