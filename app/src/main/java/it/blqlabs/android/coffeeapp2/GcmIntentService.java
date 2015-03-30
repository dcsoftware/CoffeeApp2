package it.blqlabs.android.coffeeapp2;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.gcm.GoogleCloudMessaging;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by davide on 25/03/15.
 */
public class GcmIntentService extends IntentService {

    private NotificationCompat.Builder notifBuilder;
    private NotificationManager notifMgr;

    public GcmIntentService() {
        super("GcmIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        notifBuilder = new NotificationCompat.Builder(getApplicationContext()).setSmallIcon(R.drawable.ic_launcher).setContentTitle("Coffee App");
        notifMgr = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Bundle extras = intent.getExtras();
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
        Log.d("GcmIntentService", "messaggio ricevuto");
        // The getMessageType() intent parameter must be the intent you received
        // in your BroadcastReceiver.
        String messageType = gcm.getMessageType(intent);

        if (extras != null && !extras.isEmpty()) {  // has effect of unparcelling Bundle
            // Since we're not using two way messaging, this is all we really to check for
            if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)) {
                Logger.getLogger("GCM_RECEIVED").log(Level.INFO, extras.toString());

                String key = extras.getString("key");
                String date = extras.getString("date");

                notifBuilder.setContentText("New key received:\nkey = " + key + "\ndate = " + date);
                notifMgr.notify(1, notifBuilder.build());


                showToast(extras.getString("message"));
            }
        }
        GcmBroadcastReceiver.completeWakefulIntent(intent);
    }

    protected void showToast(final String message) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
            }
        });
    }
}
