package bleh
import com.taskadapter.redmineapi._
import com.taskadapter.redmineapi.bean.{Issue, IssueFactory}

import scala.collection.JavaConversions._
import scala.util.Try


/**
  * Created by mikep on 1/9/17.
  */
object Bleh {

  def main(args: Array[String]): Unit = {
    print(args)
    val x = 23
    x
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
}
