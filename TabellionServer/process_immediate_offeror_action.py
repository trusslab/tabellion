import mysql.connector
import sys
import os
import time

mydb = mysql.connector.connect(
  host="localhost",
  user="myles",
  passwd="Fyqlyx12~",
  database="SignIt"
)

mycursor = mydb.cursor()

# mycursor.execute("CREATE TABLE users (email VARCHAR(255) PRIMARY KEY, firstname VARCHAR(255), lastname VARCHAR(255), password VARCHAR(255), token VARCHAR(255))")

# mycursor.execute("CREATE TABLE contracts (contractid INT AUTO_INCREMENT PRIMARY KEY, contractname VARCHAR(255), offeroremail VARCHAR(255), offereeemail VARCHAR(255))")
mycursor.execute("SELECT contractid, status, offereeemail FROM contracts WHERE offeroremail='" + sys.argv[1] + "'")

myresult = mycursor.fetchall()

offerorToken = os.popen("python3 get_user_token.py " + sys.argv[1]).read()

for contract_info in myresult:
    # print("The user email is:", sys.argv[1], "; and the contract info:", contract_info)
    if int(contract_info[1]) == 2 or int(contract_info[1]) == 11:
        # Tempory way for finishing contract, should try to wait for sgx sinature first
        mycursor.execute("UPDATE contracts SET status = '7' WHERE contractid = '" + str(contract_info[0]) + "'")
        mydb.commit()
        time_log = open("./debug_log/finish_log.txt", "a+")
        time_log.write("Contract ID: " + str(contract_info[0]) + "\n")
        time_log.write("Finish Time: " + str(time.time()) + "\n")
        time_log.write("-------------------------------------------------------------------------\n")
        time_log.close()
        offereeToken = os.popen("python3 get_user_token.py " + contract_info[2]).read()
        os.popen("python FCMmsg_done1.py " + offerorToken)
        os.popen("python FCMmsg_done1.py " + offereeToken)



