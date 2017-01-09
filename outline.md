


The software creates the run issue and relates the samples. [we would want a verification step here.] run happens. A new script runs locally, trusts the Miseq filenames (no interaction w/ redmine), puts them into ReadData and then by folder into ReadsBySample
tech would add watchers, confirm correctness, after run is created.

If there is a mistake found in the samplesheet:
  *

 Input: sample-sheet, VDBPM info
 Output: Samplesheet with sample_name column under Data set to issue IDs.
 Side-effects: A new run issue with the old and corrected samplesheet uploaded, and issues for each sample related.

Everything above [Data] is saved and wrote as the top of the new CSV output
samplesheet [Data] encoded as a case class, where only SampleName and SampleID are required. Fails if any row fails.




PI creates sequencing request. They provide samplenames. 
They want a resultant table of samplename => ID for the techs.

Right before the run, or right after, the tech creates a run issue with the
miseq-generated samplesheet. 
 -> from this samplesheet, the sample issues can be related. 
 
rename fastq files: add their sample ID, date, run ID 
beware underscore issue subject search
hopefully techs use description from now on

`<samplename>_S<samplenumber>_<lane...>`
where <samplename> comes from the Sample_Name column; and <samplenumber> is the 
line on the Samplesheet.csv ([Data] section) where the sample appears.

so look up sample by samplenumber in samplesheet, replace samplenumber with sample_id


if there is a problem with naming, make smaple_name <issueid>_1, <issueid>_2...


### Post-run file manipulation
1. Copy $runfolder to RawData.
2. Create ReadData/$runfolder.
3. un-gzip fastq.gzs from ReadData/$runfolder. Renaming:
   1. rename "samplenumber" to "Sample_ID" (from samplesheet)
   2.  add current date.
4. for each $fastq in ReadData:
   1. create ReadsBySample/$SN for each SN=samplename of fastq. (mkdir -p)
   2. create a (relative?) symbolic link from ReadData/$fastq to ReadsBySample/$SN/$fastq
   
   
can a  sequencing request ever be connected to multiple runs? YES


### PI Sequencing Request processing
1. Given a samplelist; search/create issues
2. produce a TSV map of samplename => issueid
3. progromatticaly attach to sequencing request
4. relate these sample issues or not?
5. Other stuff?

### Tech Sequencing Run & Samplesheet upload
-- lowest import?
1. create a runissue.
2. Attach Samplesheet.csv
3. Using Sample_Name column of Samplesheet.csv, relate the sample issues.
