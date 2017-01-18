package bleh
import com.taskadapter.redmineapi._
import com.taskadapter.redmineapi.bean._
import caseapp._
import better.files._
import java.io.{IOError, File => JFile}

import scala.collection.JavaConversions._
import scala.util.Try
import better.files._
import java.io.{File => JFile}

import scalaz.IList
import scalaz.Maybe.Just
import scalaz._
import Scalaz._
import kantan.csv.{ParseError, ReadError, RowDecoder}
import kantan.csv.ops._
import kantan.csv.generic._
import net.jcazevedo.moultingyaml.DefaultYamlProtocol
import net.jcazevedo.moultingyaml._
// case class decoder derivation
import kantan.csv.scalaz._
// monad for result type
import scala.util._

/**
  * Created by mikep on 1/9/17.
  */
case class Config(sequence_request_tracker: String, api_key: String, sample_tracker: String, url: String, ngs_data: String)
object MyYamlProtocol extends DefaultYamlProtocol {
  implicit val configFormat = yamlFormat5(Config) // 4 = number of fields in Config
}
// Args

case class PostRunArgs(runDir: String, runIssue: Int) extends App {
  // main happens here
  Bleh.run(PostRunArgs(runDir, runIssue))
}
object PostRunApp extends AppOf[PostRunArgs]
// PostRunApp.main(Seq("--run-dir", "/media/RUNS/foobar").toArray)

import MyYamlProtocol._

// Program
object Bleh {
  case class SampleSheetRow(sampleId: String, sampleName: Int) // this argOrder matters
    def getSheetRows(ssPath: File): Either[ReadError, List[SampleSheetRow]] = {
    //def getSheetRows(ssPath: File): ReadError\/List[SampleSheetRow] = {
      val ss = ssPath.contentAsString
      implicit val decoder: RowDecoder[SampleSheetRow] = RowDecoder.ordered(SampleSheetRow.apply _)
      val DATAHEADER = "\\[Data\\]\\s+"
      for {
        data <- ss.split(DATAHEADER).lift(1).toRight(ParseError.IOError(f"Malformed Spreadsheet at $ssPath missing $DATAHEADER")): scala.util.Either[ReadError, String]
        rawRows = data.readCsv[List, SampleSheetRow](',', true).map(_.toEither)
        rows <- rawRows.sequenceU
      } yield rows
    }
  def eitherToDisjunct[A,B](x: Either[A,B]): A\/B = x match {
    case Left(x) => \/.left(x)
    case Right(x) => \/.right(x)
  }
  def getConfig() = "config.yaml".toFile.contentAsString.parseYaml.convertTo[Config]
  def run(args: PostRunArgs): Unit = {
    val cfg = getConfig()
    val ssPath = args.runDir / "SampleSheet.csv"
    if (!ssPath.exists()) throw new Exception(f"No sample sheet found in directory ${args.runDir}")

    val mgr = RedmineManagerFactory.createWithApiKey(cfg.url, cfg.api_key)
    val result = for {
      runIssue <- uploadSampleSheet(mgr, args, ssPath)
      rows <- eitherToDisjunct(getSheetRows(ssPath))
      _ <- relateSampleSheetIssues(mgr, runIssue, rows)
      _ <- fileProcesses(args, cfg).leftMap(new Exception(_))
      result3 <- toReadsBySample(cfg, rows, args)
    } yield result3
    result match {
      case (\/-(files)) => files.foreach(f => println(s"successfully created $f"))
      case (-\/(error)) => throw error
    }
    //println("done")
  }

  def attachFile(issue: Issue, mgr: RedmineManager, file: File): Throwable\/Attachment = {
    val attachment = mgr.getAttachmentManager.uploadAttachment("text/plain", file.toJava)
    issue.addAttachment(attachment)
    //runIssue.addCustomField(mgr.getCustomFieldManager.get)
    Try({ mgr.getIssueManager.update(issue); attachment }).toDisjunction
  }
  def uploadSampleSheet(mgr: RedmineManager, args: PostRunArgs, sampleSheet: File): Throwable\/Issue = {
    val runIssue = mgr.getIssueManager.getIssueById(args.runIssue)
    val _ = attachFile(runIssue, mgr, sampleSheet)
    \/.right(runIssue) // or is this created after-the-fact? no, it has to be created by the tech, because we need sequencing progress notifications
  }
  def relateSampleSheetIssues(mgr: RedmineManager, runIssue: Issue, rows: List[SampleSheetRow]): Throwable\/List[IssueRelation] = {
    rows.map(row => Try(mgr.getIssueManager.createRelation(runIssue.getId, row.sampleName, "blocks")).toDisjunction).sequenceU
  }

type Error = String

def fileProcesses(args: PostRunArgs, cfg: Config): Error\/List[File]  = {
  args.runDir.toFile.copyTo(cfg.ngs_data / "RawData" / "MiSeq" / args.runDir.toFile.name, overwrite = false)
  val fastqDir = args.runDir/"Data/Intensities/BaseCalls"
  val readDataDir = cfg.ngs_data / "ReadData" / "MiSeq" / args.runDir
  readDataDir.createDirectory()
  for (gz <- fastqDir.list) {
    gz.unzipTo(readDataDir / gz.nameWithoutExtension) // does nameWithoutExtension really work?
  }
  /// but First! rename files in readdata to include the sampleName.
  val ssPath = args.runDir / "SampleSheet.csv"
  val rows = getSheetRows(ssPath).right.get
  val renames = rows.zipWithIndex.map({ case (row: SampleSheetRow, idx: Int) => {
    val sIndex = idx + 1
    val files = fastqDir.glob(f"${row.sampleName}_S${sIndex}_*.fastq").toList // can be multiple, i.e. R1, R2! .headOption.toRight(f"Missing Sample for ID ${row.sampleId}")
    if (files.length != 4) \/.left(s"Glob for ${row.sampleName} failed with results: ${files}")
    else files.map(file =>
      for {
        basename <- file.nameOption.toRightDisjunction(f"Bad filename has no basename: $file")
        newBaseName = basename.replaceFirst("\\.fastq", row.sampleId) ++ ".fastq": String
        result <- Try(file.renameTo((readDataDir / newBaseName).toString)).toDisjunction.leftMap(_.getMessage)
      } yield result).sequenceU
  }
  }).sequenceU
  renames.map(_.join)
}
def toReadsBySample(cfg: Config, rows: List[SampleSheetRow], args: PostRunArgs): Throwable\/List[File] = {
  val readData = cfg.ngs_data / "ReadData" / "MiSeq" / args.runDir
  rows.map(row => {
    val issueDir = cfg.ngs_data / "ReadsBySample" / row.sampleName.toString
    for {
      _ <- Try(issueDir.createIfNotExists(asDirectory = true)).toDisjunction
      sources = readData.glob(s"${row.sampleName}_S_*.fastq").toList
      res <- sources.map(src => Try(src.symbolicLinkTo(issueDir / src.name)).toDisjunction).sequenceU
    } yield res
  }).sequenceU.map(_.join)
}

}

//type APath = String
//
// relate all the issue ids from the samplesheet to the run.
// https://github.com/taskadapter/redmine-java-api/blob/15cc8ff1e22e9d4e6401eb0e73d5886b7f8117bc/src/main/java/com/taskadapter/redmineapi/bean/Issue.java#L64
// it's tech's job to relate run and sequencing request issues.
