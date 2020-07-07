<?php
echo "Revoke request received....";

$file = 'status1.txt';
$current = "REVOKED\n";
file_put_contents($file, $current);

?>

