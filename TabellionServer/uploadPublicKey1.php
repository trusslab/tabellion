<?php
if(isset($_FILES['file'])){
	echo "uploading....";
	$key_name =  $_POST['email'];
	$token = $_POST['token'];
	$target_path = "users/keys/"; //here folder name 
	$target_path = $target_path . basename($_FILES['file']['name']);

	echo $target_path;

	if(isset($_FILES['error'])){
		error_log("Upload File >>" . $target_path . $_FILES['error'] . " \r\n", 3,
		"Log.log");
	}

	error_log("Upload File >>" . basename($_FILES['file']['name']) . "     \r\n",
	3, "Log.log");

	
	echo "Trying to get file: ".$_FILES['file']['name']." with: ".$_FILES['file']['tmp_name'];

	 if(move_uploaded_file($_FILES['file']['tmp_name'], $target_path)) {
		echo "The file " . basename($_FILES['uploadedfile']['name']) .
	   " has been uploaded";
	
		set_time_limit(0);
		do {
    			if (file_exists("users/keys/publicKey.pem")) {
					rename("users/keys/publicKey.pem", "users/keys/$key_name.pem");
					chmod("users/keys/$key_name.pem", 01777);	// Just a temp solution, not safe (0)
        			echo "The file was found: " . date("d-m-Y h:i:s") . "<br>";
        			break;
    			}
		} while(true);

		# shell_exec("python FCMmsg_uploadPublicKeySuccess1.py $token");
		echo "Key Upload Success!";

	   } else {
	  echo "There was an error uploading the file, please try again!";
	 }
}

?>

