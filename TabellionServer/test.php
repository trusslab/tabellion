<?php

$cpu_stat = '/proc/stat';
$cpu_stat_content = file_get_contents($cpu_stat);

$stop_pos = strpos($cpu_stat_content, "intr");

$real_cpu_stat_content = substr($cpu_stat_content, 0, $stop_pos);

echo $real_cpu_stat_content;

?>