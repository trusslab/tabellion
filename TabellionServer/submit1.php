<?php
echo "submitting....";
$contract_name = $_POST['contractname'];
$offeror_email = $_POST['offeroremail'];
$offeree_email = $_POST['offereeemail'];
$description = $_POST['description'];
$status = (int)$_POST['status'];
$confirm_status = (int)$_POST['confirmstatus'];
$contract_id = $_POST['contractid'];
$fms_token = $_POST['fms_token'];

echo "start rendering...";

$width_of_device = 1080;
$height_of_device = 1920;

// If it is from hikey...
if($offeror_email == "saeed@saeed.com"){
    echo "Hikey detected...";
    $width_of_device = 1920;
    $height_of_device = 1080;
}

$width_of_note = $width_of_device;
$height_of_note = 134;   # Note if this get changed, the corresponding one in the checker.py also needs to be changed

$width_of_temp_img = $width_of_device;
$height_of_temp_img = $height_of_device - $height_of_note;

// For tracking how much time it taks to render the contract (Start)
$time_pre = microtime(true);

//$log_data = shell_exec("cd submitted_files/$contract_id;./all.sh");

$cpu_stat = '/proc/stat';
$cpu_stat_content = file_get_contents($cpu_stat);

$stop_pos = strpos($cpu_stat_content, "intr");

$real_cpu_stat_content_start = substr($cpu_stat_content, 0, $stop_pos);

exec("cd submitted_files/$contract_id;xvfb-run -a wkhtmltoimage --height $height_of_note --width $width_of_note note.html note.png > /dev/null &");
//exec("cd submitted_files/$contract_id;xvfb-run -a wkhtmltoimage --height $height_of_note --width $width_of_note review_note.html review_note.png > /dev/null &");

$time_first = microtime(true);

shell_exec("cd submitted_files/$contract_id;python mdtohtml.py > doc.html");

$time_second = microtime(true);

$style_txt = "./submitted_files/$contract_id/style.txt";
$style_txt_content = "<body style=\"margin:0px;padding:0px\">
<meta name=\"viewport\" content=\"width=$width_of_temp_img, height=$height_of_temp_img\">

<style>
h1 { 
    font-size: 5em;
    margin-top: 0px;
    margin-bottom: 0px;
    margin-left: 40px;
    margin-right: 40px;
    line-height: 130px;
}
h2 { 
    font-size: 4em;
    margin-top: 0px;
    margin-bottom: 0px;
    margin-left: 40px;
    margin-right: 40px;
    line-height: 180px;
}
h3 { 
    font-size: 3em;
    margin-top: 0px;
    margin-bottom: 0px;
    margin-left: 40px;
    margin-right: 40px;
    line-height: 150px;
}
p { 
    font-size: 42px;
    margin-top: 0px;
    margin-bottom: 0px;
    margin-left: 40px;
    margin-right: 40px;
    line-height: 60px;
}
li { 
    font-size: 40px;
    margin-top: 0px;
    margin-bottom: 0px;
    margin-left: 40px;
    margin-right: 40px;
    line-height: 60px;
}

</style>


";
file_put_contents($style_txt, $style_txt_content);



shell_exec("cd submitted_files/$contract_id;cat style.txt doc.html > Ndoc.html");

$time_third = microtime(true);

//$width_of_temp_img_in_mm = $width_of_temp_img * 0.271;
//$height_of_temp_img_in_mm = $height_of_temp_img * 0.271;

$width_of_temp_img_in_mm = $width_of_temp_img * 0.18;
$height_of_temp_img_in_mm = $height_of_temp_img * 0.18;

/*
if($offeror_email == "saeed@saeed.com"){
    shell_exec("cd submitted_files/$contract_id;xvfb-run -a wkhtmltopdf --page-width $width_of_temp_img_in_mm --page-height $height_of_temp_img_in_mm --header-center 'Page [page]/[toPage]' --header-font-size 25 --header-spacing 0 --header-line --header-font-name Helvetica Ndoc.html doc.pdf");
} else {
    shell_exec("cd submitted_files/$contract_id;xvfb-run -a wkhtmltopdf --header-center 'Page [page]/[toPage]' --header-font-size 25 --header-spacing 0 --header-line --header-font-name Helvetica Ndoc.html doc.pdf");
}
*/
shell_exec("cd submitted_files/$contract_id;xvfb-run -a wkhtmltopdf --page-width $width_of_temp_img_in_mm --page-height $height_of_temp_img_in_mm --header-center 'Page [page]/[toPage]' --header-font-size 25 --header-spacing 0 --header-line --header-font-name Helvetica Ndoc.html doc.pdf");

$time_forth = microtime(true);

shell_exec("cd submitted_files/$contract_id;pdftoppm -scale-to-x $width_of_temp_img -scale-to-y $height_of_temp_img doc.pdf doc -png");

//shell_exec("cd submitted_files/$contract_id;python mdtohtml.py > doc.html");
// For tracking how much time it taks to render the contract (Middle)

# echo $log_data;

/*
$count_of_images = 1;
$file_name_for_check = "./submitted_files/$contract_id/doc-$count_of_images.png";
while(file_exists($file_name_for_check)){
	$count_of_images++;
	$file_name_for_check = "./submitted_files/$contract_id/doc-$count_of_images.png";
}


if($count_of_images != 1){
	$count_of_images--;
}
*/

$time_fifth = microtime(true);

$string = 'doc-';
$imageArray = array();

$dir = new DirectoryIterator("./submitted_files/$contract_id");
foreach ($dir as $file) {
    $file_name = (string)$file;
    if(strpos($file_name, "doc-") !== false  and strpos($file_name, ".png") !== false){
        array_push($imageArray, $file_name);
    }
}

$count_of_images = count($imageArray);

sort($imageArray);

$temp_counter_for_image = 0;

foreach ($imageArray as $file){
    ++$temp_counter_for_image;
	exec("cd submitted_files/$contract_id;python checker.py $file $temp_counter_for_image $width_of_device $height_of_device > /dev/null &");
}
exec("cd submitted_files/$contract_id;python checker.py last-1.png 1 $width_of_device $height_of_device > /dev/null &");

$cpu_stat_content = file_get_contents($cpu_stat);

$stop_pos = strpos($cpu_stat_content, "intr");

$real_cpu_stat_content_finish = substr($cpu_stat_content, 0, $stop_pos);

$time_mid = microtime(true);

// For tracking how much time it taks to render the contract (End)
$time_post = microtime(true);
$exec_time = $time_post - $time_pre;
$first_part_time = $time_first - $time_pre;
$seoncd_part_time = $time_second - $time_first;
$third_part_time = $time_third - $time_second;
$forth_part_time = $time_forth - $time_third;
$fifth_part_time = $time_fifth - $time_forth;
$sixth_part_time = $time_mid - $time_fifth;
$time_log = './debug_log/render_log.txt';
$current = file_get_contents($time_log);
$current .= "Contract ID: " . $contract_id . "\n";
$current .= "Contract Name: " . $contract_name . "\n";
$current .= "Count of Images: " . $count_of_images . "\n";
$current .= "Render time(first part(Not useful anymore)): " . $first_part_time . "\n";
$current .= "Render time(second part): " . $seoncd_part_time . "\n";
$current .= "Render time(third part): " . $third_part_time . "\n";
$current .= "Render time(forth part): " . $forth_part_time . "\n";
$current .= "Render time(fifth part): " . $fifth_part_time . "\n";
$current .= "Render time(mid part): " . $sixth_part_time . "\n";
$current .= "Render time(total): " . $exec_time . "\n";
$current .= "CPU stat(Before): " . "\n" . $real_cpu_stat_content_start . "\n";
$current .= "CPU stat(After): " . "\n" . $real_cpu_stat_content_finish . "\n";
$current .= "-------------------------------------------------------------------------\n";
file_put_contents($time_log, $current);

$msg_contracts_db = shell_exec("python3 add_contract.py \"$contract_name\" $offeror_email $offeree_email \"$description\" $status $count_of_images $confirm_status");

echo $msg_contracts_db;
shell_exec("./FCMmsg_rendered1.py $fms_token");
echo "Message sent!$count_of_images";

?>

