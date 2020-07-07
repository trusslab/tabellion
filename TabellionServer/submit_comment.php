<?php
if(isset($_FILES['screenshot']) && isset($_FILES['signature'])){
    $contract_id = $_POST['contractid'];
    $comment_string = $_POST['comment_string'];

    $user_email = shell_exec("python3 get_offeree_email.py $contract_id");

    $old_umask = umask(0);
    mkdir("./submitted_files/$contract_id/revision", 0777);
    umask($old_umask);

    $target_path_screenshot = "./submitted_files/$contract_id/revision/"; //here folder name 
    $target_path_screenshot = $target_path_screenshot . basename($_FILES['screenshot']['name']);
    
    $target_path_signature = "./submitted_files/$contract_id/revision/"; //here folder name 
    $target_path_signature = $target_path_signature . basename($_FILES['signature']['name']);

    if(move_uploaded_file($_FILES['screenshot']['tmp_name'], $target_path_screenshot)) {
        if(move_uploaded_file($_FILES['signature']['tmp_name'], $target_path_signature)) {
            echo "Signature file has been uploaded";
			shell_exec("cd submitted_files/$contract_id/revision/;openssl base64 -d -in " . basename($_FILES['signature']['name']) . " -out sign.sha256");
			chmod("submitted_files/$contract_id/revision/sign.sha256", 01777);	// Just a temp solution, not safe (2)	
            chmod($target_path_screenshot, 01777);	// Just a temp solution, not safe (3)	
            $output = shell_exec("cd submitted_files/$contract_id/revision/;
			openssl dgst -sha256 -verify ../../../users/keys/$user_email.pem -signature sign.sha256 " . basename($_FILES['screenshot']['name']));
			if(substr($output, 0, 11) == "Verified OK"){
                echo "photo verified!!!";
                $comment_file = "./submitted_files/$contract_id/revision/comment.txt";

                file_put_contents($comment_file, $comment_string);

                echo "comment string is successfully stored...\n";

                $status_set_result = shell_exec("python3 set_contract_status.py $contract_id 5");

                $offeror_email = shell_exec("python3 get_offeror_email.py $contract_id");

                $offeror_token = shell_exec("python3 get_user_token.py $offeror_email");

                $send_result = shell_exec("python3 FCMmsg_offereeLeaveCommentForOfferor.py $offeror_token");

                echo "message sent to $offeror_email, whose token is $offeror_token\n";
			} else {
                echo "verification failure!!!";
            }
        }
    }
}

?>

