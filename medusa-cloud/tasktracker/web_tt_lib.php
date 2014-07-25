<?php

function get_dbhost() {
    $fp = fopen("../config/db_host.info", "r");
    $dbhost = fgets($fp);
    fclose($fp);
    return trim($dbhost);
}

function get_dbaccount() {
    $fp = fopen("../config/db_account.info", "r");
    $line = fgets($fp);
	$info = split('\|', $line);
    fclose($fp);
    return $info;
}

$rdb_host=get_dbhost();
$acctinfo=get_dbaccount();
$rdb_username=trim($acctinfo[0]); // Mysql username
$rdb_password=trim($acctinfo[1]); // Mysql password
$rdb_dbname="medusa"; // Database name

$dbConn="";

function dbconnect()
{
	// Connect to server and select databse.
	global $dbConn, $rdb_host, $rdb_username, $rdb_password, $rdb_dbname;
	print ("$rdb_dbname");
	$dbConn = mysql_connect("$rdb_host", "$rdb_username", "$rdb_password") or print("! cannot connect");
	print("here");
	if (!$dbConn) {
		echo mysql_error();
		exit;
	} else {
	}

	mysql_select_db("$rdb_dbname", $dbConn) or die("! cannot select DB");

	return $dbConn;
}

function get_env_var($conn, $tag) {
	$sql = "select medvalue from CS_env where medkey='$tag'";
	$res = mysql_query($sql);
	if (mysql_num_rows($res) > 0) {
		$row = mysql_fetch_row($res);
		return $row[0];
	}
	return "No Data";
}

function get_hostname()
{
	exec('hostname',$out,$err);
	return strtok($out[0], ".");
}

function mergefile($targetdir, $tmpdir, $fullname, $randname)
{
	$bItr = 0;
	$cnt = 0;
	$tok = strtok($fullname, ".");

	if ($tok != false) {
		// merge files..
		if ($randname != null) {
			$fp = fopen($targetdir.$randname, "w");
		} else {
			$fp = fopen($targetdir.$fullname, "w");
		}
		
		while ($bItr == 0 && $fp != null) {
			$ext = sprintf("%03d", $cnt);
			$name = $tmpdir . $tok . ".V" . $ext;
			$cnt = $cnt + 1;
			//print "$name\n";
			if (file_exists($name) == true) {
				//echo "existing file " . $name . "\n";
				$fpin = fopen($name, "r");
				$contents = fread($fpin, filesize($name));
				fwrite($fp, $contents, filesize($name));
				fclose($fpin);
			} else {
				$bItr = 1;
				//echo "does not exist $name\n";
				$name = $tmpdir . $fullname;
				$fpin = fopen($name, "r");
				$contents = fread($fpin, filesize($name));
				fwrite($fp, $contents, filesize($name));
				fclose($fpin);
			}
		} 

		fflush($fp);
		fclose($fp);
	}

	return $cnt;
}

function mergefile_m($targetdir, $tmpdir, $fullname, $randname, $fext)
{
    $bItr = 0;
    $cnt = 0;
    $totalsize = 0;
    $tok = strtok($fullname, ".");

    if ($tok != false) {
        // merge files..
        if ($randname != null) {
            $fp = fopen($targetdir.$randname, "w");
        } else {
            $fp = fopen($targetdir.$fullname, "w");
        }

        while ($bItr == 0 && $fp != null) {
            $ext = sprintf("%03d", $cnt);
			if ($fext == "mp4" || $fext == "3gp") $name = $tmpdir . $tok . ".V" . $ext;
			else if ($fext == "jpg") $name = $tmpdir . $tok . ".I" . $ext;
			else if ($fext == "flv") $name = $tmpdir . $tok . ".F" . $ext;
            $cnt = $cnt + 1;
            //print "$name\n";
            if (file_exists($name) == true) {
                //echo "existing file " . $name . "\n";
                $fpin = fopen($name, "r");
                $contents = fread($fpin, filesize($name));
                $totalsize += fwrite($fp, $contents, filesize($name));
                fclose($fpin);
            } else {
                $bItr = 1;
                //echo "does not exist $name\n";
                $name = $tmpdir . $fullname;
                $fpin = fopen($name, "r");
                $contents = fread($fpin, filesize($name));
                $totalsize += fwrite($fp, $contents, filesize($name));
                fclose($fpin);
            }
        }

        fflush($fp);
        fclose($fp);
    }

    return $totalsize;
}

function mergefile_m5($targetdir, $tmpdir, $fullname, $randname, $fext)
{
    $bItr = 0;
    $cnt = 0;
    $totalsize = 0;
    $tok = strtok($fullname, ".");

    if ($tok != false) {
        // merge files..
        if ($randname != null) {
            $fp = fopen($targetdir.$randname, "w");
        } else {
            $fp = fopen($targetdir.$fullname, "w");
        }

        while ($bItr == 0 && $fp != null) {
            $ext = sprintf("%05d", $cnt);
			/*
			if ($fext == "mp4" || $fext == "3gp") $name = $tmpdir . $tok . ".V" . $ext;
			else if ($fext == "jpg") $name = $tmpdir . $tok . ".I" . $ext;
			else if ($fext == "flv") $name = $tmpdir . $tok . ".F" . $ext;
			*/
			$name = $tmpdir . $tok . ".V" . $ext;
            $cnt = $cnt + 1;
            //print "$name\n";
            if (file_exists($name) == true) {
                //echo "existing file " . $name . "\n";
                $fpin = fopen($name, "r");
                $contents = fread($fpin, filesize($name));
                $totalsize += fwrite($fp, $contents, filesize($name));
                fclose($fpin);
            } else {
                $bItr = 1;
                //echo "does not exist $name\n";
                $name = $tmpdir . $fullname;
                $fpin = fopen($name, "r");
                $contents = fread($fpin, filesize($name));
                $totalsize += fwrite($fp, $contents, filesize($name));
                fclose($fpin);
            }
        }

        fflush($fp);
        fclose($fp);
    }

    return $totalsize;
}

function getrandomfilename($ext)
{
	$randname = md5(rand() * time());
	return $randname.'.'.$ext;
}

function getmergefilename($imei, $fname)
{
	return $imei . "_" . $fname;
}

?>


