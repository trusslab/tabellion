<?php
$contract_id = $_POST['contractid'];

$page_count = shell_exec("python3 get_page_count.py $contract_id");
$contract_name = shell_exec("python3 get_contract_name.py $contract_id");
$description = shell_exec("python3 get_description.py $contract_id");
$status = shell_exec("python3 get_contract_status.py $contract_id");
$offeror_email = shell_exec("python3 get_offeror_email.py $contract_id");
$offeree_email = shell_exec("python3 get_offeree_email.py $contract_id");
$confirm_status = shell_exec("python3 get_contract_confirmstatus.py $contract_id");

$info_sep = "#This is for seperating the info#";

echo $contract_name;
echo $info_sep;
echo $description;
echo $info_sep;
echo $status;
echo $info_sep;
echo $page_count;
echo $info_sep;
echo $offeror_email;
echo $info_sep;
echo $offeree_email;
echo $info_sep;
echo $confirm_status;
?>

