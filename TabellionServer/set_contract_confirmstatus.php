<?php
echo "setting contract's confirmstatus....";
$contract_id = $_POST['contractid'];
$confirm_status = $_POST['confirmstatus'];
$offeror_email = shell_exec("python3 get_offeror_email.py $contract_id");
$offeree_email = shell_exec("python3 get_offeree_email.py $contract_id");

echo "python3 set_contract_confirmstatus.py \"$contract_id\" \"$confirm_status\"";

$msg_back = shell_exec("python3 set_contract_confirmstatus.py \"$contract_id\" \"$confirm_status\"");
echo $msg_back;

$token;
if($confirm_status == "1"){
    $token = shell_exec("python3 get_user_token.py \"$offeror_email\"");
    shell_exec("python FCMmsg_offereeConfirmReceivingContract.py \"$token\"");
    
    // Since offeree confirms, we now copy its photo to the contract folder
    shell_exec("cd submitted_files;cp -p ../users/user_photos/User_photo_{$offeree_email}.jpg $contract_id/offeree_info/user_photo.jpg");
    shell_exec("cd submitted_files;cp -p ../users/keys/{$offeree_email}.pem $contract_id/offeree_info/user_public_key.pem");
} else {
    $token = shell_exec("python3 get_user_token.py \"$offeree_email\"");
    shell_exec("python FCMmsg_offerorConfirmSendingContract.py \"$token\"");
    
    // Since offeror confirms, we now copy its photo to the contract folder
    shell_exec("cd submitted_files;cp -p ../users/user_photos/User_photo_{$offeror_email}.jpg $contract_id/offeror_info/user_photo.jpg");
    shell_exec("cd submitted_files;cp -p ../users/keys/{$offeror_email}.pem $contract_id/offeror_info/user_public_key.pem");
}
echo "Message sent!";
?>

