package it.blqlabs.android.coffeeapp2;

import android.accounts.AccountManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;

import java.io.IOException;

import it.blqlabs.android.coffeeapp2.OtpGenerator.OtpGenerator;
import it.blqlabs.appengine.coffeeappbackend.myApi.MyApi;
import it.blqlabs.appengine.coffeeappbackend.myApi.model.UserBean;


public class RegisterUserActivity extends ActionBarActivity {

    Button registerButton, okButton;
    TextView textArea;
    private static final int REQUEST_ACCOUNT_PICKER = 2;
    private static GoogleAccountCredential credential;
    private SharedPreferences privSharedPrefs;
    private String accountName;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_user);

        registerButton = (Button)findViewById(R.id.button5);
        okButton = (Button)findViewById(R.id.button6);
        okButton.setEnabled(false);
        textArea = (TextView)findViewById(R.id.textView7);

        privSharedPrefs = getSharedPreferences(Constants.M_SHARED_PREF, MODE_PRIVATE);
        credential = GoogleAccountCredential.usingAudience(this, "server:client_id:824332171015-5ckmh8k95cbj2ffr2mccret8nl2fuucp.apps.googleusercontent.com");
        setAccountName(privSharedPrefs.getString("ACCOUNT_NAME", null));

        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(credential.getSelectedAccountName() != null) {
                    Toast.makeText(getApplicationContext(), "Logged in with: " + credential.getSelectedAccountName(), Toast.LENGTH_LONG).show();
                } else {
                    chooseAccount();
                }
            }
        });

        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(99);
                finish();
            }
        });

    }

    private void setAccountName(String accountName) {
        SharedPreferences.Editor prefsEditor = privSharedPrefs.edit();
        prefsEditor.putString("ACCOUNT_NAME", accountName);
        prefsEditor.commit();
        credential.setSelectedAccountName(accountName);
        this.accountName = accountName;
    }

    void chooseAccount() {
        startActivityForResult(credential.newChooseAccountIntent(), REQUEST_ACCOUNT_PICKER);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_ACCOUNT_PICKER:
                if(data != null && data.getExtras() != null) {
                    String accountName = data.getExtras().getString(AccountManager.KEY_ACCOUNT_NAME);
                    if(accountName != null) {
                        setAccountName(accountName);
                        SharedPreferences.Editor prefsEditor = privSharedPrefs.edit();
                        prefsEditor.putString("ACCOUNT_NAME", accountName);
                        prefsEditor.commit();
                        UserBean newUser = new UserBean();
                        newUser.setUserEmail(accountName);
                        newUser.setRegistrationTimestamp(String.valueOf(new OtpGenerator(null).getTimestamp()));

                        new RegisterTask().execute(newUser);
                    }
                }
        }
    }

    @Override
    public void onBackPressed() {
        setResult(101);
        super.onBackPressed();
    }

    public class RegisterTask extends AsyncTask<UserBean, Void, UserBean> {

        private MyApi myApiService;

        @Override
        protected void onPreExecute() {
            //progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected UserBean doInBackground(UserBean... params) {
            MyApi.Builder builder = new MyApi.Builder(AndroidHttp.newCompatibleTransport(), new AndroidJsonFactory(), null);
            myApiService = builder.build();

            UserBean responseUser = new UserBean();

            try {
                responseUser = myApiService.registerNewUser(params[0]).execute();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return responseUser;
        }

        @Override
        protected void onPostExecute(UserBean bean) {
            SharedPreferences userSharedPref = getSharedPreferences(Constants.USER_SHARED_PREF, MODE_PRIVATE);
            SharedPreferences.Editor userPrefEditor = userSharedPref.edit();

            userPrefEditor.putString(Constants.USER_NAME, "Davide");
            userPrefEditor.putString(Constants.USER_SURNAME, "Corradini");
            userPrefEditor.putString(Constants.USER_CREDIT, bean.getUserCredit());
            userPrefEditor.putString(Constants.USER_ID, bean.getUserId());
            userPrefEditor.putString("userEmail", bean.getUserEmail());
            userPrefEditor.commit();

            textArea.setText("User Email: " + bean.getUserEmail() + "\nUser ID: " + bean.getUserId() + "\nUser Credit: " + bean.getUserCredit());

            okButton.setEnabled(true);


            //updateUserData();

            //userIdText.setText(userId);
            //userCreditText.setText(userCredit);

            //progressBar.setVisibility(View.INVISIBLE);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_register_user, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
