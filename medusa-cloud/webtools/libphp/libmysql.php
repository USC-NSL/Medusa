<?php

#
# - caveat
#	: path to the config file may be important in some cases.
#	  below two functions should be called from the directory 
#	  at the same level with config/ directory.
#
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

	$dbConn = mysql_connect("$rdb_host", "$rdb_username", "$rdb_password") or die("! cannot connect");
	if (!$dbConn) {
		echo mysql_error();
		exit;
	} else {
		//echo "just connected..";
	}

	mysql_select_db("$rdb_dbname", $dbConn) or die("! cannot select DB");

	return $dbConn;
}

function get_env_var($conn, $tag) 
{
	$sql = "select medvalue from CS_env where medkey='$tag'";
	$res = mysql_query($sql);
	if (mysql_num_rows($res) > 0) {
		$row = mysql_fetch_row($res);
		return $row[0];
	}
	return "No Data";
}

?>

