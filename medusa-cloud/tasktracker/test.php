<?php
ini_set('display_errors', 1);  
error_reporting(E_ALL);  
	$dbConn = mysql_connect('localhost', 'root', 'xx') or print("! cannot connect");
	print("here");
if(!$dbConn)  
{  
   die("DB connection failed: " . mysql_error());  
}
?>


