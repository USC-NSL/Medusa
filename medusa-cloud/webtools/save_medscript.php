<?

include_once "libphp/libconfig.php";
include_once "libphp/libmysql.php";

$path = $_POST['path'];
$encoded = base64_decode($_POST['content']);

if ($path != null) {
	print "* [ajax] saving [$path].";
	$fp = fopen($path, "w");
	fwrite($fp, $encoded, strlen($encoded));
	fclose($fp);
}
else {
	print "! invalid arguments.<br>";
	print "- path: $path<br>";
	print "- xml: $xml<br>";

}

?>


