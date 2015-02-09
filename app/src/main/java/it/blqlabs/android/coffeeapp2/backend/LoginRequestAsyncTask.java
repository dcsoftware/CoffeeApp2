package it.blqlabs.android.coffeeapp2.backend;

import android.os.AsyncTask;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;

import java.io.IOException;

import it.blqlabs.appengine.coffeeappbackend.myApi.MyApi;
import it.blqlabs.appengine.coffeeappbackend.myApi.model.LoginRequestBean;
import it.blqlabs.appengine.coffeeappbackend.myApi.model.LoginResponseBean;
import it.blqlabs.appengine.coffeeappbackend.myApi.model.ResponseBean;

/**
 * Created by davide on 14/01/15.
 */
public class LoginRequestAsyncTask extends AsyncTask<LoginRequestBean, Void, LoginResponseBean> {

    private MyApi myApiService;

    @Override
    protected LoginResponseBean doInBackground(LoginRequestBean... params) {
        MyApi.Builder builder = new MyApi.Builder(AndroidHttp.newCompatibleTransport(), new AndroidJsonFactory(), null);
        myApiService = builder.build();

        LoginResponseBean response = new LoginResponseBean();

        try {
            response = myApiService.login(params[0]).execute();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return response;
    }

    @Override
    protected void onPostExecute(LoginResponseBean bean) {

    }
}
