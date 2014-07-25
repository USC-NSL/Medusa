<html>
<head>
<title>Insert Anything into DB</title>
</head>

<body>

<?php

include_once "libphp/libmysql.php";

$conn = dbconnect();

$opcode=$_GET['opcode'];
$cmd=$_GET['cmd'];
$program=$_GET['medscript'];

if ($opcode == "insert") {
	$sql = "insert into CS_cmdqueue(cmd,medscript) values('$cmd', 'program/$program');";
	$res = mysql_query($sql, $conn);
}

mysql_close($conn);

?> 

Program may be executed..

</body>

