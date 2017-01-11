import com.taskadapter.redmineapi._
import com.taskadapter.redmineapi.bean.{Issue, IssueFactory}
import net.jcazevedo.moultingyaml.DefaultYamlProtocol

import scala.collection.JavaConversions._
import scala.util.Try
import scalaz.IList
import scalaz.Maybe.Just

val uri = "https://www.vdbpm.org"
val apiKey = "TODO"

val mgr = RedmineManagerFactory.createWithApiKey(uri, apiKey)
//val issues = mgr.getIssueManager.getIssues("vdbsequencing", null)
//
//issue.getCustomFieldByName("SampleList")
import scalaz._
import Scalaz._
val sampleNames = IList("test-run", "not-exist")
val searchSubject = (n:String) => mgr.getIssueManager.getIssues(Map(("subject", n))) // getIssues throws RedmineException
val sampleProject = mgr.getProjectManager.getProjectByKey("samples"); // throws Redmine...

import scalaz.IList._
//import scalaz.MonadPlus
val existingIssues: IList[Issue] = sampleNames.map(searchSubject)
                                              .map(x => fromList(x.getResults.toList).headMaybe)
                                              .unite // catMaybes
val newSamples = sampleNames.filterNot(x => existingIssues.map(_.getSubject).element(x))
// Don't delete this!
//lazy val newIssues = newSamples.map(x =>
//  mgr.getIssueManager.createIssue(IssueFactory.create(sampleProject.getId, x))) // throws RedmineException

val newIssues = existingIssues
val HEADER = "ID\tSampleName\n"
val tsv = HEADER ++ (newIssues ++ existingIssues).map(i => f"${i.getId}\t${i.getSubject}").foldRight("")(_ ++ "\n" ++ _)
val attachment = mgr.getAttachmentManager.uploadAttachment("id-samplename.tsv", "text/plain", tsv.getBytes("UTF-8"))
val issue2 = searchSubject("test-run").getResults.head
val reqIssue = issue2
reqIssue.addAttachment(attachment)
// issue2.setAssigneeId(issue.getAssigneeId) // !! getXId is nullable! (Integer, not int)

// Config & Args
case class Config(sequence_request_tracker: String, api_key: String, sample_tracker: String, url: String)
object MyYamlProtocol extends DefaultYamlProtocol {
  implicit val configFormat = yamlFormat4(Config) // 4 = number of fields in Config
}

import better.files._
import java.io.{File => JFile}
import MyYamlProtocol._
import net.jcazevedo.moultingyaml._
val cfg = "config.yaml".toFile.contentAsString.parseYaml.convertTo[Config]

def run(cfg: Config, args: EntryPoint): Unit = {
  val mgr = RedmineManagerFactory.createWithApiKey(uri, apiKey)
}


import caseapp._
type APath = String
case class EntryPoint(ids: APath, reqIssue: Int) extends App {
  // main happens here
  println("Hi, this is my app.")
}
object EntryPointApp extends AppOf[EntryPoint]
EntryPointApp.main(Seq("--ids", "IDSFILE", "--req-issue", "12314").toArray)
//val yaml = Config("Sequencing Request", apiKey, "Samples!").toYaml.prettyPrint
//"config.yaml".toFile.write(yaml)

mgr.getIssueManager.update(issue2) // !! Needed to update!
val requestIssueId = 0 // comes from commandline arg
if (!Maybe.fromNullable(reqIssue.getTracker).map(_.getName == cfg.sequence_request_tracker).getOrElse(false))
    Try(new Exception(f"Request issue number $requestIssueId does not have correct tracker ${cfg.sequence_request_tracker}"))

// could create a data structure defining the update needed and then process in the IO Monad. would help with
// not forgetting the .update required call and also isolating exceptions and IO and testing
//issue.addAttachment(attachment)

//val newIssues = sampleNames.diff(existingIssues.map(_.g))
// for each samplename, look up sample issues by subject
// the above will automatically look for a "bai" index file by convention

/*
val file = new java.io.File("/Users/mikep/scala/attemptscala/samples.csv")
val out = new java.io.FileWriter("/Users/mikep/scala/attemptscala/out.csv")
import kantan.csv.ops._
import kantan.csv.generic._ // case class decoder derivation

case class SampleInputRow(sampleName: String) //, sampleId: Option[String])
case class SampleResultRow(sampleName: String, sampleId: String)
val input = file.readCsv[List, SampleInputRow](',', false)
input
//"sampleName\nfoo\nbar\nbaz".readCsv[List, SampleSheetRow](',', true).foreach(println)
//out.writeCsv[SampleSheetRow](List(SampleSheetRow("Yowsers!")), ',')
//val sns = reader.map(_.getOrElse(99))
//sns
//fw.write("Flouboo")
//reader.foreach(println)
//sns.foreach(println)
// https://www.biostars.org/p/214515/


*/
