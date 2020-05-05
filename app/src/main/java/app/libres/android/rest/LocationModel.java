package app.libres.android.rest;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class LocationModel implements Serializable {

    @SerializedName("latitude")
    private Double latitude;
    @SerializedName("longitude")
    private Double longitude;

    public LocationModel() {
    }

    public LocationModel(Double latitude, Double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }
}
