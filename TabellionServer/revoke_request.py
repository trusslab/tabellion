import mysql.connector
import sys
from datetime import datetime
import time
import os

# Threshold is in secs
threshold = 30 

mydb = mysql.connector.connect(
  host="localhost",
  user="myles",
  passwd="Fyqlyx12~",
  database="SignIt"
)

mycursor = mydb.cursor()

# mycursor.execute("CREATE TABLE contracts (contractid INT AUTO_INCREMENT PRIMARY KEY, contractname VARCHAR(255), offeroremail VARCHAR(255), offereeemail VARCHAR(255))")

# mycursor.execute("TRUNCATE TABLE contracts")


#sql = "DELETE FROM contracts"
#mycursor.execute(sql)

#mycursor.execute("SELECT email, password FROM users")

#mycursor.execute("SELECT contractid, offeroremail, offereeemail FROM contracts")
#mycursor.execute("ALTER TABLE contracts ADD COLUMN pagecount INT AFTER status;")

#myresult = mycursor.fetchall()

# mycursor.execute("CREATE TABLE users (email VARCHAR(255) PRIMARY KEY, firstname VARCHAR(255), lastname VARCHAR(255), password VARCHAR(255), token VARCHAR(255))")

# mycursor.execute("CREATE TABLE contracts (contractid INT AUTO_INCREMENT PRIMARY KEY, contractname VARCHAR(255), offeroremail VARCHAR(255), offereeemail VARCHAR(255))")
mycursor.execute("SELECT contractid, status, lastactiontimeinterval, offeroremail, offereeemail, confirmstatus FROM contracts")

myresult = mycursor.fetchall()

status = 0
lastActionTimeInterval = ""

offerorToken = ""
offereeToken = ""
offerorEmail = ""
offereeEmail = ""
confirmStatus = 0

for contract_info in myresult:
  if int(sys.argv[1]) == contract_info[0]:
      status = contract_info[1]
      lastActionTimeInterval = contract_info[2]
      offerorEmail = contract_info[3]
      offereeEmail = contract_info[4]
      print("The confirmStatus is:", contract_info[5])
      confirmStatus = contract_info[5]
      offerorToken = os.popen("python3 get_user_token.py " + offerorEmail).read()
      offereeToken = os.popen("python3 get_user_token.py " + offereeEmail).read()
      print("The offereeToeken is:", offereeToken)


print("current_request_time_interval:", sys.argv[2])
print("last_action_time_interval:", lastActionTimeInterval)

# Cannot read a file in guest session, so we give up
'''
print("Going to open threshold file")
thresholdFile = open("status_change_threshold.txt", "r")
print("Going to read threshold")
threshold = int(thresholdFile.readline())
print("Going to print threshold")
'''

print("Current threshold is:", threshold)

previousTimeIntervalStart = None
previousTimeIntervalEnd = None

def try_revoke():
    sql = "UPDATE contracts SET status = '8' WHERE contractid = '" + sys.argv[1] + "'"
    mycursor.execute(sql)
    mycursor.execute("UPDATE contracts SET lastactiontimeinterval = '" + sys.argv[2] + "' WHERE contractid = '" + sys.argv[1] + "'")
    mydb.commit()
    os.popen("python FCMmsg_revoke_processing_offeror.py " + offerorToken).read()
    time.sleep(threshold)
    mycursor.execute("SELECT status FROM contracts WHERE contractid = '" + sys.argv[1] + "'")
    myresult = mycursor.fetchall()
    status = myresult[0][0]
    print("MyResult:", myresult, "; status:", status)
    if status == 8:
        print("Going to make contract revoked!")
        sql = "UPDATE contracts SET status = '3' WHERE contractid = '" + sys.argv[1] + "'"
        mycursor.execute(sql)
        mydb.commit()
        mycursor.execute("SELECT status FROM contracts WHERE contractid = '" + sys.argv[1] + "'")
        myresult = mycursor.fetchall()
        print("After update:", myresult)
        os.popen("python FCMmsg_revoke_success_offeror.py " + offerorToken).read()
        os.popen("python FCMmsg_revoke_success_offeree.py " + offereeToken).read()

def sync_status():
    os.popen("python process_immediately_action.py " + offerorEmail + " " + offereeEmail)

if lastActionTimeInterval != None:
    tempString = lastActionTimeInterval.split("to")
    print("We now have", tempString)
    previousTimeIntervalStart = datetime.strptime(tempString[0], "%Y-%m-%d-%H-%M-%S")
    previousTimeIntervalEnd = datetime.strptime(tempString[1], "%Y-%m-%d-%H-%M-%S")
    print("We get:", previousTimeIntervalStart, " and ", previousTimeIntervalEnd)

sql = ""
start_time = time.time()
if (status in [0, 5, 10]) or (confirmStatus in [0, 1]):
    print("First circumstance")
    sql = "UPDATE contracts SET status = '3' WHERE contractid = '" + sys.argv[1] + "'"
    mycursor.execute(sql)
    mydb.commit()
    time_log = open("./debug_log/revoke_log.txt", "a+")
    time_log.write("Contract ID: " + sys.argv[1] + "\n")
    time_log.write("Revoke TimeInterval: " + sys.argv[2] + "\n")
    time_log.write("Time for execution: " + str(time.time() - start_time) + "\n")
    time_log.write("-------------------------------------------------------------------------\n")
    time_log.close()
    os.popen("python FCMmsg_revoke_success_offeror.py " + offerorToken).read()
    os.popen("python FCMmsg_revoke_success_offeree.py " + offereeToken).read()    # Should we notify the offeree even if the offeror has not signed yet?
elif status in [1, 2, 9]:
    print("Second circumstance")
    if previousTimeIntervalStart != None:
        print("Second circumstance First Part")
        tempString = sys.argv[2].split("to")
        currentTimeIntervalStart = datetime.strptime(tempString[0], "%Y-%m-%d-%H-%M-%S")
        currentTimeIntervalEnd = datetime.strptime(tempString[1], "%Y-%m-%d-%H-%M-%S")
        print("Trying to compare, previousTimeInervalStart:", previousTimeIntervalStart, " with currentTimeIntervalStart:", currentTimeIntervalStart)
        if currentTimeIntervalStart < previousTimeIntervalStart or status == 1:
            print("Second circumstance First Part First section")
            try_revoke()
        else:
            print("Second circumstance First Part Second section")
            # Should also do corresponding action for status 2, 9
            os.popen("python FCMmsg_time_protect_earlier_action_detected.py " + offerorToken).read()
            sync_status()
    else:
        print("Second circumstance Second Part")
        try_revoke()
else:
    print("Third circumstance")
    os.popen("python FCMmsg_revoke_fail_offeror.py " + offerorToken).read()

mydb.commit()