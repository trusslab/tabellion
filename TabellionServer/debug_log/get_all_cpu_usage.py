import sys

file = open(sys.argv[1])

content = file.readlines()

result = []

status = 0

def get_two_times(input_line):
    result = []
    result.append(int(input_line[2]) + int(input_line[3]) + int(input_line[4]) \
        + int(input_line[5]) + int(input_line[6]) + int(input_line[7]) + int(input_line[8]) + int(input_line[9]))
    result.append(int(input_line[2]) + int(input_line[3]) + int(input_line[4]) \
        + int(input_line[6]) + int(input_line[7]) + int(input_line[8]) + int(input_line[9]))
    return result

first_time = []
second_time = []

for line in content:
    if "CPU stat(Before):" in line:
        status = 1
        continue
    elif status == 1:
        temp_line_list = line.split(" ")
        status = 2
        first_time = get_two_times(temp_line_list)
    elif status == 2 and "CPU stat(After):" in line:
        status = 3
    elif status == 3:
        temp_line_list = line.split(" ")
        status = 0
        second_time = get_two_times(temp_line_list)
        result.append((second_time[1] - first_time[1]) / (second_time[0] - first_time[0]) * 100)

print(result)
