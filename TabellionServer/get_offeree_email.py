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
mycursor.execute("SELECT contractid, offeroremail, offereeemail FROM contracts")

myresult = mycursor.fetchall()

for contract_info in myresult:
  if int(sys.argv[1]) in contract_info:
      print(contract_info[2], end='')


