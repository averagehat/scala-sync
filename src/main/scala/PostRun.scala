// a single app that runs with the following parameters:
// runissue id
// rundir (e.g. from TMDPIR/RUNS)
// post-run redmine stuff
val vdbSequencingProject = mgr.getProjectManager.getProjectByKey("vdb sequencing") // throws Redmine...
//val runIssue = IssueFactory.create(vdbSequencingProject.getId, RUNNAME)
val runIssue = mgr.getIssueManager.getIssueById(args.runIssue)
// args.runDir is absolute path
val sampleSheet = args.runDir/"SampleSheet.csv"
if (!sampleSheet.exists()) Try(throw new Exception(f"No sample sheet found in directory ${args.runName}"))
val attachment = mgr.getAttachmentManager.uploadAttachment(sampleSheet, "text/plain", tsv.getBytes("UTF-8"))
runIssue.addAttachment(attachment)
case class SampleResultRow(sampleName: String, sampleId: Int)
rows.map(issueManager.createRelation(runIssue.getId, _.sampleId, "blocks")) // right order?

// post-run file-system stuff
val runFolder = args.runDir.toFile
runFolder.copy(cfg.ngs_data/"RawData")
val rdDir = cfg.ngs_data/"ReadData"/runFolder
rdDir.mkdir()
for (gz <- runFolder.listdir()) {
  gz.unzipTo( rdDir/gz.dropExtension )
  }
/// but First! rename files in readdata to include the sampleName.
rows.zipWithIndex.map( (idx, row) => {
  val file = f"${row.sampleId}_S${idx}_*.fastq.gz".glob(runFolder).headEither // record that it's missing
  val newBaseName = file.basename.replace(".fastq.gz", row.sampleName) ++ ".fastq.gz" 
}
readDataFiles.map( fq
  val issueDir = cfg.ngs_data / "ReadsBySample" / row.sampleId
  issueDir.mkdir_p()

  

// 
// relate all the issue ids from the samplesheet to the run. 
// https://github.com/taskadapter/redmine-java-api/blob/15cc8ff1e22e9d4e6401eb0e73d5886b7f8117bc/src/main/java/com/taskadapter/redmineapi/bean/Issue.java#L64
// it's tech's job to relate run and sequencing request issues.

//mgr.getIssueManager.createIssue()
// we still need some issue for the tech to let us know when the data transfer happened.
// so this software should get the $runissue and $runname
