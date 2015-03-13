package it.blqlabs.android.coffeeapp2.backend;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.widget.Toast;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;

import java.io.IOException;

import it.blqlabs.android.coffeeapp2.Constants;
import it.blqlabs.android.coffeeapp2.MainActivity;
import it.blqlabs.appengine.coffeeappbackend.myApi.MyApi;
import it.blqlabs.appengine.coffeeappbackend.myApi.model.KeyBean;

/**
 * Created by davide on 11/12/14.
 */
public class GetSecureKeyAsyncTask extends AsyncTask<Context, Void, KeyBean> {

    private MyApi myApiService;
    private Context context;
    private GoogleAccountCredential credential;

    @Override
    protected void onPreExecute() {

        //cancel(true);
    }

    @Override
    protected KeyBean doInBackground(Context... params) {
        //credential = MainActivity.getCredential();

//        if(!isCancelled()) {
//            //fai quello che devi fare
//        }

        MyApi.Builder builder = new MyApi.Builder(AndroidHttp.newCompatibleTransport(), new AndroidJsonFactory(), credential);
        myApiService = builder.build();
        context = params[0];

        KeyBean responseBean = new KeyBean();

        try {

            responseBean = myApiService.getTodayKey().execute();

        } catch (IOException e) {
            e.printStackTrace();
        }
        return responseBean;
    }

    @Override
    protected void onCancelled(KeyBean result) {

    }

    @Override
    protected void onPostExecute(KeyBean result) {
        if(result.size() != 0) {
            SharedPreferences prefs = context.getSharedPreferences(Constants.M_SHARED_PREF, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();

            editor.putString(Constants.PREF_KEY_DATE, result.getDate());
            editor.putString(Constants.PREF_SECRET_KEY, result.getKey());
            editor.commit();

            Toast.makeText(context, "KEY: " + result.getKey(), Toast.LENGTH_SHORT).show();
            //TODO set alarm to fire at 00:01 of the next day
        } else {
            Toast.makeText(context, "Error downloading Key", Toast.LENGTH_SHORT).show();
            //TODO set alarm to fire again in 10 seconds
        }
    }
}
