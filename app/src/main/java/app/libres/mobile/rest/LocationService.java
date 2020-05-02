package app.libres.mobile.rest;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface LocationService {
    @POST("/api/location/add")
    Call<Void> pushLocation(@Body LocationModel locationModel);

    @GET("/api/location")
    Call<LocationResponse> getLocations();

}
