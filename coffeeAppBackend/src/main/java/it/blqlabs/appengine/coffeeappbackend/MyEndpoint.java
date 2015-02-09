/*
   For step-by-step instructions on connecting your Android application to this backend module,
   see "App Engine Java Endpoints Module" template documentation at
   https://github.com/GoogleCloudPlatform/gradle-appengine-templates/tree/master/HelloEndpoints
*/

package it.blqlabs.appengine.coffeeappbackend;

import com.google.api.server.spi.Constant;
import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Transaction;
import com.google.appengine.api.datastore.TransactionOptions;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;

import java.util.Calendar;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import it.blqlabs.appengine.coffeeappbackend.OTPGenerator.OtpGenerator;

/**
 * An endpoint class we are exposing
 */
@Api(name = "myApi",
        version = "v1",
        namespace = @ApiNamespace(ownerDomain = "coffeeappbackend.appengine.blqlabs.it",
                ownerName = "coffeeappbackend.appengine.blqlabs.it", packagePath = ""))

public class MyEndpoint {


    private static final Logger log = Logger.getLogger(MyEndpoint.class.getName());

    @ApiMethod(name = "login")
    public LoginResponseBean login(LoginRequestBean bean) {

        log.setLevel(Level.ALL);
        long otpReceived = bean.getOtpPassword();
        long timestamp = bean.getTimestamp();
        long otpGenerated = 0;
        boolean confirmed = false;
        String machineId = bean.getMachineId();
        String userId = bean.getUserId();
        String secretKey = "";
        String controlKey = "";

        log.info("Received Data: UserId: " + userId + ",\nMachineId: " + machineId + ",\nOTP: " + otpReceived + ",\nTimestamp: " + timestamp);

        DatastoreService datastoreService = DatastoreServiceFactory.getDatastoreService();
        Query.Filter machineFilter = new Query.FilterPredicate(Constants.PROPERTY_MACHINE_ID, Query.FilterOperator.EQUAL, machineId);

        Query machineQuery = new Query(Constants.ENTITY_NAME_MACHINE).setFilter(machineFilter);
        Entity machine = datastoreService.prepare(machineQuery).asSingleEntity();

        if (machine == null) {
            return new LoginResponseBean(userId, null, null, false);
        } else {

            secretKey = (String) machine.getProperty(Constants.PROPERTY_MACHINE_SECRET_KEY);
            controlKey = (String) machine.getProperty(Constants.PROPERTY_MACHINE_CONTROL_KEY);


            OtpGenerator otpGenerator = new OtpGenerator(secretKey);

            try {
                otpGenerated = otpGenerator.getCode();
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (InvalidKeyException e) {
                e.printStackTrace();
            }

            Query.Filter userFilter = new Query.FilterPredicate(Constants.PROPERTY_USER_ID, Query.FilterOperator.EQUAL, userId);
            Query userQuery = new Query(Constants.ENTITY_NAME_USER).setFilter(userFilter);
            Entity user = datastoreService.prepare(userQuery).asSingleEntity();

            log.info("Generated data: SecretKey: " + secretKey + ",\nOTP: " + otpGenerated + ",\nTimestamp: " + otpGenerator.getTimestamp() + ",\nControlKey=" + controlKey);

            if (otpReceived == otpGenerated) {
                confirmed = true;
            } else {
                confirmed = false;
            }

            Transaction txn = datastoreService.beginTransaction();
            try {
                Entity entity = new Entity(Constants.ENTITY_NAME_EVENT);
                entity.setProperty(Constants.PROPERTY_MACHINE_ID, machineId);
                entity.setProperty(Constants.PROPERTY_USER_ID, userId);
                entity.setProperty(Constants.PROPERTY_EVENT_TYPE, "log_in");
                entity.setProperty(Constants.PROPERTY_TIMESTAMP, timestamp);
                entity.setProperty(Constants.PROPERTY_CONFIRMED, confirmed);
                datastoreService.put(entity);
                txn.commit();
            } finally {
                if (txn.isActive()) {
                    txn.rollback();
                }
            }

            log.info("Response: userId: " + user.getProperty(Constants.PROPERTY_USER_ID) + "\nuserCredit: " + user.getProperty(Constants.PROPERTY_USER_CREDIT) + "\nLogged: " + confirmed);

            return new LoginResponseBean(user.getProperty(Constants.PROPERTY_USER_ID).toString(), user.getProperty(Constants.PROPERTY_USER_CREDIT).toString(), controlKey, confirmed);
        }
    }

    @ApiMethod(name = "storeClientTransaction", httpMethod = ApiMethod.HttpMethod.POST)
    public StoreResponseBean storeClientTransaction(StoreRequestBean bean) {
        DatastoreService datastoreService = DatastoreServiceFactory.getDatastoreService();
        TransactionOptions storeOptions = TransactionOptions.Builder.withXG(true);
        Transaction txn = datastoreService.beginTransaction(storeOptions);

        String userId = bean.getUserId();
        Query.Filter userFilter = new Query.FilterPredicate(Constants.PROPERTY_USER_ID, Query.FilterOperator.EQUAL, bean.getUserId());

        Query userQuery = new Query(Constants.ENTITY_NAME_USER).setFilter(userFilter);

        Entity userEntity = datastoreService.prepare(userQuery).asSingleEntity();

        float userCredit = (Float) userEntity.getProperty(Constants.PROPERTY_USER_CREDIT);
        float amount = Float.valueOf(bean.getAmount());

        userCredit += amount;

        try{

            userEntity.setProperty(Constants.PROPERTY_USER_CREDIT, userCredit);
            datastoreService.put(userEntity);

            Entity transaction = new Entity(Constants.ENTITY_NAME_CLIENT_TRANSACTION);
            transaction.setProperty(Constants.PROPERTY_TRANSACTION_ID, bean.getId());
            transaction.setProperty(Constants.PROPERTY_USER_ID, bean.getUserId());
            transaction.setProperty(Constants.PROPERTY_MACHINE_ID, bean.getMachineId());
            transaction.setProperty(Constants.PROPERTY_TIMESTAMP, bean.getTimestamp());
            transaction.setProperty(Constants.PROPERTY_AMOUNT, bean.getAmount());
            datastoreService.put(transaction);
            txn.commit();
        } finally {
            if(txn.isActive()) {
                txn.rollback();
            }
        }

        StoreResponseBean response = new StoreResponseBean();
        response.setTransactionId(bean.getId());
        response.setAmount(bean.getAmount());
        response.setConfirmed(true);

        log.info("Request: " + bean.toString());
        log.info("Response: " + response.toString());

        return response;

    }

    @ApiMethod(name = "clearClientHistory")
    public ResponseBean clearClientHistory(UserBean bean) {
        DatastoreService datastoreService = DatastoreServiceFactory.getDatastoreService();
        Transaction txn = datastoreService.beginTransaction();
        Query.Filter userFilter = new Query.FilterPredicate(Constants.PROPERTY_USER_ID, Query.FilterOperator.EQUAL, bean.getUserId());
        try {
            Query query = new Query(Constants.ENTITY_NAME_CLIENT_TRANSACTION).setFilter(userFilter);
            List<Entity> results = datastoreService.prepare(query)
                    .asList(FetchOptions.Builder.withDefaults());
            for (Entity result : results) {
                datastoreService.delete(result.getKey());
            }
            txn.commit();
        } finally {
            if (txn.isActive()) { txn.rollback(); }
        }

        ResponseBean response = new ResponseBean();
        response.setTransactionId("null");
        response.setConfirmed(true);

        return response;
    }

    @ApiMethod(name = "getTodayKey", httpMethod = ApiMethod.HttpMethod.GET, scopes = {Constants.EMAIL_SCOPE},
                clientIds = {Constants.WEB_CLIENT_ID,
                            Constants.ANDROID_ID,
                             Constant.API_EXPLORER_CLIENT_ID},
                audiences = {Constants.ANDROID_AUDIENCE})
    public KeyBean getTodayKey(UserBean bean) {
        DatastoreService datastoreService = DatastoreServiceFactory.getDatastoreService();

        Calendar c = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("ddMMyyyy");

        Query.Filter userFilter = new Query.FilterPredicate(Constants.PROPERTY_DATE, Query.FilterOperator.EQUAL, dateFormat.format(c.getTime()));

        Query query = new Query(Constants.ENTITY_NAME_KEY).setFilter(userFilter);

        List<Entity> results = datastoreService.prepare(query).asList(FetchOptions.Builder.withDefaults());

        KeyBean responseKey = new KeyBean();
        responseKey.setKey((String) results.get(0).getProperty(Constants.PROPERTY_KEY));
        responseKey.setDate((String) results.get(0).getProperty(Constants.PROPERTY_DATE));

        log.info("Request: " + bean.toString());
        log.info("Response: " + responseKey.toString());

        return responseKey;
    }

    @ApiMethod(name = "registerNewUser")
    public UserBean registerNewUser(UserBean user) {
        String id;
        DatastoreService datastoreService = DatastoreServiceFactory.getDatastoreService();
        TransactionOptions options = TransactionOptions.Builder.withXG(true);
        Transaction newUserTxn = datastoreService.beginTransaction(options);

        Query.Filter userFilter = new Query.FilterPredicate(Constants.PROPERTY_USER_EMAIL, Query.FilterOperator.EQUAL, user.getUserEmail());
        Query userQuery = new Query(Constants.ENTITY_NAME_USER).setFilter(userFilter);

        Entity oldUser = datastoreService.prepare(userQuery).asSingleEntity();

        if(oldUser == null) {
            try {
                id = generateUserId();
                Entity newUser = new Entity(Constants.ENTITY_NAME_USER);
                newUser.setProperty(Constants.PROPERTY_USER_ID, id);
                newUser.setProperty(Constants.PROPERTY_USER_EMAIL, user.getUserEmail());
                newUser.setProperty(Constants.PROPERTY_USER_CREDIT, "0.00");
                newUser.setProperty(Constants.PROPERTY_REGISTRATION_TIMESTAMP, user.getRegistrationTimestamp());
                log.info("New User: id = " + id + ", email= " + user.getUserEmail());
                datastoreService.put(newUserTxn,newUser);
                Entity newEvent = new Entity(Constants.ENTITY_NAME_EVENT);
                newEvent.setProperty(Constants.PROPERTY_USER_ID, id);
                newEvent.setProperty(Constants.PROPERTY_EVENT_TYPE, "new_user");
                newEvent.setProperty(Constants.PROPERTY_MACHINE_ID, "null");
                newEvent.setProperty(Constants.PROPERTY_TIMESTAMP, user.getRegistrationTimestamp());
                newEvent.setProperty(Constants.PROPERTY_CONFIRMED, "true");
                datastoreService.put(newUserTxn, newEvent);
                newUserTxn.commit();
            } finally {
                if (newUserTxn.isActive()) {
                    newUserTxn.rollback();
                }
            }

            user.setUserId(id);
            user.setUserCredit("0.00");
            return user;
        } else {

            user.setUserId((String) oldUser.getProperty(Constants.PROPERTY_USER_ID));
            user.setUserCredit((String) oldUser.getProperty(Constants.PROPERTY_USER_CREDIT));

            return user;
        }


    }

    private String generateUserId() {

        return UUID.randomUUID().toString();
    }

}
