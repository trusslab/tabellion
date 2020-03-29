<?php
if(isset($_FILES['file'])){
	echo "uploadding....";
	$target_path = "signedfiles_offeror/"; //here folder name 
	$target_path = $target_path . basename($_FILES['file']['name']);

	echo $target_path;

	if(isset($_FILES['error'])){
		error_log("Upload File >>" . $target_path . $_FILES['error'] . " \r\n", 3,
		"Log.log");
	}
	

	error_log("Upload File >>" . basename($_FILES['file']['name']) . "     \r\n",
	3, "Log.log");

	 if(move_uploaded_file($_FILES['file']['tmp_name'], $target_path)) {
		echo "The signature has been uploaded";
		//shell_exec("./push_rendered2.sh");
		if($_FILES['file']['name'] == 'signature5') /* FIXME */
			shell_exec("./push_rendered2.sh");
	   } else {
	  echo "There was an error uploading the file, please try again!";
	 }
}
?>

