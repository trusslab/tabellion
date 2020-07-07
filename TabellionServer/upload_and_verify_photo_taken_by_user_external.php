<?php

if(isset($_FILES['file'])){
	echo "uploading photo taken by user....";
	$current_time_interval = $_POST['timeinterval'];
	$user_email = $_POST['useremail'];
	$photo_folder_name = "photos_taken_by_user";
	$signature_folder_name = "signatures_of_photos_taken_by_user";

	echo "The user email is: $user_email";

	echo "Going to make dir: " . "./users/$user_email/$photo_folder_name/" . "\n";

	$old_umask = umask(0);
	mkdir("./users/$user_email/$photo_folder_name/", 0777, true);
	mkdir("./users/$user_email/$signature_folder_name/", 0777, true);
	umask($old_umask);

	$target_path_photo = "./users/$user_email/$photo_folder_name/"; //here folder name 
	$target_path_photo = $target_path_photo . basename($_FILES['file']['name']);
	
	$target_path_signature = "./users/$user_email/$signature_folder_name/"; //here folder name 
	$target_path_signature = $target_path_signature . basename($_FILES['signature']['name']);

	echo $target_path_photo;

	if(isset($_FILES['error'])){
		error_log("Upload File >>" . $target_path_photo . $_FILES['error'] . " \r\n", 3,
		"Log.log");
	}

	error_log("Upload File >>" . basename($_FILES['file']['name']) . "     \r\n",
	3, "Log.log");

	// Saving photo's time_interval
	$path_of_photos_time_intervals = "./users/$user_email/photos_taken_by_user_time_intervals.txt";
	$photos_time_intervals_content = file_get_contents($path_of_photos_time_intervals);
	$photos_time_intervals_content .= basename($_FILES['file']['name']);
	$photos_time_intervals_content .= ":";
	$photos_time_intervals_content .= $current_time_interval;
	$photos_time_intervals_content .= "\n";
	file_put_contents($path_of_photos_time_intervals, $photos_time_intervals_content);

	if(move_uploaded_file($_FILES['file']['tmp_name'], $target_path_photo)) {
		echo "The file has been uploaded";
		//shell_exec("./push_rendered2.sh");
		echo $_FILES['file']['name'];

		if(move_uploaded_file($_FILES['signature']['tmp_name'], $target_path_signature)) {
			// Verify signature
			echo "Signature file has been uploaded";
			shell_exec("cd users/$user_email/$signature_folder_name/;openssl base64 -d -in " . basename($_FILES['signature']['name']) . " -out sign.sha256");
			chmod("users/$user_email/$signature_folder_name/sign.sha256", 01777);	// Just a temp solution, not safe (2)	
			chmod("users/$user_email/$photo_folder_name/" . basename($_FILES['file']['name']), 01777);	// Just a temp solution, not safe (3)	

			echo "cd users/$user_email/$photo_folder_name/;
			openssl dgst -sha256 -verify ../../keys/$user_email.pem -signature ../$signature_folder_name/sign.sha256 " . basename($_FILES['file']['name']);
			$output = shell_exec("cd users/$user_email/$photo_folder_name/;
			openssl dgst -sha256 -verify ../../keys/$user_email.pem -signature ../$signature_folder_name/sign.sha256 " . basename($_FILES['file']['name']));
			if(substr($output, 0, 11) == "Verified OK"){
				echo "photo verified!!!";
			}
		}

	} else {
		echo "There was an error uploading the file, please try again!";
	}
}

?>