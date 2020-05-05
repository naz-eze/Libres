package app.libres.mobile.rest;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

public class LocationResponse {

    @SerializedName("locations")
    private ArrayList<LocationModel> locations;

    public ArrayList<LocationModel> getLocations() {
        return locations;
    }

    public void setLocations(ArrayList<LocationModel> locations) {
        this.locations = locations;
    }
}
