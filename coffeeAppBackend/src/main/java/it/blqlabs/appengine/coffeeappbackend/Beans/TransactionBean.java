package it.blqlabs.appengine.coffeeappbackend.Beans;

/**
 * Created by davide on 28/10/14.
 */
public class TransactionBean {

    private String id;
    private String userId;
    private String timestamp;
    private String amount;
    private String machineId;
    private boolean confirmed;

    public String getMachineId() {
        return machineId;
    }

    public void setMachineId(String machineId) {
        this.machineId = machineId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public void setConfirmed(boolean confirmed) {
        this.confirmed = confirmed;
    }



    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String toString() {
        return "transactionID=" + id + ",\nuserId=" + userId + ",\nmachineId= " + machineId + ",\ntimestamp=" + timestamp + ",\namount=" + amount;
    }
}
