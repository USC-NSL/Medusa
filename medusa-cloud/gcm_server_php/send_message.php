<?php
ini_set('display_errors', 'On');
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
if (isset($_GET["regId"]) && isset($_GET["message"])) {
    $regId = $_GET["regId"];
    $message = $_GET["message"];
    $path = '/home/jyr/project/'.$message;    
    //echo $path
    $file = file_get_contents($path, true);
    include_once './GCM.php';
    
    $gcm = new GCM();

    $registatoin_ids = array($regId);
    $message = array("msg" => $message);
    $nm = array("msg" => $file);
    $result = $gcm->send_notification($registatoin_ids, $nm);

    echo $result;
}
?>
