<?php
if(isset($_FILES['file'])){
	echo "uploadding....";
	$contract_id = $_POST['contractid'];
	$last_screenshot_num = $_POST['lastscreenshotid'];
	$user_email = $_POST['useremail'];
	$current_time_interval = $_POST['timeinterval'];
	$last_screenshot_num_i = (int)$last_screenshot_num;
	$offeror_email = shell_exec("python3 get_offeror_email.py $contract_id");
	$offeree_email = shell_exec("python3 get_offeree_email.py $contract_id");
	$offeror_token = shell_exec("python3 get_user_token.py $offeror_email");
	$offeree_token = shell_exec("python3 get_user_token.py $offeree_email");
	$status = shell_exec("python3 get_contract_status.py $contract_id");

	$current_role = $_POST['current_role'];

	if($current_role == ""){
		$current_role = "offeree";
		if($user_email == $offeror_email){
			$current_role = "offeror";
		}
	}

	echo "user is $current_role";

	# echo "The offeror email is: $offeror_email";

	$old_umask = umask(0);
	mkdir("./submitted_files/$contract_id/$current_role/", 0777);
	umask($old_umask);

	$target_path = "submitted_files/$contract_id/$current_role/"; //here folder name 
	$target_path = $target_path . basename($_FILES['file']['name']);

	echo $target_path;

	if(isset($_FILES['error'])){
		error_log("Upload File >>" . $target_path . $_FILES['error'] . " \r\n", 3,
		"Log.log");
	}
	

	error_log("Upload File >>" . basename($_FILES['file']['name']) . "     \r\n",
	3, "Log.log");

	 if(move_uploaded_file($_FILES['file']['tmp_name'], $target_path)) {
		echo "The file has been uploaded";
		//shell_exec("./push_rendered2.sh");
		echo $_FILES['file']['name'];
		
		if($_FILES['file']['name'] == "signature$last_screenshot_num"){ /* FIXME */
			echo "last file submitted!";
		}
	   } else {
	  echo "There was an error uploading the file, please try again!";
	 }
}
?>

