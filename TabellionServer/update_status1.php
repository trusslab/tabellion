<?php

error_log("Updating status...");

$status = $_POST['status'];
$timestamp = $_POST['timestamp'];

$fp = fopen('status1.txt', 'w+');

if($status == 1) {
	$current_status = "REVOKED\n";
	fwrite($fp, $current_status);
	fwrite($fp, $timestamp);
}
else{
	$current_status = "SIGNED\n";
	fwrite($fp, $current_status);
	fwrite($fp, $timestamp);
}
?>
