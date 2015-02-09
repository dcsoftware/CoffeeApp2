package it.blqlabs.android.coffeeapp2.backend;

import android.content.Context;
import android.os.AsyncTask;
import android.support.v4.util.Pair;

import it.blqlabs.appengine.coffeeappbackend.myApi.model.LoginRequestBean;

/**
 * Created by davide on 26/01/15.
 */
public class LoginTask extends AsyncTask<Pair<LoginRequestBean, Context>, Void, Void> {
    @Override
    protected Void doInBackground(Pair<LoginRequestBean, Context>... params) {

        Context context = params[0].second;



        return null;
    }
}
