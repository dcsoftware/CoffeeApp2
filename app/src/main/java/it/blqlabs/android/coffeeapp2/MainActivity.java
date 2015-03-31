package it.blqlabs.android.coffeeapp2;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.SystemClock;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.widget.CardView;
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
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

import it.blqlabs.android.coffeeapp2.OtpGenerator.Clock;
import it.blqlabs.android.coffeeapp2.OtpGenerator.OtpGenerator;
import it.blqlabs.android.coffeeapp2.backend.GcmRegistrationAsyncTask;
import it.blqlabs.android.coffeeapp2.backend.GetSecureKeyAsyncTask;
import it.blqlabs.android.coffeeapp2.database.TransactionsDBOpenHelper;
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
    private MenuItem itemProgress;
    private boolean onLineMode = false;
    private ConnectivityManager connManager;
    private NetworkInfo netInfo;

    private SharedPreferences mSharedPref;

    private CardView cardStatus;
    private CardView cardCredit;

    private PendingIntent alarmIntent;

    Calendar c = Calendar.getInstance();
    SimpleDateFormat format = new SimpleDateFormat("ddMMyyyy");



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
                        itemProgress.setActionView(R.layout.progress);
                        itemProgress.expandActionView();
                        //progressBar.setVisibility(View.VISIBLE);
                    } else if (msg.obj == 0) {
                        Log.d("MAIN ACTIVITY", "Stop progress bar");
                        itemProgress.collapseActionView();
                        itemProgress.setActionView(null);
                        MainActivity.this.invalidateOptionsMenu();
                        //progressBar.setVisibility(View.INVISIBLE);
                    } else {
                        Toast.makeText(getApplicationContext() ,"MAIN ACTIVITY: error message", Toast.LENGTH_SHORT).show();
                    }
                    break;
                case 2:
                    Toast.makeText(getApplicationContext(), "Updating Credit", Toast.LENGTH_SHORT).show();
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
        setContentView(R.layout.activity_main_new);

        checkConnection();
        if(onLineMode){
            Toast.makeText(this, "ON LINE MODE", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "OFF LINE MODE", Toast.LENGTH_SHORT).show();

        }

        mSharedPref = getSharedPreferences(Constants.M_SHARED_PREF, MODE_PRIVATE);


        cardStatus = (CardView)findViewById(R.id.card_connection_status);
        cardCredit = (CardView)findViewById(R.id.card_credit);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);

        mMainActivity = this;
        mContext = getApplicationContext();

        logText = (TextView)findViewById(R.id.textView2);
        userIdText = (TextView)findViewById(R.id.textView);
        userCreditText = (TextView)findViewById(R.id.textView_credit);
        text3 = (TextView)findViewById(R.id.textView4);
        button = (Button)findViewById(R.id.button);
        purchaseButton = (Button)findViewById(R.id.button2);
        rechargeButton = (Button)findViewById(R.id.button3);
        newUserButton = (Button) findViewById(R.id.button4);
        progressBar = (ProgressBar) findViewById(R.id.progressBar2);

        final OtpGenerator otpG = new OtpGenerator("ABCDEFGHIJ");

        isFirstRun();

        handleIntent(getIntent());
    }

    public void getSecureKey() {

        String storedDate = mSharedPref.getString(Constants.PREF_KEY_DATE, "00000000");
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        String currentDate = format.format(c.getTime());

        if(!storedDate.equals(currentDate)) {
            Toast.makeText(this, "GETTING KEY", Toast.LENGTH_SHORT).show();
            new GetSecureKeyAsyncTask().execute(this);
        } else {
            Toast.makeText(this, "KEY already stored:" + mSharedPref.getString(Constants.PREF_SECRET_KEY, "00000000"), Toast.LENGTH_SHORT).show();
        }
    }

    public void checkConnection() {
        connManager = (ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);

        netInfo = connManager.getActiveNetworkInfo();

        onLineMode = netInfo != null && netInfo.isConnected();
    }

    public void updateUserData() {
        SharedPreferences userSharedPref = getSharedPreferences(Constants.USER_SHARED_PREF, MODE_PRIVATE);
        Toast.makeText(getApplicationContext(), "updating new credit.", Toast.LENGTH_SHORT).show();
        userId = userSharedPref.getString(Constants.USER_ID, "null");
        userCredit = userSharedPref.getString(Constants.USER_CREDIT, "null");
        //userIdText.setText(userId);
        userCreditText.setText(userCredit);
    }

    public void isFirstRun() {
        if (mSharedPref.getBoolean(Constants.IS_FIRST_RUN, true)) {
            Toast.makeText(this, "FIRST RUN", Toast.LENGTH_SHORT).show();
            Intent i = new Intent(MainActivity.this, RegisterUserActivity.class);
            startActivityForResult(i, 100);

        } else {
            updateUserData();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
            switch(requestCode) {
                case 100:
                    SharedPreferences.Editor mPrefEditor = mSharedPref.edit();
                    switch(resultCode) {
                        case 99:
                            mPrefEditor.putBoolean(Constants.IS_FIRST_RUN, false);
                            mPrefEditor.commit();
                            Toast.makeText(this, "User registered succesfull", Toast.LENGTH_SHORT).show();
                            updateUserData();
                            break;
                        case 101:
                            mPrefEditor.putBoolean(Constants.IS_FIRST_RUN, true);
                            mPrefEditor.commit();
                            Toast.makeText(this, "User not registered", Toast.LENGTH_SHORT).show();
                            break;
                    }
            }



    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_new_main, menu);
        itemProgress = menu.findItem(R.id.action_refresh);
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

        if(id == R.id.action_refresh) {
            itemProgress = item;
            itemProgress.setActionView(R.layout.progress);
            itemProgress.expandActionView();

            UserBean bean = new UserBean();
            bean.setUserId(userId);
            bean.setUserCredit(userCredit);
            bean.setRegistrationTimestamp(String.valueOf(new Clock().getCurrentSecond()));
            checkConnection();
            if(onLineMode){
                Toast.makeText(this, "ON LINE MODE", Toast.LENGTH_SHORT).show();
                //new CheckUserDataTask().execute(bean);
                new GetSecureKeyAsyncTask().execute(this);
            } else {
                Toast.makeText(this, "OFF LINE MODE", Toast.LENGTH_SHORT).show();
                itemProgress.collapseActionView();
                itemProgress.setActionView(null);
                MainActivity.this.invalidateOptionsMenu();
            }
        }

        if(id == R.id.action_history) {
            Intent i = new Intent(MainActivity.this, HistoryActivity.class);
            startActivityForResult(i, 200);
        }

        if(id == R.id.action_reset) {
            new TransactionsDBOpenHelper(getContext()).reset();
            SharedPreferences.Editor editor = mSharedPref.edit();
            editor.putString("ACCOUNT_NAME", null);
            editor.putBoolean(Constants.IS_FIRST_RUN, true);
            editor.commit();
            RegisterUserActivity.credential.setSelectedAccountName(null);
            finish();
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
                //Intent readerService = new Intent(MainActivity.this, OnLineComService.class);
                Intent readerService = new Intent(MainActivity.this, OffLineComService.class);
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

    public class CheckUserDataTask extends AsyncTask<UserBean, Void, ResponseBean> {

        private MyApi myApiService;

        @Override
        protected ResponseBean doInBackground(UserBean... params) {
            MyApi.Builder builder = new MyApi.Builder(AndroidHttp.newCompatibleTransport(), new AndroidJsonFactory(), null);
            myApiService = builder.build();

            ResponseBean response = new ResponseBean();

            try {
                response = myApiService.checkUserData(params[0]).execute();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return response;
        }

        @Override
        protected void onPostExecute(ResponseBean responseBean) {
            if(responseBean.size() != 0) {
                itemProgress.collapseActionView();
                itemProgress.setActionView(null);
                MainActivity.this.invalidateOptionsMenu();
                if (responseBean.getConfirmed()) {
                    Toast.makeText(MainActivity.this, "DATA OK", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "DATA NOT OK", Toast.LENGTH_SHORT).show();

                }
            } else {
                Toast.makeText(MainActivity.this, "DATA NULL", Toast.LENGTH_SHORT).show();

            }
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
