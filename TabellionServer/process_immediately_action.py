import mysql.connector
import sys
import os

mydb = mysql.connector.connect(
  host="localhost",
  user="myles",
  passwd="Fyqlyx12~",
  database="SignIt"
)

mycursor = mydb.cursor()

# mycursor.execute("CREATE TABLE users (email VARCHAR(255) PRIMARY KEY, firstname VARCHAR(255), lastname VARCHAR(255), password VARCHAR(255), token VARCHAR(255))")

# mycursor.execute("CREATE TABLE contracts (contractid INT AUTO_INCREMENT PRIMARY KEY, contractname VARCHAR(255), offeroremail VARCHAR(255), offereeemail VARCHAR(255))")
mycursor.execute("SELECT contractid, status FROM contracts WHERE offeroremail='" + sys.argv[1] + "' AND offereeemail='" + sys.argv[2] + "'")

myresult = mycursor.fetchall()

offerorToken = os.popen("python3 get_user_token.py " + sys.argv[1]).read()
offereeToken = os.popen("python3 get_user_token.py " + sys.argv[2]).read()

for contract_info in myresult:
    if contract_info[1] == 8:
        mycursor.execute("UPDATE contracts SET status = '3' WHERE contractid = '" + str(contract_info[0]) + "'")
        mydb.commit()
        os.popen("python FCMmsg_revoke_success_offeror.py " + offerorToken)
        os.popen("python FCMmsg_revoke_success_offeree.py " + offereeToken)
    elif contract_info[1] == 2:
        mycursor.execute("UPDATE contracts SET status = '7' WHERE contractid = '" + str(contract_info[0]) + "'")
        mydb.commit()
        os.popen("python FCMmsg_done1.py " + offerorToken)
        os.popen("python FCMmsg_done1.py " + offereeToken)



