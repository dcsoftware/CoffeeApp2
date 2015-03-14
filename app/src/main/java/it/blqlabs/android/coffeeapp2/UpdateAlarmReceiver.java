package it.blqlabs.android.coffeeapp2;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

import it.blqlabs.android.coffeeapp2.backend.GetSecureKeyAsyncTask;

public class UpdateAlarmReceiver extends BroadcastReceiver {
    private SharedPreferences mSharedPref;

    public UpdateAlarmReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO: This method is called when the BroadcastReceiver is receiving
        // an Intent broadcast.
//        Calendar c = Calendar.getInstance();
//        SimpleDateFormat format = new SimpleDateFormat("ddMMyyyy");
//        format.setTimeZone(TimeZone.getTimeZone("UTC"));
//        String currentDate = format.format(c.getTime());
//
//        mSharedPref = context.getSharedPreferences(Constants.M_SHARED_PREF, Context.MODE_PRIVATE);
        new GetSecureKeyAsyncTask().execute(context);
    }
}
