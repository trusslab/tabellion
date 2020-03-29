<?php
//echo "submitting....";
$contract_name = $_POST['contractname'];
$total_screenshots_counter = $_POST['countofscreeenshots'];
$user_email = $_POST['userEmail'];
$current_role = $_POST['current_role'];

$is_exist = shell_exec("python3 check_if_contract_name_exist.py \"$contract_name\" \"$total_screenshots_counter\"");
$contract_id = shell_exec("python3 get_contract_id_by_contract_name.py $contract_name");
echo "python3 set_contract_email.py \"$contract_id\" \"$current_role\" \"$user_email\"";
shell_exec("python3 set_contract_email.py \"$contract_id\" \"$current_role\" \"$user_email\"");

echo $contract_id;
echo $is_exist;

?>