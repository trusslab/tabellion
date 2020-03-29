<?php

$contract_id = $_POST['contractid'];

if(isset($_FILES['file'])){
	echo "uploading contract file....";

	$target_path = "submitted_files/$contract_id/revision/"; //here folder name 
	$target_path = $target_path . basename($_FILES['file']['name']);
	$fms_token = $_POST['fms_token'];

	echo $target_path;

	error_log("Upload File >>" . $target_path . $_FILES['error'] . " \r\n", 3,
	"Log.log");

	error_log("Upload File >>" . basename($_FILES['file']['name']) . "     \r\n",
	3, "Log.log");

	echo "Trying to upload...";

	 if(move_uploaded_file($_FILES['file']['tmp_name'], $target_path)) {
		echo "The file " . basename($_FILES['uploadedfile']['name']) .
	   " has been uploaded";
	
	
		set_time_limit(0);
		do {
    			if (file_exists("submitted_files/$contract_id/revision/doc.md")) {
        			echo "The file was found: " . date("d-m-Y h:i:s") . "<br>";
					shell_exec("cd submitted_files;cp -p ocr_checker.py all.sh checker.py mdtohtml.py checker.sh clean.sh note.html review_note.html last-1.png style.txt $contract_id/revision");
        			break;
    			}
        } while(true);
        
        echo "all success!!!";

	   } else {
	  echo "Oh there! Error happened!!!";
	 }
}

?>

