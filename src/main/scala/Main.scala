package bleh
import com.taskadapter.redmineapi._
import com.taskadapter.redmineapi.bean.{Issue, IssueFactory}

import caseapp._
import better.files._
import java.io.{File => JFile}
import MyYamlProtocol._
import net.jcazevedo.moultingyaml._
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
// case class decoder derivation
import kantan.csv.scalaz._
// monad for result type


/**
  * Created by mikep on 1/9/17.
  */
object Bleh {
  case class SampleSheetRow(sampleId: String, sampleName: Int) // this argOrder matters
    def getSheetRows(ssPath: File): IList[SampleSheetRow] = {
      val ss = ssPath.contentAsString
      implicit val decoder: RowDecoder[SampleSheetRow] = RowDecoder.ordered(SampleSheetRow.apply _)
      val DATAHEADER = "\\[Data\\]\\s+"
      for {
        data <- ss.split(DATAHEADER).lift(1).toRight(ParseError.IOError(f"Malformed Spreadsheet at $ssPath missing $DATAHEADER")): Either[ReadError, String]
        rawRows = data.readCsv[List, SampleSheetRow](',', true).map(_.toEither)
        rows <- rawRows.sequenceU
      } yield rows.toIList
    }
  def main(args: Array[String]): Unit = {
    val ssPath = "/Users/mikep/scala/attemptscala" / "SampleSheet.csv"
    getSheetRows(ssPath)
  }
  def processSampleNames(file: java.io.File): Try[Unit] = {
    val uri = "https://www.vdbpm.org"
    val apiKey = "NOTHING"

    case class SampleSheetRow (sampleId: String, sampleName: String)
    val mgr = RedmineManagerFactory.createWithApiKey (uri, apiKey)
    val issues = mgr.getIssueManager ().getIssues ("vdbsequencing", null)

    val xs = issues.iterator.toList.take (4)
    xs.foreach (println)
    val issue = xs.head
    issue.getCustomFieldByName ("SampleList")
    val results = mgr.getIssueManager.getIssues (Map (("subject", "test-run") ) )
    results.getResults.foreach (println)
    val sampleNames = List ("test-run")
    val searchSubject = (n: String) => mgr.getIssueManager.getIssues (Map (("subject", n) ) )
    val sampleProject = mgr.getProjectManager.getProjectByKey ("samples");
    def searchOrCreate(name: String): Issue = {
    val results = searchSubject (name).getResults
    assert (results.length <= 1)
    if (results.nonEmpty) results.head
    else {
    val issue = IssueFactory.create (sampleProject.getId (), name);
    mgr.getIssueManager.createIssue (issue)
    issue
  }
  }
    val existingIssues = sampleNames.map (searchSubject)
    // for each samplename, look up sample issues by subject
    Try(())
  }
  def fileProcesses(args: PostRunArgs, cfg: Config) {
    val runFolder = args.runDir.toFile
    runFolder.copyTo(cfg.ngs_data/"RawData"/"MiSeq", overwrite = false)
    val readDataDir = cfg.ngs_data/"ReadData"/"MiSeq"/runFolder
    readDataDir.createDirectory()
    for (gz <- runFolder.list) {
      gz.unzipTo(readDataDir/gz.nameWithoutExtension)
    }
    /// but First! rename files in readdata to include the sampleName.
    val ssPath = args.runDir/"SampleSheet.csv"
    val rows = getSheetRows(ssPath)
    val renames = rows.zipWithIndex.map({ case (row: SampleSheetRow, idx: Int) => {
      val sIndex = idx + 1
      val files =  runFolder.glob(f"${row.sampleId}_S${sIndex}_*.fastq.gz").toList // can be multiple, i.e. R1, R2! .headOption.toRight(f"Missing Sample for ID ${row.sampleId}")
      if (files.isEmpty) Left(s"Glob for ${row.sampleId} failed!")
      else files.map(file =>
        for {
          basename <- file.nameOption.toRight(f"Bad filename has no basename: $file")
          //newBaseName = basename.replaceFirst("\\.fastq\\.gz", row.sampleName) ++ ".fastq.gz"
          newBaseName = basename.replaceFirst("\\.fastq", row.sampleName) ++ ".fastq"
          result <- Try(file.renameTo(readDataDir / newBasename)).toOption.toRight(f"Filed to rename file $file")
        } yield result).sequenceU
      }
    }).sequenceU
      //readDataFiles.map( fq
    for (row <- rows) {
      val issueDir = cfg.ngs_data / "ReadsBySample" / row.sampleId
      issueDir.createIfNotExists(asDirectory = true)
    }
}
}

// Config & Args
case class Config(sequence_request_tracker: String, api_key: String, sample_tracker: String, url: String, ngs_data: String)
object MyYamlProtocol extends DefaultYamlProtocol {
  implicit val configFormat = yamlFormat5(Config) // 4 = number of fields in Config
}

val cfg = "config.yaml".toFile.contentAsString.parseYaml.convertTo[Config]

case class PostRunArgs(runDir: String) extends App {
}
object PostRunApp extends AppOf[PostRunArgs]
PostRunApp.main(Seq("--run-dir", "/media/RUNS/foobar").toArray)
//type APath = String
case class SeqReqCreate(ids: String, reqIssue: Int) extends App {
  // main happens here
  //println("Hi, this is my app.")
  def run(cfg: Config, args: SeqReqCreate) = {
    val mgr = RedmineManagerFactory.createWithApiKey(cfg.url, cfg.api_key)
    ()
  }
}
object EntryPointApp extends AppOf[SeqReqCreate]
EntryPointApp.main(Seq("--ids", "IDSFILE", "--req-issue", "12314").toArray)
//
// relate all the issue ids from the samplesheet to the run.
// https://github.com/taskadapter/redmine-java-api/blob/15cc8ff1e22e9d4e6401eb0e73d5886b7f8117bc/src/main/java/com/taskadapter/redmineapi/bean/Issue.java#L64
// it's tech's job to relate run and sequencing request issues.
}
