<?php

echo "adding new contract....";
$contract_name = $_POST['contractname'];
$offeror_email = $_POST['offeroremail'];
$offeree_email = $_POST['offereeemail'];

$log_msg = shell_exec("python3 add_contract.py $email_address $first_name $last_name $password $token");

echo $log_msg;

?>