<?php
echo "Revoke request received....";

$contract_id = $_POST['contractid'];
$time_interval = $_POST['timeinterval'];

echo "python3 revoke_request.py \"$contract_id\" \"$time_interval\"";

$msg_back = shell_exec("python3 revoke_request.py \"$contract_id\" \"$time_interval\"");

echo $msg_back;

?>

