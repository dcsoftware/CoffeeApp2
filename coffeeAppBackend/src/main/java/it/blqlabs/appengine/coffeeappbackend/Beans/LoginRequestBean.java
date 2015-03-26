package it.blqlabs.appengine.coffeeappbackend.Beans;

/**
 * Created by davide on 14/01/15.
 */
public class LoginRequestBean {

    private String userId;
    private long otpPassword;
    private String machineId;
    private long timestamp;

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public long getOtpPassword() {
        return otpPassword;
    }

    public void setOtpPassword(long otpPassword) {
        this.otpPassword = otpPassword;
    }

    public String getMachineId() {
        return machineId;
    }

    public void setMachineId(String machineId) {
        this.machineId = machineId;
    }
}
