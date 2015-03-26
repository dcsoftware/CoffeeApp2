package it.blqlabs.android.coffeeapp2.backend;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

import it.blqlabs.android.coffeeapp2.Constants;
import it.blqlabs.android.coffeeapp2.MainActivity;
import it.blqlabs.android.coffeeapp2.R;
import it.blqlabs.appengine.coffeeappbackend.myApi.MyApi;
import it.blqlabs.appengine.coffeeappbackend.myApi.model.KeyBean;

/**
 * Created by davide on 11/12/14.
 */
public class GetSecureKeyAsyncTask extends AsyncTask<Context, Void, KeyBean> {

    private MyApi myApiService;
    private Context context;
    private GoogleAccountCredential credential;
    private SharedPreferences mSharedPref;
    private NotificationCompat.Builder notifBuilder;
    private NotificationManager notifMgr;
    private SimpleDateFormat format = new SimpleDateFormat("ddMMyyyy");
    private Calendar c;
    private PendingIntent alarmIntent;

    @Override
    protected void onPreExecute() {
        //cancel(true);
        c = Calendar.getInstance();
        mSharedPref = context.getSharedPreferences(Constants.M_SHARED_PREF, Context.MODE_PRIVATE);
        String storedDate = mSharedPref.getString(Constants.PREF_KEY_DATE, "00000000");
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        String currentDate = format.format(c.getTime());

        if(storedDate.equals(currentDate)) {
            cancel(true);
        }
    }

    @Override
    protected KeyBean doInBackground(Context... params) {
        //credential = MainActivity.getCredential();
        context = params[0];
        notifBuilder = new NotificationCompat.Builder(context).setSmallIcon(R.drawable.ic_launcher).setContentTitle("Coffee App");
        notifMgr = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        KeyBean responseBean = new KeyBean();

        if(!isCancelled()) {

            MyApi.Builder builder = new MyApi.Builder(AndroidHttp.newCompatibleTransport(), new AndroidJsonFactory(), credential);
            myApiService = builder.build();


            try {

                responseBean = myApiService.getTodayKey().execute();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return responseBean;

    }

    @Override
    protected void onCancelled(KeyBean result) {
        Toast.makeText(context, "Key is up to date", Toast.LENGTH_SHORT).show();
        notifBuilder.setContentText("key is up to date");
    }

    @Override
    protected void onPostExecute(KeyBean result) {
        if(result.size() != 0) {
            if((result.getDate() != null) && (result.getKey() != null)) {
                SharedPreferences.Editor editor = mSharedPref.edit();

                editor.putString(Constants.PREF_KEY_DATE, result.getDate());
                editor.putString(Constants.PREF_SECRET_KEY, result.getKey());
                editor.commit();

                Toast.makeText(context, "KEY: " + result.getKey(), Toast.LENGTH_SHORT).show();
                notifBuilder.setContentText("Downloading key OK");
                AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(System.currentTimeMillis());
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.MINUTE, 1);

                alarmManager.setInexactRepeating(AlarmManager.RTC, calendar.getTimeInMillis(), AlarmManager.INTERVAL_HOUR, alarmIntent);
                //TODO set alarm to fire at 00:01 of the next day
            } else {
                Toast.makeText(context, "Error downloading key", Toast.LENGTH_SHORT).show();
                notifBuilder.setContentText("Downloading key NOT OK");
            }
        } else {
            Toast.makeText(context, "can't download Key", Toast.LENGTH_SHORT).show();
            //TODO set alarm to fire again in 10 seconds
            notifBuilder.setContentText("Can't download key");
        }

        notifMgr.notify(1, notifBuilder.build());
    }
}
