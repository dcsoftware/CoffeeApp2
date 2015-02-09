/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package it.blqlabs.android.coffeeapp2;

import android.content.Context;
import android.content.SharedPreferences;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.os.AsyncTask;
import android.util.Log;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import it.blqlabs.android.coffeeapp2.OtpGenerator.OtpGenerator;
import it.blqlabs.appengine.coffeeappbackend.myApi.MyApi;
import it.blqlabs.appengine.coffeeappbackend.myApi.model.LoginRequestBean;
import it.blqlabs.appengine.coffeeappbackend.myApi.model.LoginResponseBean;
import it.blqlabs.appengine.coffeeappbackend.myApi.model.StoreRequestBean;
import it.blqlabs.appengine.coffeeappbackend.myApi.model.StoreResponseBean;

/**
 * Callback class, invoked when an NFC card is scanned while the device is running in reader mode.
 *
 * Reader mode can be invoked by calling NfcAdapter
 */
public class CardReader {
    private static final String TAG = "LoyaltyCardReader";
    // AID for our loyalty card service.
    private static final String SAMPLE_LOYALTY_CARD_AID = "F222222222";
    // ISO-DEP command HEADER for selecting an AID.
    // Format: [Class | Instruction | Parameter 1 | Parameter 2]
    private static final String SELECT_APDU_HEADER = "00A40400";
    // "OK" status word sent in responseOk to SELECT AID command (0x9000)
    private static final byte[] SELECT_OK_SW = {(byte) 0x90, (byte) 0x00};

    private static final String APP_AID = "FF000000001234";
    private static final String APDU_LE = "00";
    private static final String APDU_SELECT = "00A4";
    private static final String APDU_AUTHENTICATE = "0020";
    private static final String APDU_LOG_IN = "0030";
    private static final String APDU_READ_STATUS = "0040";
    private static final String APDU_UPDATE_CREDIT = "0050";
    private static final String APDU_P1_SELECT_BY_NAME = "04";
    private static final String APDU_P2_SELECT_BY_NAME = "00";
    private static final String APDU_P1_GENERAL = "00";
    private static final String APDU_P2_GENERAL = "00";
    private static final byte[] RESULT_OK = {(byte) 0x90, (byte) 0x00};
    private static final byte[] RESULT_STATUS_WAITING = {(byte) 0x22, (byte) 0x33};
    private static final byte[] RESULT_STATUS_RECHARGED = {(byte) 0x33, (byte) 0x44};
    private static final byte[] RESULT_STATUS_PURCHASE = {(byte) 0x44, (byte) 0x55};
    private static final byte[] RESULT_DATA_UPDATED = {(byte) 0x55, (byte) 0x66};
    private static final byte[] RESULT_PRIV_APP_SELECTED = {(byte) 0x66, (byte) 0x77};
    private static final byte[] RESULT_AUTH_ERROR = {(byte) 0xB1, (byte) 0xB2};
    private static final String CARD_READER = "CARD READER";

    private Context context;
    private Tag tag;
    private SharedPreferences userPrefs;
    private SharedPreferences.Editor prefEditor;
    private String userId;
    private String userCredit;
    private String machineId;
    private String secretKey;
    private String controlKey = "MNOPQRSTUV";
    private String transactionNumber;
    private IsoDep isoTag;
    private Constants.State cardState;
    private OtpGenerator otpGenerator;
    private byte[] command;
    private byte[] result;
    private byte[] statusWord;
    private byte[] data;
    private float newCredit = 0;
    private boolean responseOk = false;

    // Weak reference to prevent retain loop. mAccountCallback is responsible for exiting
    // foreground mode before it becomes invalid (e.g. during onPause() or onStop()).
    private WeakReference<AccountCallback> mAccountCallback;

    public interface AccountCallback {
        public void updateLogTextView(String log);
        public void updateCredit(String credit);
        public void updateProgressBar(boolean visibility);
    }

    public CardReader(AccountCallback accountCallback, Context context) {
        mAccountCallback = new WeakReference<AccountCallback>(accountCallback);
        this.context = context;
    }

    /**
     * Callback when a new tag is discovered by the system.
     *
     * <p>Communication with the card should take place here.
     *
     * @param tag Discovered tag
     */
    /*@Override
    public void onTagDiscovered(Tag tag) {
        // Android's Host-based Card Emulation (HCE) feature implements the ISO-DEP (ISO 14443-4)
        // protocol.
        //
        // In order to communicate with a device using HCE, the discovered tag should be processed
        // using the IsoDep class.
        IsoDep isoDep = IsoDep.get(tag);
        if (isoDep != null) {
            try {
                // Connect to the remote NFC device
                isoDep.connect();
                // Build SELECT AID command for our loyalty card service.
                // This command tells the remote device which service we wish to communicate with.
                byte[] command = BuildSelectApdu(SAMPLE_LOYALTY_CARD_AID);
                // Send command to remote device
                byte[] result = isoDep.transceive(command);
                // If AID is successfully selected, 0x9000 is returned as the status word (last 2
                // bytes of the result) by convention. Everything before the status word is
                // optional payload, which is used here to hold the account number.
                int resultLength = result.length;
                byte[] statusWord = {result[resultLength-2], result[resultLength-1]};
                byte[] payload = Arrays.copyOf(result, resultLength-2);
                if (Arrays.equals(SELECT_OK_SW, statusWord)) {
                    // The remote NFC device will immediately respond with its stored account number
                    String accountNumber = new String(payload, "UTF-8");
                    // Inform CardReaderFragment of received account number
                    mAccountCallback.get().onAccountReceived(accountNumber);
                }
            } catch (IOException e) {
            }
        }
    }*/

    public void StartReadingTag(Tag tag){
        userPrefs = context.getSharedPreferences(Constants.USER_SHARED_PREF, Context.MODE_PRIVATE);
        prefEditor = userPrefs.edit();

        userId = userPrefs.getString(Constants.USER_ID, "");
        userCredit = userPrefs.getString(Constants.USER_CREDIT, "");
        newCredit = Float.valueOf(userCredit);
        cardState = Constants.State.DISCONNECTED;
        isoTag = IsoDep.get(tag);

        if(isoTag != null) {
            try {
                isoTag.setTimeout(10000);

                isoTag.connect();

                Log.d(CARD_READER, "Connected to tag");
                cardState = Constants.State.CONNECTED;

                while(isoTag.isConnected()) {
                    switch(cardState) {
                        case CONNECTED:
                            Thread.sleep(500);

                            command = BuildApduCommand(APDU_SELECT, APDU_P1_SELECT_BY_NAME, APDU_P2_SELECT_BY_NAME, APP_AID, APDU_LE);
                            result = isoTag.transceive(command);
                            statusWord = new byte[]{result[0], result[1]};

                            Log.d(CARD_READER, "App Selection:\nStatus Word:" + new String(statusWord)
                                    + "\nResult:" + new String(result));

                            if(Arrays.equals(RESULT_PRIV_APP_SELECTED, statusWord)) {
                                machineId = new String(Arrays.copyOfRange(result, 2, 10));
                                secretKey = new String(Arrays.copyOfRange(result, 11, result.length));

                                Log.d(CARD_READER, "App Selection:\nMachine ID:" + machineId
                                                    + "\nSercet Key:" + secretKey);

                                otpGenerator = new OtpGenerator(secretKey);

                                LoginRequestBean loginRequest = new LoginRequestBean();
                                loginRequest.setMachineId(machineId);
                                loginRequest.setUserId(userId);
                                loginRequest.setOtpPassword(otpGenerator.getCode1());
                                loginRequest.setTimestamp(otpGenerator.getTimestamp());

                                Log.d(CARD_READER, "Backend Login Request:\nOTP Code:" + loginRequest.getOtpPassword()
                                                    + "\nTimestamp:" + loginRequest.getTimestamp());

                                cardState = Constants.State.WAITING_RESPONSE;
                                responseOk = false;

                                new LoginAsyncTask().execute(loginRequest);
                            }

                            break;
                        case WAITING_RESPONSE:
                            //START SPINNER
                            while(!responseOk);
                            break;
                        case AUTHENTICATED:
                            data = (controlKey + "," + userId + "," + userCredit).getBytes();
                            command = BuildApduCommand(APDU_LOG_IN, APDU_P1_GENERAL, APDU_P2_GENERAL, ByteArrayToHexString(data), APDU_LE);

                            result = isoTag.transceive(command);
                            statusWord = new byte[]{result[0], result[1]};
                            Log.d(CARD_READER, "Log in:\nStatus Word:" + new String(statusWord)
                                    + "\nResult:" + new String(result));
                            if(Arrays.equals(RESULT_OK, statusWord)) {
                                cardState = Constants.State.READING_STATUS;

                            }

                            break;
                        case READING_STATUS:
                            Thread.sleep(500);

                            data = new byte[]{(byte) 0x11, (byte) 0x22};
                            command = BuildApduCommand(APDU_READ_STATUS, APDU_P1_GENERAL, APDU_P2_GENERAL, ByteArrayToHexString(data), APDU_LE);
                            result = isoTag.transceive(command);
                            statusWord = new byte[]{result[0], result[1]};

                            byte[] timestamp;

                            if(Arrays.equals(RESULT_STATUS_WAITING, statusWord)) {
                                cardState = Constants.State.READING_STATUS;
                            } else if (Arrays.equals(RESULT_STATUS_RECHARGED, statusWord)) {
                                timestamp = Arrays.copyOfRange(result, 8, 18);
                                float rechargeValue = Float.valueOf(new String(Arrays.copyOfRange(result, 2, 7)));
                                transactionNumber = new String(Arrays.copyOfRange(result, 18, result.length));


                                StoreRequestBean storeRequest = new StoreRequestBean();
                                storeRequest.setMachineId(machineId);
                                storeRequest.setUserId(userId);
                                storeRequest.setAmount("+" + String.valueOf(rechargeValue));
                                storeRequest.setTimestamp(new String(timestamp));

                                //newCredit += rechargeValue;
                                responseOk = false;
                                cardState = Constants.State.WAITING_RESPONSE;

                                new StoreAsyncTask().execute(storeRequest);
                                //STORE TRANSACTION TASK

                            } else if (Arrays.equals(RESULT_STATUS_PURCHASE, statusWord)) {
                                timestamp = Arrays.copyOfRange(result, 8, 18);
                                float purchaseValue = Float.valueOf(new String(Arrays.copyOfRange(result, 2, 7)));
                                transactionNumber = new String(Arrays.copyOfRange(result, 18, result.length));

                                StoreRequestBean storeRequest = new StoreRequestBean();
                                storeRequest.setMachineId(machineId);
                                storeRequest.setUserId(userId);
                                storeRequest.setAmount("-" + String.valueOf(purchaseValue));
                                storeRequest.setTimestamp(new String(timestamp));


                                //newCredit -= purchaseValue;
                                //STORE TRANSACTION TASK
                                responseOk = false;
                                cardState = Constants.State.WAITING_RESPONSE;
                                new StoreAsyncTask().execute(storeRequest);

                            }

                            break;
                        case DATA_UPDATED:
                            Thread.sleep(500);

                            newCredit = (float)Math.round(newCredit * 100) / 100;

                            prefEditor.putString(Constants.USER_CREDIT, String.valueOf(newCredit));
                            prefEditor.commit();

                            userCredit = userPrefs.getString(Constants.USER_CREDIT, "");

                            cardState = Constants.State.AUTHENTICATED;

                            break;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (InvalidKeyException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Build APDU for SELECT AID command. This command indicates which service a reader is
     * interested in communicating with. See ISO 7816-4.
     *
     * @param aid Application ID (AID) to select
     * @return APDU for SELECT AID command
     */
    public static byte[] BuildSelectApdu(String aid) {
        // Format: [CLASS | INSTRUCTION | PARAMETER 1 | PARAMETER 2 | LENGTH | DATA]
        return HexStringToByteArray(SELECT_APDU_HEADER + String.format("%02X", aid.length() / 2) + aid);
    }

    public byte[] BuildApduCommand(String header, String p1, String p2, String data, String le) {
        return HexStringToByteArray(header + p1 + p2 + String.format("%02X", data.length() / 2) + data + le);
    }

    /**
     * Utility class to convert a byte array to a hexadecimal string.
     *
     * @param bytes Bytes to convert
     * @return String, containing hexadecimal representation.
     */
    public static String ByteArrayToHexString(byte[] bytes) {
        final char[] hexArray = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};
        char[] hexChars = new char[bytes.length * 2];
        int v;
        for ( int j = 0; j < bytes.length; j++ ) {
            v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    /**
     * Utility class to convert a hexadecimal string to a byte string.
     *
     * <p>Behavior with input strings containing non-hexadecimal characters is undefined.
     *
     * @param s String containing hexadecimal characters to convert
     * @return Byte array generated from input
     */
    public static byte[] HexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    public class LoginAsyncTask extends AsyncTask<LoginRequestBean, Void, LoginResponseBean> {

        private MyApi myApiService;

        @Override
        protected void onPreExecute(){
            mAccountCallback.get().updateProgressBar(true);
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
            if(bean.getLogged()) {
                controlKey = bean.getControlKey();
                cardState = Constants.State.AUTHENTICATED;
                responseOk = true;
                Log.d("LOGIN TASK", "Confirmed:" + bean.getLogged() + "\nControl Key:" + controlKey);
            } else {

            }
            mAccountCallback.get().updateProgressBar(false);
        }
    }

    public class StoreAsyncTask extends AsyncTask<StoreRequestBean, Void, StoreResponseBean> {

        private MyApi myApiService;

        @Override
        protected void onPreExecute() {
            mAccountCallback.get().updateProgressBar(true);
        }

        @Override
        protected StoreResponseBean doInBackground(StoreRequestBean... params) {
            MyApi.Builder builder = new MyApi.Builder(AndroidHttp.newCompatibleTransport(), new AndroidJsonFactory(), null);
            myApiService = builder.build();

            StoreResponseBean response = new StoreResponseBean();

            try {
                response = myApiService.storeClientTransaction(params[0]).execute();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return response;
        }

        @Override
        protected void onPostExecute(StoreResponseBean bean) {
            if(bean.getConfirmed()) {
                //update @newcredit con l'amuont ricevuto e settare cardstate in DATA_UPDATED
                newCredit += Float.valueOf(bean.getAmount());
                cardState = Constants.State.DATA_UPDATED;
                responseOk = true;
                Log.d("STORE TASK", "Confirmed:" + bean.getConfirmed() + "\nCredit:" + newCredit);

            }
            mAccountCallback.get().updateProgressBar(false);
        }
    }

}
