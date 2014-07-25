<?php

$target_path = $_GET['dir'];
$target_path = $target_path . "/";

$fname = basename( $_FILES['uploadedfile']['name']);
$target_path = $target_path . $fname;

if(move_uploaded_file($_FILES['uploadedfile']['tmp_name'], $target_path)) {
    print "The file ".$fname." has been uploaded <br>";
	print "<a href=\"./medusalet_check.php?name=$fname\">Click to analyze it immediately</a>";
} else{
    print "There was an error uploading the file, please try again!";
}

?>
