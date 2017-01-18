import bleh.{Bleh, Config}
import caseapp._
import com.taskadapter.redmineapi.{RedmineManager, RedmineManagerFactory}
import better.files._
import bleh.Bleh.SampleSheetRow
import com.taskadapter.redmineapi.bean.{Issue, IssueFactory}
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
  case class SeqReqCreate(ids: String, reqIssue: Option[Int]) extends App {
    // main happens here
    run(Bleh.getConfig(), SeqReqCreate(ids, reqIssue))

    def run(cfg: Config, args: SeqReqCreate) = {
      val mgr = RedmineManagerFactory.createWithApiKey(cfg.url, cfg.api_key)
      val names = ids.toFile.lines.map(SampleName.apply _)
      redmineStuff(args, mgr, cfg, names)
      ()
    }


    def redmineStuff(args: SeqReqCreate, mgr: RedmineManager, cfg: Config, names: Traversable[SampleName]) = {

      def searchSubject(subj: String) = mgr.getIssueManager.getIssues(Map(("subject", subj)))
      // getIssues throws RedmineException
      val sampleProject = mgr.getProjectManager.getProjectByKey("samples"); // throws Redmine...

      import scalaz.IList._
      //import scalaz.MonadPlus
      val existingIssues: List[Issue] = names.map(x => searchSubject( x.name ))
        .map(x => x.getResults.headOption)
        .toList.unite // catMaybes
      val newSamples = names.filterNot(x => existingIssues.map(_.getSubject).element(x.name)).toList
      // Don't delete this!
      val newIssues = newSamples.map(x =>
        mgr.getIssueManager.createIssue(IssueFactory.create(sampleProject.getId, x.name))) // throws RedmineException

      val HEADER = "ID\tSampleName"
      val rows = (newIssues ++ existingIssues).map(x => SampleSheetRow(x.getSubject, x.getId))
      import kantan.csv.ops._
      import kantan.csv.generic._ // Automatic derivation of codecs.

      val tsv = rows.toList.asCsv('\t', HEADER)
      val attachment = mgr.getAttachmentManager.uploadAttachment("samplename-id.csv", "text/plain", tsv.getBytes("UTF-8"))
      args.reqIssue match {
        case Some(id) => {
          val reqIssue = mgr.getIssueManager.getIssueById(id)
          reqIssue.addAttachment(attachment)
          mgr.getIssueManager.update(reqIssue)
          println(s"uploaded samplename/id file to issue ${cfg.url}/issues/${id}")
        }
        case None => println(tsv)
      }
      //runIssue.addCustomField(mgr.getCustomFieldManager.get)
// should this be automated? or let PI upload themselves.
  //    val attachment = mgr.getAttachmentManager.uploadAttachment("id-samplename.tsv", "text/plain", tsv.getBytes("UTF-8"))
  //    reqIssue.addAttachment(attachment)
      // issue2.setAssigneeId(issue.getAssigneeId) // !! getXId is nullable! (Integer, not int)
      //mgr.getIssueManager.update(issue2) // !! Needed to update!
    //  if (!Maybe.fromNullable(reqIssue.getTracker).map(_.getName == cfg.sequence_request_tracker).getOrElse(false))
    //    Try(throw new Exception(f"Request issue number $requestIssueId does not have correct tracker ${cfg.sequence_request_tracker}"))
    }
  }
  object SeqCreateApp extends AppOf[SeqReqCreate]
  SeqCreateApp.main(Seq("--ids", "IDSFILE", "--req-issue", "12314").toArray)
}
