<?php

echo "trying sign in....";
$email_address = $_POST['email'];
$password = $_POST['password'];

$log_msg = shell_exec("python3 try_sign_in.py $email_address $password");
echo "Going to print some message back!";
echo $log_msg;

?>