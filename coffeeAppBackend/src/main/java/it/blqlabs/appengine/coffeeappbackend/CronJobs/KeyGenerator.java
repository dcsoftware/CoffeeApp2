package it.blqlabs.appengine.coffeeappbackend.CronJobs;

import com.google.android.gcm.server.Message;
import com.google.android.gcm.server.Result;
import com.google.android.gcm.server.Sender;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Transaction;

import java.io.IOException;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.logging.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import it.blqlabs.appengine.coffeeappbackend.Constants;
import it.blqlabs.appengine.coffeeappbackend.Records.RegistrationRecord;
import it.blqlabs.appengine.coffeeappbackend.Records.SecretKeyRecord;
import it.blqlabs.appengine.coffeeappbackend.Records.UserRecord;

import static it.blqlabs.appengine.coffeeappbackend.OfyService.ofy;

/**
 * Created by davide on 03/11/14.
 */
public class KeyGenerator extends HttpServlet{

    private static final String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private static final int keyLength = 20;
    private static final String API_KEY = System.getProperty("gcm.api.key");
    private static final Logger log = Logger.getLogger(KeyGenerator.class.getName());



    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        //DatastoreService datastoreService = DatastoreServiceFactory.getDatastoreService();
        //Transaction txn = datastoreService.beginTransaction();

        Calendar c = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");

        String generatedKey = generateNewKey();
        sendKeyToClients(generatedKey, dateFormat.format(c.getTime()));

        SecretKeyRecord newRecord = new SecretKeyRecord();
        newRecord.setSecretKey(generatedKey);
        newRecord.setDate(dateFormat.format(c.getTime()));

        ofy().save().entity(newRecord).now();
        /*try{
            Entity entity = new Entity(Constants.ENTITY_NAME_KEY);
            entity.setProperty(Constants.PROPERTY_DATE, dateFormat.format(c.getTime()));
            entity.setProperty(Constants.PROPERTY_KEY, generatedKey);
            datastoreService.put(entity);
            txn.commit();
        } finally {
            if(txn.isActive()) {
                txn.rollback();
            }
        }*/
    }

    private String generateNewKey() {
        SecureRandom rnd = new SecureRandom();
        StringBuilder sb = new StringBuilder(keyLength);

        for(int i = 0; i < keyLength; i++ ){
            sb.append(alphabet.charAt(rnd.nextInt(alphabet.length())));
        }

        return sb.toString();
    }

    private void sendKeyToClients(String key, String date) throws IOException {
        log.info("Sending key");
        Sender sender = new Sender(API_KEY);
        Message msg = new Message.Builder().addData("key", key).addData("date", date).build();
        List<UserRecord> records = ofy().load().type(UserRecord.class).limit(10).list();
        for (UserRecord record : records) {
            Result result = sender.send(msg, record.getGcmId(), 5);
            if (result.getMessageId() != null) {
                log.info("Message sent to " + record.getGcmId());
                String canonicalRegId = result.getCanonicalRegistrationId();
                if (canonicalRegId != null) {
                    // if the regId changed, we have to update the datastore
                    log.info("Registration Id changed for " + record.getGcmId() + " updating to " + canonicalRegId);
                    record.setGcmId(canonicalRegId);
                    ofy().save().entity(record).now();
                }
            } else {
                String error = result.getErrorCodeName();
                if (error.equals(com.google.android.gcm.server.Constants.ERROR_NOT_REGISTERED)) {
                    log.warning("Registration Id " + record.getGcmId() + " no longer registered with GCM, removing from datastore");
                    // if the device is no longer registered with Gcm, remove it from the datastore
                    ofy().delete().entity(record).now();
                } else {
                    log.warning("Error when sending message : " + error);
                }
            }
        }
    };
}
