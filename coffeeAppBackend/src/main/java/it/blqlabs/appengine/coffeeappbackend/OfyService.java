package it.blqlabs.appengine.coffeeappbackend;

import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.ObjectifyService;

import it.blqlabs.appengine.coffeeappbackend.Records.MachineRecord;
import it.blqlabs.appengine.coffeeappbackend.Records.OperationRecord;
import it.blqlabs.appengine.coffeeappbackend.Records.RegistrationRecord;
import it.blqlabs.appengine.coffeeappbackend.Records.SecretKeyRecord;
import it.blqlabs.appengine.coffeeappbackend.Records.TransactionRecord;
import it.blqlabs.appengine.coffeeappbackend.Records.UserRecord;

/**
 * Objectify service wrapper so we can statically register our persistence classes
 * More on Objectify here : https://code.google.com/p/objectify-appengine/
 */
public class OfyService {

    static {
        ObjectifyService.register(RegistrationRecord.class);
        ObjectifyService.register(UserRecord.class);
        ObjectifyService.register(TransactionRecord.class);
        ObjectifyService.register(SecretKeyRecord.class);
    }

    public static Objectify ofy() {
        return ObjectifyService.ofy();
    }

    public static ObjectifyFactory factory() {
        return ObjectifyService.factory();
    }
}
