<?php
echo "getting....";
$user_email = $_POST['email'];
$contract_id = $_POST['contractid'];

$contract_ids = shell_exec("python3 get_user_role_in_contract.py $contract_id \"$user_email\"");

echo $contract_ids;
?>

