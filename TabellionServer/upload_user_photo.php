<?php
if(isset($_FILES['file'])){
	echo "uploading new user photo....";
    $email_address = $_POST['email'];
    $file_name = $_POST['filename'];
    
    $target_path = "users/user_photos/"; //here folder name 
	$target_path = $target_path . basename($_FILES['file']['name']);

	echo $target_path;

	if(isset($_FILES['error'])){
		error_log("Upload File >>" . $target_path . $_FILES['error'] . " \r\n", 3,
		"Log.log");
	}

	error_log("Upload File >>" . basename($_FILES['file']['name']) . "     \r\n",
	3, "Log.log");

	 if(move_uploaded_file($_FILES['file']['tmp_name'], $target_path)) {
		echo "The file " . basename($_FILES['uploadedfile']['name']) .
	   " has been uploaded";
	
		set_time_limit(0);
		do {
    			if (file_exists("users/user_photos/$file_name")) {
					chmod("users/user_photos/$file_name", 01777);	// Just a temp solution, not safe (0)
        			echo "The file was found: " . date("d-m-Y h:i:s") . "<br>";
        			break;
    			}
		} while(true);

		echo "User Photo Upload Success!";

	   } else {
	  echo "There was an error uploading the file, please try again!";
	 }
}
?>