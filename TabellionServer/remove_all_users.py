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


sql = "DELETE FROM users"
mycursor.execute(sql)

#mycursor.execute("SELECT email, password FROM users")

#mycursor.execute("SELECT contractid, offeroremail, offereeemail FROM contracts")
#mycursor.execute("ALTER TABLE contracts ADD COLUMN pagecount INT AFTER status;")

#myresult = mycursor.fetchall()

#mycursor.execute("select * from users")
#mycursor.execute("ALTER TABLE contracts AUTO_INCREMENT = 1")

#sql = "INSERT INTO contracts (contractname, offeroremail, offereeemail, description, status, pagecount) VALUES (%s, %s, %s, %s, %s, %s)"
#val = ("contract_for_init", "init@init.com", "init@init.com", "init contract", "7", "0")
#mycursor.execute(sql, val)

mycursor.execute("select * from users")

for x in mycursor:
  print(x)

#print(myresult)

mydb.commit() 