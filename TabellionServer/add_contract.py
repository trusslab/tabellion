import mysql.connector
import sys

mydb = mysql.connector.connect(
  host="localhost",
  user="myles",
  passwd="Fyqlyx12~",
  database="SignIt"
)

mycursor = mydb.cursor()

# mycursor.execute("CREATE TABLE contracts (contractid INT AUTO_INCREMENT PRIMARY KEY, contractname VARCHAR(255), offeroremail VARCHAR(255), offereeemail VARCHAR(255))")

# mycursor.execute("TRUNCATE TABLE contracts")


sql = "INSERT INTO contracts (contractname, offeroremail, offereeemail, description, status, pagecount, confirmstatus) VALUES (%s, %s, %s, %s, %s, %s, %s)"
print("contractname: " + sys.argv[1])
print("offeroremail: " + sys.argv[2])
print("offereeemail: " + sys.argv[3])
print("description: " + sys.argv[4])
print("status: " + sys.argv[5])
print("pagecount: " + sys.argv[6])
print("confirmstatus: " + sys.argv[7])

val = (sys.argv[1], sys.argv[2], sys.argv[3], sys.argv[4], int(sys.argv[5]), int(sys.argv[6]), int(sys.argv[7]))
mycursor.execute(sql, val)

mydb.commit()

mycursor.execute("SELECT contractid FROM contracts")

print("start printing contract_id" + str(mycursor.fetchall()[-1][0]) + "has been inserted!\n")



