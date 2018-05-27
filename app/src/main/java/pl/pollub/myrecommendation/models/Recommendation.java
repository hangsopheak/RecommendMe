package pl.pollub.myrecommendation.models;

import android.net.Uri;

import java.util.Date;
import java.util.List;

public class Recommendation {

    public static final int TYPE_LINK = 1;
    public static final int TYPE_LOCATION = 2;

    protected String id;
    protected String userId;
    protected Uri imageUri;
    protected String title;
    protected String description;
    protected String idea;
    protected int recommendationType;
    protected String categoryId;
    protected Category category;
    protected Date timstamp;
    protected User user;
    protected List<SaveUser> saveUserList;

    public List<SaveUser> getSaveUserList() {
        return saveUserList;
    }

    public void setSaveUserList(List<SaveUser> saveUserList) {
        this.saveUserList = saveUserList;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Date getTimstamp() {
        return timstamp;
    }

    public void setTimstamp(Date timstamp) {
        this.timstamp = timstamp;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Uri getImageUri() {
        return imageUri;
    }

    public void setImageUri(Uri imageUri) {
        this.imageUri = imageUri;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIdea() {
        return idea;
    }

    public void setIdea(String idea) {
        this.idea = idea;
    }

    public int getRecommendationType() {
        return recommendationType;
    }

    public void setRecommendationType(int recommendationType) {
        this.recommendationType = recommendationType;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }


}
