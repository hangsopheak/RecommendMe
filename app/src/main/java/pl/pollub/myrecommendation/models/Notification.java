package pl.pollub.myrecommendation.models;

import java.util.Date;

public class Notification {
    public static final int TYPE_SAVE = 1;
    public static final int TYPE_COMMENT = 2;
    protected String id;
    protected String senderId;
    protected String recommendationId;
    protected Date timestamp;
    protected int type;
    protected boolean unseen;
    protected User sender;
    protected Recommendation recommendation;


    public String getContent() {
        String content = null;
        if(type == TYPE_SAVE){
            content = "saved your recommendation";
        }else if(type == TYPE_COMMENT){
            content = "commented on recommendation";
        }
        return content;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getRecommendationId() {
        return recommendationId;
    }

    public void setRecommendationId(String recommendationId) {
        this.recommendationId = recommendationId;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public boolean isUnseen() {
        return unseen;
    }

    public void setUnseen(boolean unseen) {
        this.unseen = unseen;
    }

    public User getSender() {
        return sender;
    }

    public void setSender(User sender) {
        this.sender = sender;
    }

    public Recommendation getRecommendation() {
        return recommendation;
    }

    public void setRecommendation(Recommendation recommendation) {
        this.recommendation = recommendation;
    }
}
