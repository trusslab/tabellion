<?php
echo "deleting user...";
$email_address = $_POST['email'];

$msg_back = shell_exec("python3 delete_a_user.py \"$email_address\"");

echo $msg_back;
?>
