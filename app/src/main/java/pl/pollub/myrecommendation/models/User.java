package pl.pollub.myrecommendation.models;

import java.util.List;

public class User {

    public static final String SEX_MALE = "M";
    public static final String SEX_FEMALE = "F";

    protected String id;
    protected String email;
    protected String sex;

    protected String password;
    protected String name;
    protected String profilePicture;
    protected List<String> interestedCategories;


    public List<String> getInterestedCategories() {
        return interestedCategories;
    }

    public void setInterestedCategories(List<String> interestedCategories) {
        this.interestedCategories = interestedCategories;
    }


    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProfilePicture() {
        return profilePicture;
    }

    public void setProfilePicture(String profilePicture) {
        this.profilePicture = profilePicture;
    }
}
