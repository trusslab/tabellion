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

print("email: " + sys.argv[1])
print("firstname: " + sys.argv[2])
print("lastname: " + sys.argv[3])
print("password: " + sys.argv[4])
print("token: " + sys.argv[5])

sql = "INSERT INTO users (email, firstname, lastname, password, token) VALUES (%s, %s, %s, %s, %s)"
val = (sys.argv[1], sys.argv[2], sys.argv[3], sys.argv[4], sys.argv[5])
mycursor.execute(sql, val)

mydb.commit()

print(mycursor.rowcount, "user created.")


