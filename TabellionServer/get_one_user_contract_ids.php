<?php
$user_email = $_POST['email'];

$contract_ids = shell_exec("python3 get_one_user_contract_ids.py \"$user_email\"");

//echo "python3 process_immediate_offeror_action.py \"$user_email\"";

shell_exec("python3 process_immediate_offeror_action.py \"$user_email\"");

echo $contract_ids;
?>

