import bleh.Config
import caseapp._
import com.taskadapter.redmineapi.{RedmineManager, RedmineManagerFactory}
import better.files._
import bleh.Bleh.SampleSheetRow
import com.taskadapter.redmineapi.bean.Issue
import kantan.csv.RowEncoder

import scala.collection.JavaConversions._
import scalaz.IList
import scalaz._
import Scalaz._
/**
  * Created by mikep on 1/17/17.
  */
object RequestCreate {
  case class SampleName(name: String)
  case class SeqReqCreate(ids: String, reqIssue: Int) extends App {
    // main happens here
    //println("Hi, this is my app.")
    def run(cfg: Config, args: SeqReqCreate) = {
      val mgr = RedmineManagerFactory.createWithApiKey(cfg.url, cfg.api_key)
      val names = ids.toFile.lines.map(SampleName.apply _)
      redmineStuff(mgr, cfg, names)
      ()
    }


    def redmineStuff(mgr: RedmineManager, cfg: Config, names: Traversable[SampleName]) = {

      //val issues = mgr.getIssueManager.getIssues("vdbsequencing", null)
      //
      //issue.getCustomFieldByName("SampleList")
      def searchSubject(subj: String) = mgr.getIssueManager.getIssues(Map(("subject", subj)))
      // getIssues throws RedmineException
      val sampleProject = mgr.getProjectManager.getProjectByKey("samples"); // throws Redmine...

      import scalaz.IList._
      //import scalaz.MonadPlus
      val existingIssues: IList[Issue] = names.map(x => searchSubject( x.name ))
        .map(x => fromList(x.getResults.toList).headMaybe)
        .toList.toIList.unite
      // catMaybes
      val newSamples = names.filterNot(x => existingIssues.map(_.getSubject).element(x.name))
      // Don't delete this!
      //lazy val newIssues = newSamples.map(x =>
      //  mgr.getIssueManager.createIssue(IssueFactory.create(sampleProject.getId, x))) // throws RedmineException

      val newIssues = existingIssues
      val HEADER = "ID\tSampleName\n"
      val rows = (newIssues ++ existingIssues).map(x => SampleSheetRow(x.getSubject, x.getId))
      import kantan.csv.ops._
      import kantan.csv.generic._ // Automatic derivation of codecs.

      val tsv = rows.toList.asCsv('\t', HEADER)
      println(tsv)
// should this be automated? or let PI upload themselves.
  //    val attachment = mgr.getAttachmentManager.uploadAttachment("id-samplename.tsv", "text/plain", tsv.getBytes("UTF-8"))
  //    reqIssue.addAttachment(attachment)
      // issue2.setAssigneeId(issue.getAssigneeId) // !! getXId is nullable! (Integer, not int)
      //mgr.getIssueManager.update(issue2) // !! Needed to update!
      val requestIssueId = 0 // comes from commandline arg
    //  if (!Maybe.fromNullable(reqIssue.getTracker).map(_.getName == cfg.sequence_request_tracker).getOrElse(false))
    //    Try(throw new Exception(f"Request issue number $requestIssueId does not have correct tracker ${cfg.sequence_request_tracker}"))
    }
  }
  object SeqCreateApp extends AppOf[SeqReqCreate]
  SeqCreateApp.main(Seq("--ids", "IDSFILE", "--req-issue", "12314").toArray)
}
