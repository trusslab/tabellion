<?php
$contract_id = $_POST['contractid'];

$comment_file = "./submitted_files/$contract_id/revision/comment.txt";

$comment_string = file_get_contents($comment_file);

echo $comment_string;

?>

