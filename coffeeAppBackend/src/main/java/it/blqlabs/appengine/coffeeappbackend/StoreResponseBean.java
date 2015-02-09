package it.blqlabs.appengine.coffeeappbackend;

/**
 * Created by davide on 29/01/15.
 */
public class StoreResponseBean {

    private String transactionId;
    private String amount;
    private boolean confirmed;

    public StoreResponseBean() {

    }

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public StoreResponseBean(boolean confirmed) {
        this.confirmed = confirmed;
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

    public String toString() {
        return "transactionId=" + transactionId + ",confirmed=" + confirmed;
    }



}
