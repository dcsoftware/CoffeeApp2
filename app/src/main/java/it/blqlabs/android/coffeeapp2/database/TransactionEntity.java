package it.blqlabs.android.coffeeapp2.database;

import it.blqlabs.appengine.coffeeappbackend.myApi.model.StoreRequestBean;

/**
 * Created by davide on 24/10/14.
 */
public class TransactionEntity {
    public Long _id;
    public String timestamp;
    public String amount;
    public String machineId;
    public String transactionId;
    public boolean confirmed = false;


    public TransactionEntity(StoreRequestBean bean) {
        this.transactionId = bean.getTransactionId();
        this.timestamp = bean.getTimestamp();
        this.machineId = bean.getMachineId();
        this.amount = bean.getAmount();
        this.confirmed = bean.getConfirmed();
    }

    public Long get_id() {
        return _id;
    }

    public String getMachineId() {
        return machineId;
    }

    public void setMachineId(String machineId) {
        this.machineId = machineId;
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

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public void setConfirmed(boolean confirmed) {
        this.confirmed = confirmed;
    }
}
