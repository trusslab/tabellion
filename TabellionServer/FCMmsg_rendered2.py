#!/usr/bin/python

#import cgi

#print cgi.__file__

#import os, cgi
#import cgitb; cgitb.enable()

import sys
#import site
#site.addsitedir('/home/saeed/.local/lib/python2.7/site-packages/')
#sys.path.append('/usr/lib/python2.7/')
sys.path.insert(0, '/home/saeed/.local/lib/python2.7/site-packages')
#sys.path.append('/home/saeed/.local/lib/python2.7/site-packages/firebase_admin')

import os
print os.getegid()

import firebase_admin
from firebase_admin import credentials
from firebase_admin import messaging

print firebase_admin.__file__

#app1
#cred = credentials.Certificate('myserver-214300-firebase-adminsdk-jj01b-0c9288b3ae.json')
#app2
cred = credentials.Certificate('truesignresearch-firebase-adminsdk-gl0ko-0e85616485.json')
default_app = firebase_admin.initialize_app(cred)


#This registration token comes from the client FCM SDKs.
#app1
#registration_token = 'fo5VvF5KTy8:APA91bH5f_5xpvwpjkfZg8hCvZdJhvVKR8Mj4yrBW86kgKAvGG-dNHayGCP3wSPhYkJnD3nY_nkiiF_FAg5ZKaXl8ebg0EbnjBo6BDh6gNBgKd5W4Exa_lDTkUzJwDXM3-AiuejpBu6f'
#app2
#registration_token = 'ew6XQVW62QI:APA91bH2zZxqrEVlfGzVaHb3jftFad0UEoCFMfSJvW5x6Jj-k99GB1JxUzyajGyviGhod6mEAA8Q-25DMNp6u_9ln5MJQdI4IRc9yB07ZQCK_IKcU9gizMZxD2D8qXbzMZaltcM0w1wQ'
#registration_token = 'fFzCCxm64wA:APA91bEQHy4mHGPFsZ7z83x4Ji9ykCXL00XFUgGVdhrWXsNDXQ2C4Pt1eAS9m4wyeHwLtbTl3XZogbTep4KcySSAra4htyGk0ZP-96VHVF7D3WPQgY-AyxKAZ2aKt8zJ6cQ6GPUjFDN4'
#registration_token = 'eHWYqnoDfUA:APA91bFNiPd1UdRupr6Y0RxK5DxkQyp2NlHqqDHAmkMbBrlltIBIaG0N69SptL9JqwSaizUyoKMubLI55nQt9uMcoUfSem34fSp2MI50vJWdAq2magQZBoaXyWS-tdHBUh9qLUDBzZ7Q'
registration_token = sys.argv[1]
#export GCLOUD_PROJECT= 'myserver-214300'

# See documentation on defining a message payload.
notif = messaging.Notification(
	title="Hello!" ,
	body="Offeror has sent you a new document to sign!" ,
)

message = messaging.Message(
    data={
        'command': '1',
        'url': '2:45',
	    'contractid' : sys.argv[2],
	    'contract_name' : sys.argv[3],
	    'description' : sys.argv[4],
	    'status' : sys.argv[5],
	    'page_count' : sys.argv[6],
	    'offeror_email' : sys.argv[7],
		'offeree_email' : sys.argv[8],
    },
    token=registration_token,
    notification = notif,
)

# Send a message to the device corresponding to the provided
# registration token.
response = messaging.send(message)
# Response is a message ID string.
print('Successfully sent message:', response)

#

