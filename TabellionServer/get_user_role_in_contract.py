import mysql.connector
import sys

mydb = mysql.connector.connect(
  host="localhost",
  user="myles",
  passwd="Fyqlyx12~",
  database="SignIt"
)

mycursor = mydb.cursor()

# mycursor.execute("CREATE TABLE users (email VARCHAR(255) PRIMARY KEY, firstname VARCHAR(255), lastname VARCHAR(255), password VARCHAR(255), token VARCHAR(255))")

# mycursor.execute("CREATE TABLE contracts (contractid INT AUTO_INCREMENT PRIMARY KEY, contractname VARCHAR(255), offeroremail VARCHAR(255), offereeemail VARCHAR(255))")
mycursor.execute("SELECT offeroremail, offereeemail FROM contracts WHERE contractid ='" + sys.argv[1] + "'")

myresult = mycursor.fetchall()

# Print 0 if the user is offeror, otherwise print 1

for contract_info in myresult:
    if contract_info[0] == sys.argv[2]:
        print("0", end="")
    elif contract_info[1] == sys.argv[2]:
        print("1", end="")



