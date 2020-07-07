<?php
if(isset($_FILES['file'])){
	//echo "uploading pdf for extracting text....";

	$old_umask = umask(0);
	mkdir("./files/cache_for_pdf", 0777);
	umask($old_umask);

	$target_path = "files/cache_for_pdf/"; //here folder name 
	$target_path = $target_path . basename($_FILES['file']['name']);

	//echo $target_path;

	error_log("Upload File >>" . $target_path . $_FILES['error'] . " \r\n", 3,
	"Log.log");

	error_log("Upload File >>" . basename($_FILES['file']['name']) . "     \r\n",
	3, "Log.log");

	//echo "Trying to upload...";

	 if(move_uploaded_file($_FILES['file']['tmp_name'], $target_path)) {
        //echo "The file " . basename($_FILES['uploadedfile']['name']) . " has been uploaded";
        
        //echo "python3 extract_text_from_pdf.py $target_path";

        $contract_content = shell_exec("python3 extract_text_from_pdf.py \"$target_path\"");

        //echo "testing...";
        
        echo $contract_content;

	   } else {
	  echo "Oh there! Error happened!!!";
	 }
}

?>
