package it.blqlabs.appengine.coffeeappbackend.Records;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.api.server.spi.response.CollectionResponse;
import com.google.api.server.spi.response.NotFoundException;
import com.google.appengine.api.datastore.Cursor;
import com.google.appengine.api.datastore.QueryResultIterator;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.cmd.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.annotation.Nullable;
import javax.inject.Named;

import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * WARNING: This generated code is intended as a sample or starting point for using a
 * Google Cloud Endpoints RESTful API with an Objectify entity. It provides no data access
 * restrictions and no data validation.
 * <p/>
 * DO NOT deploy this code unchanged as part of a real application to real users.
 */
@Api(
        name = "secretKeyRecordApi",
        version = "v1",
        resource = "secretKeyRecord",
        namespace = @ApiNamespace(
                ownerDomain = "Records.coffeeappbackend.appengine.blqlabs.it",
                ownerName = "Records.coffeeappbackend.appengine.blqlabs.it",
                packagePath = ""
        )
)
public class SecretKeyRecordEndpoint {

    private static final Logger logger = Logger.getLogger(SecretKeyRecordEndpoint.class.getName());

    private static final int DEFAULT_LIST_LIMIT = 20;

    static {
        // Typically you would register this inside an OfyServive wrapper. See: https://code.google.com/p/objectify-appengine/wiki/BestPractices
        ObjectifyService.register(SecretKeyRecord.class);
    }

    /**
     * Returns the {@link SecretKeyRecord} with the corresponding ID.
     *
     * @param id the ID of the entity to be retrieved
     * @return the entity with the corresponding ID
     * @throws NotFoundException if there is no {@code SecretKeyRecord} with the provided ID.
     */
    @ApiMethod(
            name = "get",
            path = "secretKeyRecord/{id}",
            httpMethod = ApiMethod.HttpMethod.GET)
    public SecretKeyRecord get(@Named("id") Long id) throws NotFoundException {
        logger.info("Getting SecretKeyRecord with ID: " + id);
        SecretKeyRecord secretKeyRecord = ofy().load().type(SecretKeyRecord.class).id(id).now();
        if (secretKeyRecord == null) {
            throw new NotFoundException("Could not find SecretKeyRecord with ID: " + id);
        }
        return secretKeyRecord;
    }

    /**
     * Inserts a new {@code SecretKeyRecord}.
     */
    @ApiMethod(
            name = "insert",
            path = "secretKeyRecord",
            httpMethod = ApiMethod.HttpMethod.POST)
    public SecretKeyRecord insert(SecretKeyRecord secretKeyRecord) {
        // Typically in a RESTful API a POST does not have a known ID (assuming the ID is used in the resource path).
        // You should validate that secretKeyRecord.id has not been set. If the ID type is not supported by the
        // Objectify ID generator, e.g. long or String, then you should generate the unique ID yourself prior to saving.
        //
        // If your client provides the ID then you should probably use PUT instead.
        ofy().save().entity(secretKeyRecord).now();
        logger.info("Created SecretKeyRecord with ID: " + secretKeyRecord.getId());

        return ofy().load().entity(secretKeyRecord).now();
    }

    /**
     * Updates an existing {@code SecretKeyRecord}.
     *
     * @param id              the ID of the entity to be updated
     * @param secretKeyRecord the desired state of the entity
     * @return the updated version of the entity
     * @throws NotFoundException if the {@code id} does not correspond to an existing
     *                           {@code SecretKeyRecord}
     */
    @ApiMethod(
            name = "update",
            path = "secretKeyRecord/{id}",
            httpMethod = ApiMethod.HttpMethod.PUT)
    public SecretKeyRecord update(@Named("id") Long id, SecretKeyRecord secretKeyRecord) throws NotFoundException {
        // TODO: You should validate your ID parameter against your resource's ID here.
        checkExists(id);
        ofy().save().entity(secretKeyRecord).now();
        logger.info("Updated SecretKeyRecord: " + secretKeyRecord);
        return ofy().load().entity(secretKeyRecord).now();
    }

    /**
     * Deletes the specified {@code SecretKeyRecord}.
     *
     * @param id the ID of the entity to delete
     * @throws NotFoundException if the {@code id} does not correspond to an existing
     *                           {@code SecretKeyRecord}
     */
    @ApiMethod(
            name = "remove",
            path = "secretKeyRecord/{id}",
            httpMethod = ApiMethod.HttpMethod.DELETE)
    public void remove(@Named("id") Long id) throws NotFoundException {
        checkExists(id);
        ofy().delete().type(SecretKeyRecord.class).id(id).now();
        logger.info("Deleted SecretKeyRecord with ID: " + id);
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
            path = "secretKeyRecord",
            httpMethod = ApiMethod.HttpMethod.GET)
    public CollectionResponse<SecretKeyRecord> list(@Nullable @Named("cursor") String cursor, @Nullable @Named("limit") Integer limit) {
        limit = limit == null ? DEFAULT_LIST_LIMIT : limit;
        Query<SecretKeyRecord> query = ofy().load().type(SecretKeyRecord.class).limit(limit);
        if (cursor != null) {
            query = query.startAt(Cursor.fromWebSafeString(cursor));
        }
        QueryResultIterator<SecretKeyRecord> queryIterator = query.iterator();
        List<SecretKeyRecord> secretKeyRecordList = new ArrayList<SecretKeyRecord>(limit);
        while (queryIterator.hasNext()) {
            secretKeyRecordList.add(queryIterator.next());
        }
        return CollectionResponse.<SecretKeyRecord>builder().setItems(secretKeyRecordList).setNextPageToken(queryIterator.getCursor().toWebSafeString()).build();
    }

    private void checkExists(Long id) throws NotFoundException {
        try {
            ofy().load().type(SecretKeyRecord.class).id(id).safe();
        } catch (com.googlecode.objectify.NotFoundException e) {
            throw new NotFoundException("Could not find SecretKeyRecord with ID: " + id);
        }
    }
}