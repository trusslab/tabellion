package edu.uci.ics.charm.tabellion;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

/*
Created Date: 2018
Created By: Saeed Mirzamohammadi
Last Modified: 04/10/2019
Last Modified By: Myles Liu
Notes:

 */

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "MyFirebaseMessagingService";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage){
        int command;
        if(remoteMessage.getData().get("command") == null){
            command = 0;
        } else {
            command = Integer.valueOf(remoteMessage.getData().get("command"));
        }
        //int files = Integer.valueOf(remoteMessage.getData().get("files"));

        super.onMessageReceived(remoteMessage);
        Log.d("Saeed, msg", "onMessageReceived: " + command + " with msg: " + remoteMessage.getNotification().getBody());
        Intent intent = new Intent(this, MainActivity.class);
        /*
        if(command == 2){
            Log.d("Myles ", "The intent is going to be checking status");
            intent = new Intent(this, OfferorStatusCheckingActivity.class);
        }
        */
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT);
        String channelId = "Default";
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.app_icon)
                .setContentTitle(remoteMessage.getNotification().getTitle())
                .setContentText(remoteMessage.getNotification().getBody()).setAutoCancel(true).setContentIntent(pendingIntent);
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "Default channel", NotificationManager.IMPORTANCE_DEFAULT);
            manager.createNotificationChannel(channel);
        }
        if (command == 1) {

            manager.notify(0, builder.build());
        }
        else if (command == 2) {
            Log.d("Saeed: ", "Request for checking status");
            manager.notify(0, builder.build());
        }
        else if(command == 3){
            //FirstPage.set_status(0);
            manager.notify(0, builder.build());
        }
        else {
            manager.notify(0, builder.build());
        }
    }
}