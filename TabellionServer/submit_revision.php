<?php
echo "submitting revision....";
$contract_id = $_POST['contractid'];

$offeror_email = shell_exec("python3 get_offeror_email.py $contract_id");

echo "start rendering...";

$width_of_device = 1080;
$height_of_device = 1920;

// If it is from hikey...
if($offeror_email == "saeed@saeed.com"){
    echo "Hikey detected...\n";
    $width_of_device = 1920;
    $height_of_device = 1080;
}

$width_of_note = $width_of_device;
$height_of_note = 134;   # Note if this get changed, the corresponding one in the checker.py also needs to be changed

$width_of_temp_img = $width_of_device;
$height_of_temp_img = $height_of_device - $height_of_note;

// For tracking how much time it takes to render the contract (Start)
$time_pre = microtime(true);

//$log_data = shell_exec("cd submitted_files/$contract_id;./all.sh");

$cpu_stat = '/proc/stat';
$cpu_stat_content = file_get_contents($cpu_stat);

$stop_pos = strpos($cpu_stat_content, "intr");

$real_cpu_stat_content_start = substr($cpu_stat_content, 0, $stop_pos);

exec("cd submitted_files/$contract_id/revision;xvfb-run -a wkhtmltoimage --height $height_of_note --width $width_of_note note.html note.png > /dev/null &");
//exec("cd submitted_files/$contract_id;xvfb-run -a wkhtmltoimage --height $height_of_note --width $width_of_note review_note.html review_note.png > /dev/null &");

$time_first = microtime(true);

shell_exec("cd submitted_files/$contract_id/revision;python mdtohtml.py > doc.html");

$time_second = microtime(true);

$style_txt = "./submitted_files/$contract_id/revision/style.txt";
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



shell_exec("cd submitted_files/$contract_id/revision;cat style.txt doc.html > Ndoc.html");

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
shell_exec("cd submitted_files/$contract_id/revision;xvfb-run -a wkhtmltopdf --page-width $width_of_temp_img_in_mm --page-height $height_of_temp_img_in_mm --header-center 'Page [page]/[toPage]' --header-font-size 25 --header-spacing 0 --header-line --header-font-name Helvetica Ndoc.html doc.pdf");

$time_forth = microtime(true);

shell_exec("cd submitted_files/$contract_id/revision;pdftoppm -scale-to-x $width_of_temp_img -scale-to-y $height_of_temp_img doc.pdf doc -png");

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

$dir = new DirectoryIterator("./submitted_files/$contract_id/revision");
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
    exec("cd submitted_files/$contract_id/revision;python checker.py $file $temp_counter_for_image $width_of_device $height_of_device > /dev/null &");
}
exec("cd submitted_files/$contract_id/revision;python checker.py last-1.png 1 $width_of_device $height_of_device > /dev/null &");

$temp_counter_for_image = $temp_counter_for_image / 2;      // For some unknown reason, Ndoc and doc are being counted together...

shell_exec("python3 set_contract_total_pages.py $contract_id $temp_counter_for_image");

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
$current .= "Contract ID(R): " . $contract_id . "\n";
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

$fms_token = shell_exec("python3 get_user_token.py $contract_id");

$possible_signed_pages = shell_exec("python3 compare_two_doc_pdf.py ./submitted_files/$contract_id/revision/doc.pdf ./submitted_files/$contract_id/doc.pdf");
$current_signed_pages = shell_exec("python3 get_contract_signed_pages.py $contract_id");

echo "possible_signed_pages: $possible_signed_pages\n";
echo "current_signed_pages: $current_signed_pages\n";

$should_continue_set_signed_pages = TRUE;
$possible_signed_pages_list = [];

if($possible_signed_pages == ""){
    $signed_pages_set_result = shell_exec("python3 set_contract_signed_pages.py $contract_id None");
    echo $signed_pages_set_result;
    $should_continue_set_signed_pages = FALSE;
} else {
    $possible_signed_pages_list = explode("%", $possible_signed_pages);
}

$current_signed_pages_list = [];

if($should_continue_set_signed_pages){
    if($current_signed_pages == "None" || $current_signed_pages == ""){
        $should_continue_set_signed_pages = FALSE;
    } else {
        $current_signed_pages_list = explode("%", $current_signed_pages);
        $final_signed_pages_list = array_intersect($possible_signed_pages_list, $current_signed_pages_list);
        $final_signed_pages_string = implode("%", $final_signed_pages_list);
        $signed_pages_set_result = shell_exec("python3 set_contract_signed_pages.py $contract_id $final_signed_pages_string");
        echo $signed_pages_set_result;
    }
}

shell_exec("python3 set_contract_revised_count.py $contract_id 1");

shell_exec("python3 set_contract_status.py $contract_id 0");
shell_exec("python ./FCMmsg_rendered1.py $fms_token");
echo "Message sent!";

?>

