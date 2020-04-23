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

mycursor.execute("CREATE TABLE users (email VARCHAR(255) PRIMARY KEY, firstname VARCHAR(255), lastname VARCHAR(255), password VARCHAR(255), token VARCHAR(255))")

mycursor.execute("CREATE TABLE contracts (contractid INT AUTO_INCREMENT PRIMARY KEY, contractname VARCHAR(255), offeroremail VARCHAR(255), offereeemail VARCHAR(255), \
description VARCHAR(255), status VARCHAR(255), pagecount VARCHAR(255), confirmstatus VARCHAR(255), lastactiontimeinterval VARCHAR(255), is_created_by_tabellion VARCHAR(255), \
  signed_pages VARCHAR(255), revised_num_count VARCHAR(255))")
# mycursor.execute("SELECT contractid, offeroremail, offereeemail, status FROM contracts WHERE offeroremail='" + sys.argv[1] + "' OR offereeemail='" + sys.argv[1] + "'")

# mycursor.execute("ALTER TABLE contracts ADD revised_num_count VARCHAR(300)")

mydb.commit()

'''
myresult = mycursor.fetchall()

for contract_info in myresult:
    print(contract_info[0], end='-')
'''



