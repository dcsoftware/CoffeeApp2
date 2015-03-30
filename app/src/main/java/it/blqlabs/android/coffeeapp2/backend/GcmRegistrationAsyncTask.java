package it.blqlabs.android.coffeeapp2.backend;

import android.app.NotificationManager;
import android.content.Context;
import android.os.AsyncTask;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import it.blqlabs.android.coffeeapp2.Constants;
import it.blqlabs.android.coffeeapp2.R;
import it.blqlabs.appengine.coffeeappbackend.registration.Registration;

/**
 * Created by davide on 25/03/15.
 */
public class GcmRegistrationAsyncTask extends AsyncTask<Void, Void, String> {

    private static Registration regService = null;
    private GoogleCloudMessaging gcm;
    private Context context;
    private NotificationCompat.Builder notifBuilder;
    private NotificationManager notifMgr;

    public GcmRegistrationAsyncTask(Context context) {
        this.context = context;
    }

    @Override
    protected String doInBackground(Void... params) {
        notifBuilder = new NotificationCompat.Builder(context).setSmallIcon(R.drawable.ic_launcher).setContentTitle("Coffee App");
        notifMgr = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (regService == null) {
            Registration.Builder builder = new Registration.Builder(AndroidHttp.newCompatibleTransport(),
                    new AndroidJsonFactory(), null);
            // end of optional local run code

            regService = builder.build();
        }

        String msg = "";
        try {
            if (gcm == null) {
                gcm = GoogleCloudMessaging.getInstance(context);
            }
            String regId = gcm.register(Constants.GCM_SENDER_ID);
            msg = "Device registered, registration ID=" + regId;

            // You should send the registration ID to your server over HTTP,
            // so it can use GCM/HTTP or CCS to send messages to your app.
            // The request to your server should be authenticated if your app
            // is using accounts.
            regService.register(regId).execute();

        } catch (IOException ex) {
            ex.printStackTrace();
            msg = "Error: " + ex.getMessage();
        }
        return msg;
    }

    @Override
    protected void onPostExecute(String msg) {
        notifBuilder.setContentText(msg);
        notifMgr.notify(1, notifBuilder.build());

        //Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
        Logger.getLogger("REGISTRATION").log(Level.INFO, msg);
    }
}
