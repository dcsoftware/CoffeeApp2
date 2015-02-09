package it.blqlabs.appengine.coffeeappbackend;

/**
 * Created by davide on 20/01/15.
 */
public class LoginResponseBean {

    private String userId;
    private boolean logged = false;
    private String userCredit;
    private String controlKey;

    public String getControlKey() {

        return controlKey;
    }

    public void setControlKey(String controlKey) {
        this.controlKey = controlKey;
    }

    public LoginResponseBean(){
        this.userId = null;
        this.userCredit = null;
        this.logged = false;
    }

    public LoginResponseBean(String userId, String userCredit, String controlKey, boolean logged) {
        this.userId = userId;
        this.userCredit = userCredit;
        this.logged = logged;
        this.controlKey = controlKey;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public boolean getLogged() {
        return logged;
    }

    public void setLogged(boolean logged) {
        this.logged = logged;
    }

    public String getUserCredit() {
        return userCredit;
    }

    public void setUserCredit(String userCredit) {
        this.userCredit = userCredit;
    }
}
