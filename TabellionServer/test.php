<?php

echo "Going to run...\n";

$first_run_result = shell_exec("cd sgx_codes/Linux/sgx/test_app_saeed_plus; sudo ./TestApp > /dev/null &");

echo $first_run_result;
echo "Here we go ...\n";

$run_result = shell_exec("cd sgx_codes/Linux/sgx/test_app_saeed_plus/run_enclave; ./client 127.0.0.1 2");

echo "We got it!\n";
echo $run_result;

?>