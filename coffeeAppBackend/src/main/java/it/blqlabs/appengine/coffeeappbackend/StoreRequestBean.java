package it.blqlabs.appengine.coffeeappbackend;

/**
 * Created by davide on 29/01/15.
 */
public class StoreRequestBean {

    private String id;
    private String userId;
    private String timestamp;
    private String amount;
    private String machineId;
    private String transactionId;
    private boolean confirmed;

    public boolean isConfirmed() {
        return confirmed;
    }

    public void setConfirmed(boolean confirmed) {
        this.confirmed = confirmed;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

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
