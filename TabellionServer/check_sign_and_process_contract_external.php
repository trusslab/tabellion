<?php

// This file has used curl plugin for PHP
// Part of codes credit to: https://stackoverflow.com/questions/1217824/post-to-another-page-within-a-php-script

echo "checking and processing....";
$contract_id = $_POST['contractid'];
$last_screenshot_num = $_POST['lastscreenshotid'];
$user_email = $_POST['useremail'];
$current_time_interval = $_POST['timeinterval'];
$current_role = $_POST['current_role'];
$last_screenshot_num_i = (int)$last_screenshot_num;

$target_server_address = "http://128.195.54.65/tabellion_plus_demo_server/set_contract_verification_result.php";

// Set corresponding email address
//shell_exec("python3 set_contract_email.py \"$contract_id\" \"$current_role\" \"$user_email\"");

if($current_role == "offeror"){
    shell_exec("python3 set_contract_status.py \"$contract_id\" 18");
} else {
    shell_exec("python3 set_contract_status.py \"$contract_id\" 13");
}

$contract_name = shell_exec("python3 get_contract_name.py \"$contract_id\"");

// Prepare for ocs_checker.py
shell_exec("cd submitted_files;cp -p ocr_checker.py $contract_id/$current_role");

$is_verified = True;
echo "Going to verify signature...";
for($x = 1; $x <= $last_screenshot_num_i; $x++){
    shell_exec("cd submitted_files/$contract_id/$current_role/;openssl base64 -d -in signature" . $x . " -out sign.sha256");
    chmod("submitted_files/$contract_id/$current_role/sign.sha256", 01777);	// Just a temp solution, not safe (2)	
    chmod("submitted_files/$contract_id/$current_role/screenshot$x", 01777);	// Just a temp solution, not safe (3)	
    // (Temp fixed by above lines) We have a permission bug needs to be fixed: If I do not manually make the sign.sha256 to be writeable by public, 
    // this shell_exec will not work since it needs to overwrite an existing file.
    echo "current status:" . $x . "\n";

    echo "cd submitted_files/$contract_id/$current_role/;openssl dgst -sha256 -verify ../../../users/keys/$user_email.pem -signature sign.sha256 screenshot$x" . "\n";

    $output = shell_exec("cd submitted_files/$contract_id/$current_role/;openssl dgst -sha256 -verify ../../../users/keys/$user_email.pem -signature sign.sha256 screenshot$x");

    echo $output;
    echo "...end of current status...";
    if(substr($output, 0, 11) != "Verified OK"){
        $is_verified = False;
        break;
    }

    $original_contract_by_offeror_path = "./submitted_files/$contract_id/original_contract(signed_by_offeror)$x.txt";

    if($is_verified && $current_role == "offeror"){
        echo "Going to extract text...";
        $result = shell_exec("cd submitted_files/$contract_id/$current_role/;python3 ocr_checker.py \"./screenshot$x\"");
        echo "OCR Result(offeror): " . $result;
        $original_contract_by_offeror_content = file_get_contents($original_contract_by_offeror_path);
        file_put_contents($original_contract_by_offeror_path, $result);
    }

    if($is_verified && $current_role == "offeree"){
        echo "Going to check screenshots...";
        $original_contract_by_offeror_content = file_get_contents($original_contract_by_offeror_path);
        $result = shell_exec("cd submitted_files/$contract_id/$current_role/;python3 ocr_checker.py \"./screenshot$x\"");
        echo "OCR Result(offeree): " . $result;
        if($original_contract_by_offeror_content == $result){
            echo "OCR check passed!";
        } else {
            echo "OCR check not passed!";
            $is_verified = False;
            break;
        }
        //echo "Going to run: " . "python3 check_images_identity.py \"./submitted_files/$contract_id\" \"./submitted_files/$contract_id/$current_role\"";
        //$result = shell_exec("python3 check_images_identity.py \"./submitted_files/$contract_id\" \"./submitted_files/$contract_id/$current_role\"");
        //echo "the result: " . $result;
        /*
        if($result != "True"){
            $is_verified = False;
        }
        */
    }

}

$msg_to_target_server = $current_role;

if($is_verified){

    if($current_role == "offeror"){
        shell_exec("python3 set_contract_status.py \"$contract_id\" 12");
    } else {
        shell_exec("python3 set_contract_status.py \"$contract_id\" 15");
    }
    
    $msg_to_target_server .= ":true";

} else {

    $msg_to_target_server .= ":false";
    if($current_role == "offeror"){
        shell_exec("python3 set_contract_status.py \"$contract_id\" 17");
    } else {
        shell_exec("python3 set_contract_status.py \"$contract_id\" 12");
    }

}

echo "Going to send message";

$url = $target_server_address;

// what post fields?
$fields = array(
    'contractname' => $contract_name,
    'verificationresult' => $msg_to_target_server,
    'identity' => $user_email
 );
 
 // build the urlencoded data
 $postvars = http_build_query($fields);
 
 // open connection
 $ch = curl_init();
 
 // set the url, number of POST vars, POST data
 curl_setopt($ch, CURLOPT_URL, $url);
 curl_setopt($ch, CURLOPT_POST, count($fields));
 curl_setopt($ch, CURLOPT_POSTFIELDS, $postvars);
 
 // execute post
 $result = curl_exec($ch);
 
 // close connection
 curl_close($ch);

echo "finish...";

?>