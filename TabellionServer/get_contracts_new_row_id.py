import mysql.connector
import sys

mydb = mysql.connector.connect(
  host="localhost",
  user="myles",
  passwd="Fyqlyx12~",
  database="SignIt"
)

mycursor = mydb.cursor()

mycursor.execute("SELECT contractid FROM contracts")

print(mycursor.fetchall()[-1][0] + 1, end='')

