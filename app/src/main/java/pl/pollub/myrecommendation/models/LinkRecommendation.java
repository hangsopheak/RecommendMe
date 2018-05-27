package pl.pollub.myrecommendation.models;

public class LinkRecommendation extends Recommendation {

    protected String websiteUrl;

    public LinkRecommendation(){
        super();
    }

    public String getWebsiteUrl() {
        return websiteUrl;
    }

    public void setWebsiteUrl(String websiteUrl) {
        this.websiteUrl = websiteUrl;
    }

}
