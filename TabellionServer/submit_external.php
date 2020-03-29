<?php
//echo "submitting....";
$contract_name = $_POST['contractname'];
$offeror_email = $_POST['offeroremail'];
$offeree_email = $_POST['offereeemail'];
$description = $_POST['description'];
$status = (int)$_POST['status'];
$confirm_status = (int)$_POST['confirmstatus'];
$fms_token = $_POST['fms_token'];
$contract_content = $_POST['contractcontent'];

$contract_id = shell_exec("python3 get_contracts_new_row_id.py");

//echo "contract_id" . $contract_id;

$old_umask = umask(0);
$result = mkdir("./submitted_files/$contract_id", 0777, true);
//echo "Is it? " . $result . ";;;;";
umask($old_umask);

$original_contract_file_path = "./submitted_files/$contract_id/original_contract.txt";
$original_contract_file_content = file_get_contents($original_contract_file_path);

$identifier = "-@@-";

$contract_content_array = explode($identifier, $contract_content);

for($i = 0; $i < count($contract_content_array); $i++){
    $original_contract_file_content .= $contract_content_array[$i] . "\n";
}

file_put_contents($original_contract_file_path, $original_contract_file_content);

$confirm_status = 0;
$count_of_images = 0;
//echo "\n" . "python3 add_contract.py \"$contract_name\" $offeror_email $offeree_email \"$description\" $status $count_of_images $confirm_status" . "\n";

$msg_contracts_db = shell_exec("python3 add_contract.py \"$contract_name\" $offeror_email $offeree_email \"$description\" $status $count_of_images $confirm_status");

if(strpos($msg_contracts_db, 'has been inserted!')){
    echo "success";
} else {
    echo "failure";
}


//echo $msg_contracts_db;

?>