package pl.pollub.myrecommendation.models;

import java.util.Date;


public class SaveUser {
    protected String userId;
    protected Date timestamp;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }
}