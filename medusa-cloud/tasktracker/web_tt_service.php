<?php

header("Content-Sender: RSS");

/* 
 *	Parse incoming HTTP request.
 */
$action=$_GET['action'];
$pid=$_GET['pid'];
$medusalet_name=$_GET['qtype'];
$qid=$_GET['qid'];
$msg=$_GET['custom'];
$type=$_GET['type'];
$uidlist=$_GET['uidlist'];
$rid=$_GET['rid'];
$rkey=$_GET['rkey'];
$wid=$_GET['wid'];
$amtid=$_GET['amtid'];	/* User Identifier from Amazon Mechanical Turk */
$dlimit=$_GET['dlimit'];
$cmdpush=$_GET['cmdpush'];

if (is_null($action) == true) {
	$action=$_POST['action'];
	$pid=$_POST['pid'];
	$medusalet_name=$_POST['qtype'];
	$qid=$_POST['qid'];
	$msg=$_POST['custom'];
	$type=$_POST['type'];
	$uidlist=$_POST['uidlist'];
	$rid=$_POST['rid'];
	$rkey=$_POST['rkey'];
	$wid=$_POST['wid'];
	$amtid=$_POST['amtid'];	
	$dlimit=$_POST['dlimit'];
	$cmdpush=$_POST['cmdpush'];
}

$qtype=$medusalet_name;

include_once "web_tt_lib.php";

/* logging function */
function rsslog($str) {
	//print $str;

    $logfiled = fopen('logs/logs_web_tt_service.txt', 'a');
	/* added by yurong */
    fwrite($logfiled, $str);
    fflush($logfiled);
    fclose($logfiled);
    /* end of addition */
}

function post_request($url, $data, $referer='') {
 
    // Convert the data array into URL Parameters like a=b&foo=bar etc.
    $data = http_build_query($data);
 
    // parse the given URL
    $url = parse_url($url);
 
    if ($url['scheme'] != 'http') { 
        die('Error: Only HTTP request are supported !');
    }
 
    // extract host and path:
    $host = $url['host'];
    $path = $url['path'];
    $port = $url['port'];
 
    // open a socket connection on port 80 - timeout: 30 sec
    $fp = fsockopen($host, $port, $errno, $errstr, 30);
 
    if ($fp){
 
        // send the request headers:
        fputs($fp, "POST $path HTTP/1.1\r\n");
        fputs($fp, "Host: $host\r\n");
 
        if ($referer != '')
            fputs($fp, "Referer: $referer\r\n");
 
        fputs($fp, "Content-type: application/x-www-form-urlencoded\r\n");
        fputs($fp, "Content-length: ". strlen($data) ."\r\n");
        fputs($fp, "Connection: close\r\n\r\n");
        fputs($fp, $data);
 
        $result = ''; 
        while(!feof($fp)) {
            // receive the results of the request
            $result .= fgets($fp, 128);
        }
    }
    else { 
        return array(
            'status' => 'err', 
            'error' => "$errstr ($errno)"
        );
    }
 
    // close the socket connection:
    fclose($fp);
 
    // split the result header from the content
    $result = explode("\r\n\r\n", $result, 2);
 
    $header = isset($result[0]) ? $result[0] : '';
    $content = isset($result[1]) ? $result[1] : '';
 
    // return as structured array:
    return array(
        'status' => 'ok',
        'header' => $header,
        'content' => $content
    );
}

rsslog("start");

$conn = dbconnect();
$hit_server_url = "http://" . get_env_var($conn, 'HIT-HOST') . get_env_var($conn, 'HIT-URI');

rsslog("* action=" . $action . "<br>\n");
rsslog("* medusalet-name=" . $medusalet_name . "<br>\n");

# default table unless otherwise specified below.
$tblname = "CS_event";

/* 
 *	Execute Program Logic
 */
if ($action == "regRS") {
	/* 
	 * Register new "Remote Sensing" task
	 *		- Update RS_event database
	 *		- send SMS message to the group of phones.
	 */

	/* Verify IMEIs */
	$userarray = split('[#|]', $amtid);
	rsslog("* " . count($userarray) . " users found.<br>\n");

	/* Verify relevant medusalet */
	$sql = "select * from medusalets where name='$medusalet_name'";
	$res = mysql_query($sql);
	$n = mysql_num_rows($res);

	/* MRA: this code checks medusalet's validity
			will be enabled when all tests are done. * /
	if ($n > 0) {
		rsslog("* medusalet=" . $medusalet_name . " verified<br>");
	}
	else {
		rsslog("! no such medusalet: " . $medusalet_name . "<br>");
		header("Status: 400 Bad Request");

		$sql = "insert into $tblname(pid,action,qtype,qid,msg) values('$pid', 'error', '$qtype', '$qid', '! no such medusalet: ' . $medusalet_name)";
		$res = mysql_query($sql);
		rsslog("sms requests have been sent: $res<br>");

		return;
	}
	*/

	if ($cmdpush == 'sms') {
		for ($i = 0; $i < count($userarray); $i++)
		{
			$userid = str_replace("AMT=", "", $userarray[$i]);
	
			/* Insert DB Entry */
			$sql = "insert into $tblname(pid,userid,action,qtype,qid,msg) values('$pid','$userid','$action','$qtype','$qid','$userid::$msg')";
			$res = mysql_query($sql);
			rsslog("* event has been logged: $res<br>\n");
	
			/* [TODO] Send SMS to the users so that users activate relevant medusalets and do the jobs. */
			rsslog("");
			$dblog = "";
	
			/* Send SMS message to the mobile users */
			rsslog("SMS message is sending to " . $userid . "<br>\n");

			$amsg = "<xml><pid>$pid</pid><qid>$qid</qid><amtid>$userid</amtid>";
			if ($dlimit != null) {
				$amsg .= "<dlimit>$dlimit</dlimit>";
			}
			$msg = $amsg . "$msg</xml>";
			$params = array('action' => 'smsCmd', 'pid' => $pid, 'qid' => $qid
						  , 'content' => $msg, 'rid' => $rid, 'rkey' => $rkey
						  , 'wid' => $userid);
			$result = post_request($hit_server_url, $params);

			if ($result['status'] == 'ok'){
				// Print headers 
				rsslog($result['header']); 
				rsslog('<hr />');

				// print the result of the whole request:
				rsslog($result['content']);
			}
			else {
				rsslog('A error occured: ' . $result['error']); 
			}
			
			$dblog = $dblog . "--" . $res;
		}
	}
	else if ($cmdpush == 'c2dm') {

		/* get authentication id for c2dm yurong */
		rsslog("-----------------<br>\n");
    	
		/* Iterate all users */
		for ($i = 0; $i < count($userarray); $i++)
		{
			$userid = str_replace("AMT=", "", $userarray[$i]);
	
			/* Insert DB Entry */
			$sql = "insert into $tblname(pid,userid,action,qtype,qid,msg) values('$pid','$userid','$action','$qtype','$qid','$userid::$msg')";
			$res = mysql_query($sql);
			rsslog("* Event has been logged: $res<br>\n");
	
			/* [TODO] Send SMS to the users so that users activate relevant medusalets and do the jobs. */
			$dblog = "";
	
			/* Send SMS message to the mobile users */
			rsslog("* C2DM message is sending to " . $userid . "<br>\n");

			$amsg = "<xml><pid>$pid</pid><qid>$qid</qid><amtid>$userid</amtid>";
			if ($dlimit != null) {
				$amsg .= "<dlimit>$dlimit</dlimit>";
			}
			$msg = $amsg . "$msg</xml>";
			$params = array('action' => 'smsCmd', 'pid' => $pid, 'qid' => $qid
						  , 'content' => $msg, 'rid' => $rid, 'rkey' => $rkey
						  , 'wid' => $userid);

			/* get C2DM registartion id */
			$sql = "select regid from CS_c2dm where wid='$userarray[$i]'";
    		$res = mysql_query($sql);
			$row = mysql_fetch_row($res);
			$regid = $row[0];
			$matches_1 = $matches[1];

//			$cmd = "./c2dm_post.sh \"$matches_1\" \"$regid\" \"$msg\"";
			$cmd = "python sendmsg.py \"$regid\" \"$msg\"";
			$response = shell_exec($cmd);

			rsslog("* MATCHES: $matches_1<br>\n");
			rsslog("* REG-ID: $regid<br>\n");
			rsslog("* CMD: $cmd<br>\n");
			rsslog("* RESP: $response<br>\n");
			rsslog("-----------------<br>\n");
			rsslog($msg);
			
			$dblog = $dblog . "--" . $res;
		}
	}
	else if ($cmdpush == 'ws_c2dm') {

		rsslog("-----------------<br>\n");

		$zmq_port = explode('|', trim(file_get_contents('../config/port.info')))[3];
    	
		/* Iterate all users */
		for ($i = 0; $i < count($userarray); $i++) {
			$userid = str_replace("AMT=", "", $userarray[$i]);
	
			/* Insert DB Entry */
			$sql = "insert into $tblname(pid,userid,action,qtype,qid,msg) values('$pid','$userid','$action','$qtype','$qid','$userid::$msg')";
			$res = mysql_query($sql);
			rsslog("* Event has been logged: $res<br>\n");
	
			/* Send a message to the users so each activates the relevant medusalets to do the jobs. */
			$dblog = "";
	
			/* Send message to the mobile users */
			rsslog("* WS message is being sent to " . $userid . "<br>\n");

			$amsg = "<xml><pid>$pid</pid><qid>$qid</qid><amtid>$userid</amtid>";
			if ($dlimit != null) {
				$amsg .= "<dlimit>$dlimit</dlimit>";
			}
			$msg = $amsg . "$msg</xml>";
			$params = array('action' => 'wsCmd', 'pid' => $pid, 'qid' => $qid
						  , 'content' => $msg, 'rid' => $rid, 'rkey' => $rkey
						  , 'wid' => $userid);

			/* get WS id */
			$sql = "select regid from CS_c2dm where wid='$userarray[$i]'";
    		$res = mysql_query($sql);
			$row = mysql_fetch_row($res);
			$regid = $row[0];
			$matches_1 = $matches[1];

			$cmd = "php sendmsg.php \"$userid\" \"$msg\" $zmq_port";
			//rsslog("+ cmd= " . $cmd . "<br>\n");
			$response = shell_exec($cmd);
			rsslog("+ response= " . $response . "<br>\n");

			rsslog("* MATCHES: $matches_1<br>\n");
			rsslog("* REG-ID: $regid<br>\n");
			rsslog("* CMD: $cmd<br>\n");
			rsslog("* RESP: $response<br>\n");
			rsslog("-----------------<br>\n");
			rsslog($msg);
			
			$dblog = $dblog . "--" . $res;
		}
	}

	rsslog("completed<br>\n");

	/* Insert DB Entry */
	$sql = "insert into $tblname(pid,action,qtype,qid,msg) values('$pid', 'smsSent', '$qtype', '$qid', '$dblog')";
	$res = mysql_query($sql);
	rsslog("sms requests have been sent: $res<br>\n");
}
else if ($action == "report" || $action == "time") { //by Xing for time stamp
	if ($type == "uid") {
		$fieldlist = 'pid,userid,action,qtype,qid,msg';
		$vallist = "'$pid','$amtid','$action','$qtype','$qid','$uidlist'";
		$sql = "insert into $tblname($fieldlist) values($vallist)";
		$res = mysql_query($sql);
	}
	else {

		/* debug 
        $fp = fopen("data/debug.medusa", "w");
        fwrite($fp, $msg, strlen($msg));
        fwrite($fp, "\n", strlen($msg));
		$msg = base64_decode($msg);
        fwrite($fp, $msg, strlen($msg));
        fclose($fp);
        /* debug */

		$fieldlist = 'pid,userid,action,qtype,qid,msg';
		$vallist = "'$pid','$amtid','$action','$qtype','$qid',\"$msg\"";
		$sql = "insert into $tblname($fieldlist) values($vallist)";
		$res = mysql_query($sql);

		$vallist = "'$pid','$amtid','$type','$qtype','$qid',\"$msg\"";
		$sql = "insert into $tblname($fieldlist) values($vallist)";
		$res = mysql_query($sql);
	}
}
else if ($action == "completeTask") {
	/* aggregate all reported data */
	$sql = "select msg from $tblname where action='report' and pid='$pid' and qid='$qid' and userid='$amtid'";
    $res = mysql_query($sql);
    $itr = mysql_num_rows($res);

	$uidstr = "";
	if (is_null($uidlist) == false && $uidlist != "null")
		$uidstr = $uidlist;

	for ($i = 0; $i < $itr; $i++) {
		$row = mysql_fetch_row($res);
		if ($uidstr == "") {
			$uidstr = $row[0];
		}
		else {
			$uidstr = $uidstr . "|" . $row[0];
		}
	}
	str_replace("|null", "", str_replace("null|", "", $uidstr));

	/* delete previous completeTask entry */
	$sql = "delete from $tblname where pid='$pid' and qid='$qid' and userid='$amtid' and action='completeTask'";
	$res = mysql_query($sql);

	/* 
	 *	insert complete message into the database
	 *		: it will trigger TFS to the next stage. 
	 */
	$fieldlist = 'pid,userid,action,qtype,qid,msg';
	$vallist = "'$pid','$amtid','$action','$qtype','$qid',\"$uidstr\"";
	$sql = "insert into $tblname($fieldlist) values($vallist)";
	$res = mysql_query($sql);
	// add by YJ
	$notification = "'$action','$pid','$qid'";
    $tag = "medusa_rss";
	$cmd = "python notify.py " . $tag . " " . $notification;
	$response = shell_exec($cmd);

	rsslog("\n* cmd: " . $cmd . "\n");
    //end
	
}
else if ($action == "reviewReport") {
	$tblname = "CS_review";
	$muid = $_GET['muid'];
	$mpath = $_GET['mpath'];
	$review = base64_decode($_GET['review']);
	$fieldlist = 'userid,pid,qid,muid,mpath,review';
	$vallist = "'$amtid','$pid','$qid','$muid','$mpath','$review'";
	$sql = "insert into $tblname($fieldlist) values($vallist)";
	$res = mysql_query($sql);
}
else if ($action == "syncC2DM") {
	/* CS_c2dm TABLE: wid, regid. */
	$tblname = 'CS_c2dm';
	//$wid = base64_decode($_GET['wid']);
	$wid = $_GET['wid'];
	$regid = base64_decode($_GET['regid']);
    $sql = "select * from $tblname where wid='$wid'";
    $res = mysql_query($sql);
    $n = mysql_num_rows($res);
	/*print "$sql<br>\n";
	print "N: $n<br>\n";
	print "WID: $wid<br>\n";
	print "REGID: $regid<br>\n";*/
	if ($n == 0) {
		$fieldlist = 'wid,regid';
		$vallist = "'$wid','$regid'";
		$sql = "insert into $tblname($fieldlist) values($vallist)";
		print "SQL INSERT: $sql<br>\n";
		$res = mysql_query($sql);
	}
	else {
		// update
		$sql = "update $tblname set regid='$regid' where wid='$wid'";
		$res = mysql_query($sql);
	}
}
else if ($action == "syncENV") {
	$tblname = 'CS_globalvars';
	$wrid = base64_decode($_GET['wrid']);
	$wrkey = base64_decode($_GET['wrkey']);
	$wwid = base64_decode($_GET['wwid']);
	$fieldlist = 'pid,envkey,envval';
	// W_RID
	/* Verify relevant medusalet */
    $sql = "select * from $tblname where pid='$pid' and envkey=\"W_RID\" and envval=\"$wrid\"";
    $res = mysql_query($sql);
    $n = mysql_num_rows($res);
	if ($n == 0) {
		$vallist = "'$pid','W_RID','$wrid'";
		$sql = "insert into $tblname($fieldlist) values($vallist)";
		$res = mysql_query($sql);
	}
	// W_RKEY
    $sql = "select * from $tblname where pid='$pid' and envkey=\"W_RKEY\" and envval=\"$wrkey\"";
    $res = mysql_query($sql);
    $n = mysql_num_rows($res);
	if ($n == 0) {
		$vallist = "'$pid','W_RKEY','$wrkey'";
		$sql = "insert into $tblname($fieldlist) values($vallist)";
		$res = mysql_query($sql);
	}
	// W_WID
    $sql = "select * from $tblname where pid='$pid' and envkey=\"W_WID\" and envval=\"$wwid\"";
    $res = mysql_query($sql);
    $n = mysql_num_rows($res);
	if ($n == 0) {
		$vallist = "'$pid','W_WID','$wwid'";
		$sql = "insert into $tblname($fieldlist) values($vallist)";
		$res = mysql_query($sql);
	}
}
else {
	/* 
     *  insert complete message into the database
     *      : it will trigger TFS to the next stage. 
     */
    $fieldlist = 'pid,userid,action,qtype,qid,msg';
	$msg = base64_decode($msg);
    $vallist = "'$pid','$amtid','$action','$qtype','$qid','$msg'";
    $sql = "insert into $tblname($fieldlist) values($vallist)";
    $res = mysql_query($sql);

	//rsslog("! unknown 'action' field=" . $action . " so logged into the database..<br>");
}


/* finalization */
mysql_close($conn);

rsslog("* finished.");


?>




