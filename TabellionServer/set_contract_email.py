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

identity = 'offeroremail'

if sys.argv[2] == "offeree":
    identity = 'offereeemail'

print("Going to update contract with id: " + sys.argv[1] + "'s " + identity + " to " + sys.argv[3])

sql = "UPDATE contracts SET " + identity + " = '" + sys.argv[3] +"' WHERE contractid = '" + sys.argv[1] + "'"
mycursor.execute(sql)

mydb.commit() 