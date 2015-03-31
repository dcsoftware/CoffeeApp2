package it.blqlabs.appengine.coffeeappbackend.Endpoints;

import com.google.android.gcm.server.Message;
import com.google.android.gcm.server.Result;
import com.google.android.gcm.server.Sender;
import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.api.server.spi.response.CollectionResponse;
import com.google.api.server.spi.response.NotFoundException;
import com.google.appengine.api.datastore.Cursor;
import com.google.appengine.api.datastore.QueryResultIterator;
import com.googlecode.objectify.cmd.Query;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import javax.annotation.Nullable;
import javax.inject.Named;

import it.blqlabs.appengine.coffeeappbackend.Records.SecretKeyRecord;
import it.blqlabs.appengine.coffeeappbackend.Records.UserRecord;

import static it.blqlabs.appengine.coffeeappbackend.OfyService.ofy;

/**
 * WARNING: This generated code is intended as a sample or starting point for using a
 * Google Cloud Endpoints RESTful API with an Objectify entity. It provides no data access
 * restrictions and no data validation.
 * <p/>
 * DO NOT deploy this code unchanged as part of a real application to real users.
 */
@Api(
        name = "userRecordApi",
        version = "v1",
        resource = "userRecord",
        namespace = @ApiNamespace(
                ownerDomain = "coffeeappbackend.appengine.blqlabs.it",
                ownerName = "coffeeappbackend.appengine.blqlabs.it",
                packagePath = ""
        )
)
public class UserRecordEndpoint {

    private static final Logger logger = Logger.getLogger(UserRecordEndpoint.class.getName());

    private static final int DEFAULT_LIST_LIMIT = 20;


    @ApiMethod(
            name = "registerUser",
            httpMethod = ApiMethod.HttpMethod.POST)
    public UserRecord registerUser(UserRecord userRecord) throws IOException {
        UserRecord oldUser = ofy().load().type(UserRecord.class).filter("userEmail", userRecord.getUserEmail()).first().now();

        if(oldUser == null) {
            logger.info("Creating new User;");
            userRecord.setUserId(generateUserId());
            userRecord.setUserCredit("0.00");
            ofy().save().entity(userRecord).now();
        } else {
            logger.info("User already exist;");
            userRecord.setUserId(oldUser.getUserId());
            userRecord.setUserCredit(oldUser.getUserCredit());
            ofy().delete().entity(oldUser).now();
            ofy().save().entity(userRecord).now();
        }

        sendKeyToNewUser(userRecord);
        return ofy().load().entity(userRecord).now();
    }

    private void sendKeyToNewUser(UserRecord newUser) throws IOException{
        logger.info("Sending key to new registered user");
        String API_KEY = System.getProperty("gcm.api.key");
        Message message;
        Sender sender = new Sender(API_KEY);

        Calendar c = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
        String date = dateFormat.format(c.getTime());

        SecretKeyRecord secretKey = ofy().load().type(SecretKeyRecord.class).filter("date", date).first().now();


        if(secretKey != null) {
            logger.info("Secretkey record found");
            message = new Message.Builder().addData("key", secretKey.getSecretKey()).addData("date", secretKey.getDate()).build();
            Result result = sender.send(message, newUser.getGcmId(), 5);
            if (result.getMessageId() != null) {
                logger.info("Message sent to " + newUser.getGcmId());
                String canonicalRegId = result.getCanonicalRegistrationId();
                if (canonicalRegId != null) {
                    // if the regId changed, we have to update the datastore
                    logger.info("Registration Id changed for " + newUser.getGcmId() + " updating to " + canonicalRegId);
                    newUser.setGcmId(canonicalRegId);
                    ofy().save().entity(newUser).now();
                }
            } else {
                String error = result.getErrorCodeName();
                if (error.equals(com.google.android.gcm.server.Constants.ERROR_NOT_REGISTERED)) {
                    logger.warning("Registration Id " + newUser.getGcmId() + " no longer registered with GCM, removing from datastore");
                    // if the device is no longer registered with Gcm, remove it from the datastore
                    ofy().delete().entity(newUser).now();
                } else {
                    logger.warning("Error when sending message : " + error);
                }
            }
        } else {
            logger.info("Error retriving secretkey record");
        }
    }



    /**
     * Returns the {@link UserRecord} with the corresponding ID.
     *
     * @param id the ID of the entity to be retrieved
     * @return the entity with the corresponding ID
     * @throws NotFoundException if there is no {@code UserRecord} with the provided ID.
     */
    @ApiMethod(
            name = "get",
            path = "userRecord/{id}",
            httpMethod = ApiMethod.HttpMethod.GET)
    public UserRecord get(@Named("id") Long id) throws NotFoundException {
        logger.info("Getting UserRecord with ID: " + id);
        UserRecord userRecord = ofy().load().type(UserRecord.class).id(id).now();
        if (userRecord == null) {
            throw new NotFoundException("Could not find UserRecord with ID: " + id);
        }
        return userRecord;
    }

    /**
     * Inserts a new {@code UserRecord}.
     */
    /*@ApiMethod(
            name = "insert",
            httpMethod = ApiMethod.HttpMethod.POST)
    public UserRecord insert(UserRecord userRecord) {
        // Typically in a RESTful API a POST does not have a known ID (assuming the ID is used in the resource path).
        // You should validate that userRecord.id has not been set. If the ID type is not supported by the
        // Objectify ID generator, e.g. long or String, then you should generate the unique ID yourself prior to saving.
        //
        // If your client provides the ID then you should probably use PUT instead.
        ofy().save().entity(userRecord).now();
        logger.info("Created UserRecord with ID: " + userRecord.getId());

        return ofy().load().entity(userRecord).now();
    }*/

    /**
     * Updates an existing {@code UserRecord}.
     *
     * @param id         the ID of the entity to be updated
     * @param userRecord the desired state of the entity
     * @return the updated version of the entity
     * @throws NotFoundException if the {@code id} does not correspond to an existing
     *                           {@code UserRecord}
     */
    @ApiMethod(
            name = "update",
            path = "userRecord/{id}",
            httpMethod = ApiMethod.HttpMethod.PUT)
    public UserRecord update(@Named("id") Long id, UserRecord userRecord) throws NotFoundException {
        // TODO: You should validate your ID parameter against your resource's ID here.
        checkExists(id);
        ofy().save().entity(userRecord).now();
        logger.info("Updated UserRecord: " + userRecord);
        return ofy().load().entity(userRecord).now();
    }

    /**
     * Deletes the specified {@code UserRecord}.
     *
     * @param id the ID of the entity to delete
     * @throws NotFoundException if the {@code id} does not correspond to an existing
     *                           {@code UserRecord}
     */
    @ApiMethod(
            name = "remove",
            path = "userRecord/{id}",
            httpMethod = ApiMethod.HttpMethod.DELETE)
    public void remove(@Named("id") Long id) throws NotFoundException {
        checkExists(id);
        ofy().delete().type(UserRecord.class).id(id).now();
        logger.info("Deleted UserRecord with ID: " + id);
    }

    /**
     * List all entities.
     *
     * @param cursor used for pagination to determine which page to return
     * @param limit  the maximum number of entries to return
     * @return a response that encapsulates the result list and the next page token/cursor
     */
    @ApiMethod(
            name = "list",
            path = "userRecord",
            httpMethod = ApiMethod.HttpMethod.GET)
    public CollectionResponse<UserRecord> list(@Nullable @Named("cursor") String cursor, @Nullable @Named("limit") Integer limit) {
        limit = limit == null ? DEFAULT_LIST_LIMIT : limit;
        Query<UserRecord> query = ofy().load().type(UserRecord.class).limit(limit);
        if (cursor != null) {
            query = query.startAt(Cursor.fromWebSafeString(cursor));
        }
        QueryResultIterator<UserRecord> queryIterator = query.iterator();
        List<UserRecord> userRecordList = new ArrayList<UserRecord>(limit);
        while (queryIterator.hasNext()) {
            userRecordList.add(queryIterator.next());
        }
        return CollectionResponse.<UserRecord>builder().setItems(userRecordList).setNextPageToken(queryIterator.getCursor().toWebSafeString()).build();
    }

    private void checkExists(Long id) throws NotFoundException {
        try {
            ofy().load().type(UserRecord.class).id(id).safe();
        } catch (com.googlecode.objectify.NotFoundException e) {
            throw new NotFoundException("Could not find UserRecord with ID: " + id);
        }
    }

    private String generateUserId() {
        return UUID.randomUUID().toString();
    }
}