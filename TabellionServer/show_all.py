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


#sql = "DELETE FROM contracts"
#mycursor.execute(sql)

#mycursor.execute("SELECT email, password FROM users")

#mycursor.execute("SELECT contractid, offeroremail, offereeemail FROM contracts")
#mycursor.execute("ALTER TABLE contracts ADD COLUMN pagecount INT AFTER status;")

#myresult = mycursor.fetchall()

mycursor.execute("select * from " + sys.argv[1])

for x in mycursor:
  print(x)

#print(myresult)

mydb.commit() 