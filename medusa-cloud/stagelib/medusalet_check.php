<?php

$name = $_GET['name'];
if (strstr($name, ".apk") != ".apk") {
	$name = $name . ".apk";
}

/* upload medusalet */
print "<form enctype=\"multipart/form-data\" action=\"medusalet_upload.php?dir=medusalets\" method=\"POST\">";
print "<input type=\"hidden\" name=\"MAX_FILE_SIZE\" value=\"1000000\" />";
print "Choose an medusalet file to upload: <input name=\"uploadedfile\" type=\"file\" />";
print "<input type=\"submit\" value=\"Upload File\" />";
print "</form>";

print "Aqualet-Name: $name<br><br><hr>";

print "<table border=1><tr><td>Analyzer Log</td><td>Available Medusalets</td></tr><tr><td valign=top>\n";
/* first column content */
exec("cd medusalets; ./check_medusalet.py $name; cd ..", $out, $err);
$i = 0;
while ($out[$i]) {
	print $out[$i] . "<br>";
	$i = $i + 1;
}
$i = 0;
while ($err[$i]) {
	print $err[$i] . "<br>";
	$i = $i + 1;
}
$out = null;
print "</td><td valign=top>\n";
/* second column content */
exec("ls medusalets/*.apk", $out, $err);
$i = 0;
while ($out[$i]) {
	$aqname = str_replace("medusalets/", "", $out[$i]);
	print "<a href=\"./medusalet_check.php?name=$aqname\">";
	print $aqname . "</a><br>";
	$i = $i + 1;
}

print "</td></table>\n";

?>

