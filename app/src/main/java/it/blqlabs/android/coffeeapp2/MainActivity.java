package it.blqlabs.android.coffeeapp2;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

import it.blqlabs.android.coffeeapp2.OtpGenerator.OtpGenerator;
import it.blqlabs.appengine.coffeeappbackend.myApi.MyApi;
import it.blqlabs.appengine.coffeeappbackend.myApi.model.LoginRequestBean;
import it.blqlabs.appengine.coffeeappbackend.myApi.model.LoginResponseBean;
import it.blqlabs.appengine.coffeeappbackend.myApi.model.ResponseBean;
import it.blqlabs.appengine.coffeeappbackend.myApi.model.TransactionBean;
import it.blqlabs.appengine.coffeeappbackend.myApi.model.UserBean;


public class MainActivity extends ActionBarActivity implements CardReader.AccountCallback{

    private NfcAdapter mNfcAdapter;
    private static Tag mTag;
    private static Context mContext;
    private static MainActivity mMainActivity;
    private static final String MIME_APPLICATION = "application/coffeeapp2";

    private SharedPreferences mSharedPref;

    private CardReader reader;


    public Messenger mMessenger = new Messenger(new MessageHandler(this));

    private class MessageHandler extends Handler {
        private Context c;

        MessageHandler(Context c){
            this.c = c;
        }
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    if(msg.obj == 1) {
                        Log.d("MAIN ACTIVITY", "Start progress bar");
                        progressBar.setVisibility(View.VISIBLE);
                    } else if (msg.obj == 0) {
                        Log.d("MAIN ACTIVITY", "Stop progress bar");
                        progressBar.setVisibility(View.INVISIBLE);
                    } else {
                        Toast.makeText(getApplicationContext() ,"MAIN ACTIVITY: error message", Toast.LENGTH_SHORT).show();
                    }
                    break;
                case 2:
                    updateUserData();
                    Log.d("MAIN ACTIVITY", "Update credit");
                    break;
            }
            //onActionReceived(msg.obj.toString());
        }
    }

    TextView logText, userIdText, userCreditText, text3;
    Button button, purchaseButton, rechargeButton, newUserButton;
    ProgressBar progressBar;

    String userId, userCredit;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.test_main);
        int i = 0;
        mSharedPref = getSharedPreferences(Constants.M_SHARED_PREF, MODE_PRIVATE);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);

        mMainActivity = this;
        mContext = getApplicationContext();

        logText = (TextView)findViewById(R.id.textView2);
        userIdText = (TextView)findViewById(R.id.textView);
        userCreditText = (TextView)findViewById(R.id.textView3);
        text3 = (TextView)findViewById(R.id.textView4);
        button = (Button)findViewById(R.id.button);
        purchaseButton = (Button)findViewById(R.id.button2);
        rechargeButton = (Button)findViewById(R.id.button3);
        newUserButton = (Button) findViewById(R.id.button4);
        progressBar = (ProgressBar) findViewById(R.id.progressBar2);

        reader = new CardReader(this, mContext);

        final OtpGenerator otpG = new OtpGenerator("ABCDEFGHIJ");

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                long otp = 0;
                try {
                    otp = otpG.getCode1();
                } catch (NoSuchAlgorithmException | InvalidKeyException e) {
                    e.printStackTrace();
                }

                LoginRequestBean request = new LoginRequestBean();
                request.setOtpPassword(otp);
                request.setMachineId("00005678");
                request.setUserId(userId);
                request.setTimestamp(otpG.getTimestamp());

                logText.append("OTP: " + otp + ", Timestamp: " + otpG.getTimestamp());
                new LoginAsyncTask().execute(request);
            }
        });

        purchaseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TransactionBean request = new TransactionBean();
                request.setMachineId("00005678");
                request.setUserId(userId);
                request.setAmount("-0.55");
                request.setTimestamp(String.valueOf(otpG.getTimestamp()));

                new StoreAsyncTask().execute(request);
            }
        });

        rechargeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TransactionBean request = new TransactionBean();
                request.setMachineId("00005678");
                request.setUserId(userId);
                request.setAmount("+2.00");
                request.setTimestamp(String.valueOf(otpG.getTimestamp()));

                new StoreAsyncTask().execute(request);
            }
        });

        newUserButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                UserBean user = new UserBean();
                user.setUserEmail("ciaociao" + new Random().nextInt());
                user.setRegistrationTimestamp(String.valueOf(otpG.getTimestamp()));

                new RegisterTask().execute(user);
                newUserButton.setClickable(false);

            }
        });
        newUserButton.setClickable(false);
        isFirstRun();

        handleIntent(getIntent());

    }

    public void updateUserData() {
        SharedPreferences userSharedPref = getSharedPreferences(Constants.USER_SHARED_PREF, MODE_PRIVATE);

        userId = userSharedPref.getString(Constants.USER_ID, "null");
        userCredit = userSharedPref.getString(Constants.USER_CREDIT, "null");
        userIdText.setText(userId);
        userCreditText.setText(userCredit);
    }

    public void isFirstRun() {
        if (mSharedPref.getBoolean(Constants.IS_FIRST_RUN, true)) {

            Toast.makeText(this, "FIRST RUN", Toast.LENGTH_SHORT).show();
            newUserButton.setClickable(true);

            /*SharedPreferences userSharedPref = getSharedPreferences(Constants.USER_SHARED_PREF, MODE_PRIVATE);
            SharedPreferences.Editor userPrefEditor = userSharedPref.edit();
            userPrefEditor.putString(Constants.USER_NAME, "Davide");
            userPrefEditor.putString(Constants.USER_SURNAME, "Corradini");
            userPrefEditor.putString(Constants.USER_CREDIT, "0");
            userPrefEditor.putString(Constants.USER_ID, "123456789");
            userPrefEditor.commit();*/

            SharedPreferences.Editor mPrefEditor = mSharedPref.edit();
            mPrefEditor.putBoolean(Constants.IS_FIRST_RUN, false);
            mPrefEditor.commit();
        } else {
            updateUserData();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
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

    @Override
    protected void onResume(){
        super.onResume();
        startForegroundDispatch(this, mNfcAdapter);
    }

    public void startForegroundDispatch(final Activity activity, NfcAdapter adapter) {
        final Intent intent = new Intent(activity.getApplicationContext(), activity.getClass());
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        final PendingIntent pendingIntent = PendingIntent.getActivity(activity.getApplicationContext(), 0, intent, 0);

        IntentFilter[] filters = new IntentFilter[1];

        filters[0] = new IntentFilter();
        filters[0].addAction(NfcAdapter.ACTION_NDEF_DISCOVERED);
        filters[0].addCategory(Intent.CATEGORY_DEFAULT);
        String [][] techlist = new String[][] {};

        try{
            filters[0].addDataType(MIME_APPLICATION);
        } catch (IntentFilter.MalformedMimeTypeException e) {
            e.printStackTrace();
        }

        adapter.enableForegroundDispatch(activity, pendingIntent,filters, techlist);
    }

    @Override
    protected void onPause() {
        stopForegroundDispatch(this, mNfcAdapter);

        super.onPause();
    }

    public void stopForegroundDispatch(final Activity activity, NfcAdapter adapter) {
        adapter.disableForegroundDispatch(activity);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        handleIntent(intent);
    }


    private void handleIntent(Intent intent) {
        String action = intent.getAction();

        if(NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
            mTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

            if(mTag != null) {
                Intent readerService = new Intent(MainActivity.this, ComService.class);
                startService(readerService);
                //reader.StartReadingTag(mTag);
            }
        }
    }

    public static Tag getTag() {
        return mTag;
    }

    public static Context getContext() {
        return mContext;
    }

    public static MainActivity getMainActivity() {
        return mMainActivity;
    }

    public class LoginAsyncTask extends AsyncTask<LoginRequestBean, Void, LoginResponseBean> {

        private MyApi myApiService;

        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
        }

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
            logText.append("Login request: " + String.valueOf(bean.getLogged() + "\nControlKey:" + bean.getControlKey()));

            progressBar.setVisibility(View.INVISIBLE);
        }
    }

    public class StoreAsyncTask extends AsyncTask<TransactionBean, Void, ResponseBean> {

        private MyApi myApiService;

        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected ResponseBean doInBackground(TransactionBean... params) {
            MyApi.Builder builder = new MyApi.Builder(AndroidHttp.newCompatibleTransport(), new AndroidJsonFactory(), null);
            myApiService = builder.build();

            ResponseBean response = new ResponseBean();

            /*try {
                response = myApiService.storeClientTransaction(params[0]).execute();
            } catch (IOException e) {
                e.printStackTrace();
            }*/

            return response;
        }

        @Override
        protected void onPostExecute(ResponseBean bean) {
            progressBar.setVisibility(View.INVISIBLE);
        }
    }

    public class RegisterTask extends AsyncTask<UserBean, Void, UserBean> {

        private MyApi myApiService;

        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
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

            updateUserData();

            userIdText.setText(userId);
            userCreditText.setText(userCredit);

            progressBar.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void updateLogTextView(String log) {

    }

    @Override
    public void updateCredit(String credit) {

    }

    @Override
    public void updateProgressBar(final boolean visibility) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(visibility) {
                    progressBar.setVisibility(View.VISIBLE);
                } else {
                    progressBar.setVisibility(View.INVISIBLE);
                }
            }
        });

    }
}
