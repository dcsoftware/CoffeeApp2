package it.blqlabs.appengine.coffeeappbackend.Beans;

/**
 * Created by davide on 28/10/14.
 */

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;

@Entity
public class KeyBean {

    @Id
    private int id;
    private String key;
    private String date;

    public KeyBean() {}

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String toString() {
        return "key=" + key + ",date=" + date;
    }


}
