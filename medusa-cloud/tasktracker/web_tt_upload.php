<?php

// For integrity check.. for non-malicious HTTP 200 OK response. 
header("Content-Sender: MEDUSA");

$ver = $_GET['ver'];
$time=$_GET['time'];
$apname=$_GET['apname'];
$uid = $_GET['uid'];
$imei = $_GET['imei'];
$review = $_GET['review'];

$pid = 0;
$qid = 0;
$muid = 0;

$pid = $_GET['pid'];
$qid = $_GET['qid'];
$muid = $_GET['muid'];

include_once "web_tt_lib.php";

$tblname="CS_data"; // Table name
$targetdir = "data/";
$tmpdir = "/tmp/";
$fname = $_FILES['uploadedfile']['name'];
$fsize = $_FILES['uploadedfile']['size'];
$ftype = $_FILES['uploadedfile']['type'];

// make the random file name
$ext = substr(strrchr($fname, "."), 1);
$fname = str_replace(".$ext", "", $fname);
$fname = str_replace(".", "", $fname);
$fname = str_replace("-", "", $fname);
$fname = str_replace(" ", "", $fname);
$fname = $pid . $qid . $muid . $fname . ".$ext"; //Xing: adding pid, qid for uniqueness

if ($ext == "jpg") {
	$ftype = "image";
} else if ($ext == "3gp" || $ext == "mp4") {
	$ftype = "video";
} else if ($ext == "flv") {
	$ftype = "summary";
} else if ($ext == "amr") {
	$ftype = "audio";
} else if ($ext == "txt") {
	$ftype = "txt";
} else {
	$targetdir = "/tmp/";
}

$targetpath = $targetdir.basename($fname);
$tmppath = $tmpdir . $imei . basename($fname);

if (move_uploaded_file($_FILES['uploadedfile']['tmp_name'], $tmppath)) 
{
	if ($ext == "mp4" || $ext == "3gp" || $ext == "jpg" || $ext == "flv" || $ext == "txt") {

		$randfile = getmergefilename($imei, $fname);
		if ($ext == "3gp") {
			$randfile = str_replace(".3gp", ".mp4", $randfile);
			$ext = ".mp4";
		}
		$fsize = mergefile_m5($targetdir, $tmpdir, $imei . $fname, $randfile, $ext);
		$targetpath = $targetdir . $randfile;
		
		/* update db entry */
		$conn = dbconnect();

		$fieldlist = "path,size,type,time,ap,imei,uid,pid,qid,muid,review";
		$review = base64_decode($review);
		$base_url = get_env_var($conn, 'BASE-URL-DATA');
		$vallist = "'$base_url$targetpath','$fsize','$ftype','$time','$apname','$imei','$uid','$pid','$qid','$muid',\"$review\"";	
		$sql = "insert into $tblname($fieldlist) values($vallist)";
		$res = mysql_query($sql);

		/* debug 
		$fp = fopen("data/debug.medusa", "a");
		fwrite($fp, $sql, strlen($sql));
		fclose($fp);
		/* debug */
			
		@chmod("./".$targetpath, 0755);

		mysql_close($conn);
		
		$cmd = "python notify.py medusa_rss file,".$pid.",".$qid.",".$uid."=".$muid.",".$base_url.$targetpath;
		shell_exec($cmd);
	} 
} 

?>

