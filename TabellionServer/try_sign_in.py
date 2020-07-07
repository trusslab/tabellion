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

mycursor.execute("SELECT email, password FROM users")

myresult = mycursor.fetchall()

print("Going to print!")
for user_info in myresult:
  if sys.argv[1] in user_info:
    print("Account Existed!")
    if sys.argv[2] == user_info[1]:
      print("Account Verified!")
    else:
      print("Incorrect Password!")
    break

print("Account Check Complete!")



