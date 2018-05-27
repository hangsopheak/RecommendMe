package pl.pollub.myrecommendation.models;

public class LocationRecommendation extends Recommendation {

    protected String placeId;
    protected Location location;

    public String getPlaceId() {
        return placeId;
    }

    public void setPlaceId(String placeId) {
        this.placeId = placeId;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }
}
