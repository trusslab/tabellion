<?php
if(isset($_FILES['file'])){
	echo "uploadding....";
	$target_path = "signedfiles_offeree/"; //here folder name 
	$target_path = $target_path . basename($_FILES['file']['name']);

	$flog = fopen("mylog", "w+");
	echo $target_path;

	error_log("Upload File >>" . $target_path . $_FILES['error'] . " \r\n", 3,
	"Log.log");

	error_log("Upload File >>" . basename($_FILES['file']['name']) . "     \r\n",
	3, "Log.log");

	echo "Receiving HTTPPost data\n";

	$timestamp = $_POST['timestamp'];
	$signature = $_POST['signature'];
	$counter = $_POST['counter'];
	$rand = $_POST['rand'];

	/* 
	$base = "status_";
	$post_file = $base . basename($_FILES['file']['name']);
	file_put_contents($post_file, $timestamp, FILE_APPEND);
	file_put_contents($post_file, "\n", FILE_APPEND);
	file_put_contents($post_file, $signature, FILE_APPEND);
	file_put_contents($post_file, "\n", FILE_APPEND);
	file_put_contents($post_file, $counter, FILE_APPEND);
	file_put_contents($post_file, "\n", FILE_APPEND);
	file_put_contents($post_file, $rand, FILE_APPEND);
	file_put_contents($post_file, "\n", FILE_APPEND);

	$file = 'status2.txt';
	$current = "SIGNED\n";
	file_put_contents($file, $current);
	*/

	 if(move_uploaded_file($_FILES['file']['tmp_name'], $target_path)) {
		echo "The file " . basename($_FILES['uploadedfile']['name']) .
	   " has been uploaded";
		if($_FILES['file']['name']=='signature5') { /*FIXME*/
			/** Check if the offeror has revoked the offer before **/

			$is_verified = True;
			echo "Going to verify signature...";
			for($x = 1; $x <= 5; $x++){
				shell_exec("cd signedfiles_offeree;openssl base64 -d -in signature" . $x . " -out sign.sha256");	
				// We have a permission bug needs to be fixed: If I do not manually make the sign.sha256 to be writeable by public, 
				// this shell_exec will not work since it needs to overwrite an existing file.
				echo "current status:" . $x . "\n";
				$output = shell_exec("cd signedfiles_offeree;openssl dgst -sha256 -verify publicKey.pem -signature sign.sha256 screenshot" . $x);
				echo $output;
				if(substr($output, 0, 11) != "Verified OK"){
					$is_verified = False;
					break;
				}
			}

			shell_exec("rm status1.txt; python FCMmsg_getstatus1.py");

			set_time_limit(0);
			do {
    				if (file_exists("status1.txt")) {
        				echo "The file was found: " . date("d-m-Y h:i:s") . "<br>";
 		       			break;
    				}
			} while(true);
		
			$sfile = fopen("status1.txt", 'r');
			$status1 = fgets($sfile);
			$time1 = fgets($sfile);
			fwrite($flog, "time 1 >> " . $time1 . "\n");
			fwrite($flog, "time 2 >> " . $timestamp . "\n");
			if($status1 == "REVOKED\n") {
				if($timestamp < $time1) { /* We signed first*/
					if($is_verified){
						shell_exec("python FCMmsg_done1.py");
						shell_exec("python FCMmsg_done2.py");
					} else {
						shell_exec("python FCMmsg_offereeVerificationFailed1.py;python FCMmsg_offereeVerificationFailed2.py");
					}
				}
				else
					shell_exec("python FCMmsg_revoked2.py");
			}
			else if($status1 == "SIGNED\n") {
				if($is_verified){
					shell_exec("python FCMmsg_done1.py");
					shell_exec("python FCMmsg_done2.py");
				} else {
					shell_exec("python FCMmsg_offereeVerificationFailed1.py;python FCMmsg_offereeVerificationFailed2.py");
				}
			}
		}
	   }
	 else {
	  echo "There was an error uploading the file, please try again!";
	 }
}

?>

