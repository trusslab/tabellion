<?php
if(isset($_FILES['file'])){
    echo "adding new user....";
    //echo phpinfo();
    $email_address = $_POST['email'];
    $first_name = $_POST['firstname'];
    $last_name = $_POST['lastname'];
    $password = $_POST['password'];
    $token = $_POST['token'];
    $num_of_fingers_to_be_verified_str = $_POST["num_of_fingers_to_be_verified"];
    $num_of_fingers_to_be_verified = (int)$num_of_fingers_to_be_verified_str;

    $able_to_proceed_registering_user = TRUE;

    $file_name = $_POST['filename'];

    $old_umask = umask(0);
	mkdir("./users/user_photos", 0777);
	umask($old_umask);

    $target_path = "users/user_photos/"; //here folder name 
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
                if (file_exists("users/user_photos/$file_name")) {
                    chmod("users/user_photos/$file_name", 01777);	// Just a temp solution, not safe (0)
                    echo "The file was found: " . date("d-m-Y h:i:s") . "<br>";
                    break;
                }
        } while(true);

        echo "User Photo Upload Success!";

    } else {
        echo "There was an error uploading the file, please try again!";
        $able_to_proceed_registering_user = FALSE;
    }

    if($able_to_proceed_registering_user == TRUE){
        $fingers_str = shell_exec("python recognize.py users/user_photos/$file_name 2>&1");
        $fingers = (int)$fingers_str;
        echo "Gesture verification result --- Require finger number: $num_of_fingers_to_be_verified, actual finger number: $fingers";
        if($fingers != $num_of_fingers_to_be_verified){
            echo "Gesture verification failed! Require finger number: $num_of_fingers_to_be_verified, but actual finger number: $fingers";
            $able_to_proceed_registering_user = FALSE;
        }
    }

    if($able_to_proceed_registering_user == TRUE){
        $log_msg = shell_exec("python3 add_user.py $email_address $first_name $last_name $password $token");

        echo $log_msg;
    }

}
?>