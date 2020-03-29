<?php

echo "checking and processing....";
$contract_id = $_POST['contractid'];
$last_screenshot_num = $_POST['lastscreenshotid'];
$user_email = $_POST['useremail'];
$current_time_interval = $_POST['timeinterval'];
$last_screenshot_num_i = (int)$last_screenshot_num;
$offeror_email = shell_exec("python3 get_offeror_email.py \"$contract_id\"");
$offeree_email = shell_exec("python3 get_offeree_email.py \"$contract_id\"");
$offeror_token = shell_exec("python3 get_user_token.py \"$offeror_email\"");
$offeree_token = shell_exec("python3 get_user_token.py \"$offeree_email\"");
$status = shell_exec("python3 get_contract_status.py \"$contract_id\"");
$current_role = "offeree";
if($user_email == $offeror_email){
    $current_role = "offeror";
    shell_exec("python3 set_contract_status.py \"$contract_id\" 10");
} else {
    shell_exec("python3 set_contract_status.py \"$contract_id\" 11");
}

$time_pre = microtime(true);

$cpu_stat = '/proc/stat';
$cpu_stat_content = file_get_contents($cpu_stat);

$stop_pos = strpos($cpu_stat_content, "intr");

$real_cpu_stat_content_start = substr($cpu_stat_content, 0, $stop_pos);

$is_verified = True;
echo "Going to verify signature...";
for($x = 1; $x <= $last_screenshot_num_i; $x++){
    shell_exec("cd submitted_files/$contract_id/$current_role/;openssl base64 -d -in signature" . $x . " -out sign.sha256");
    chmod("submitted_files/$contract_id/$current_role/sign.sha256", 01777);	// Just a temp solution, not safe (2)	
    chmod("submitted_files/$contract_id/$current_role/screenshot$x", 01777);	// Just a temp solution, not safe (3)	
    // (Temp fixed by above lines) We have a permission bug needs to be fixed: If I do not manually make the sign.sha256 to be writeable by public, 
    // this shell_exec will not work since it needs to overwrite an existing file.
    echo "current status:" . $x . "\n";
    echo "cd submitted_files/$contract_id/$current_role/;openssl dgst -sha256 -verify ../../../users/keys/$offeror_email.pem -signature sign.sha256 screenshot$x";
    if($status == 0){
        $output = shell_exec("cd submitted_files/$contract_id/$current_role/;openssl dgst -sha256 -verify ../../../users/keys/$offeror_email.pem -signature sign.sha256 screenshot$x");
    } else {
        $output = shell_exec("cd submitted_files/$contract_id/$current_role/;openssl dgst -sha256 -verify ../../../users/keys/$offeree_email.pem -signature sign.sha256 screenshot$x");
    }
    echo $output;
    echo "...end of current status...";
    if(substr($output, 0, 11) != "Verified OK"){
        $is_verified = False;
        break;
    }
}
$time_signature_verified = microtime(true);

if($is_verified){
    echo "Going to check screenshots...";
    echo "Going to run: " . "python3 check_images_identity.py \"./submitted_files/$contract_id\" \"./submitted_files/$contract_id/$current_role\"";
    $result = shell_exec("python3 check_images_identity.py \"./submitted_files/$contract_id\" \"./submitted_files/$contract_id/$current_role\"");
    echo "the result: " . $result;
    if($result != "True"){
        $is_verified = False;
    }
}
$time_screenshot_verified = microtime(true);

if($is_verified){
    $page_count = shell_exec("python3 get_page_count.py $contract_id");
    $contract_name = shell_exec("python3 get_contract_name.py $contract_id");
    $description = shell_exec("python3 get_description.py $contract_id");
    
    echo "Going to send message";

    // The following codes should be changed so that the contract could be rovoked by offeror or refused by offeree
    if($status == "0"){
        echo "Now is offeror status";
        shell_exec("python3 set_contract_status.py \"$contract_id\" \"1\"");
        shell_exec("python FCMmsg_offeror_signed.py $offeror_token");
        shell_exec("python FCMmsg_rendered2.py $offeree_token $contract_id \"$contract_name\" \"$description\" $status $page_count $offeror_email $offeree_email");
    } else {
        echo "Now is offeree status";
        shell_exec("python3 done_request.py \"$contract_id\" \"$current_time_interval\"");
    }
} else {
    if($user_email == $offeror_email){
        shell_exec("python3 set_contract_status.py \"$contract_id\" 0");
    } else {
        shell_exec("python3 set_contract_status.py \"$contract_id\" 1");
    }
    shell_exec("python FCMmsg_offerorVerificationFailed1.py $offeror_token;python FCMmsg_offerorVerificationFailed2.py $offeree_token");
}

$time_last = microtime(true);

$cpu_stat_content = file_get_contents($cpu_stat);

$stop_pos = strpos($cpu_stat_content, "intr");

$real_cpu_stat_content_finish = substr($cpu_stat_content, 0, $stop_pos);

$exec_time_signature = $time_signature_verified - $time_pre;
$exec_time_screenshot = $time_screenshot_verified - $time_signature_verified;
$exec_time_total = $time_last - $time_pre;

$time_log = './debug_log/verification_log.txt';
$current = file_get_contents($time_log);
$current .= "Contract ID: " . $contract_id . "\n";
$current .= "Last screenshot #: " . $last_screenshot_num . "\n";
$current .= "Signature verification time(first part): " . $exec_time_signature . "\n";
$current .= "Screenshot verification time(second part): " . $exec_time_screenshot . "\n";
$current .= "Verification time(total): " . $exec_time_total . "\n";
$current .= "CPU stat(Before): " . "\n" . $real_cpu_stat_content_start . "\n";
$current .= "CPU stat(After): " . "\n" . $real_cpu_stat_content_finish . "\n";
$current .= "-------------------------------------------------------------------------\n";
file_put_contents($time_log, $current);

echo "finish...";

?>