package app.libres.android;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.AutocompleteActivity;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.maps.android.heatmaps.HeatmapTileProvider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import app.libres.android.rest.LocationModel;
import app.libres.android.rest.LocationResponse;
import app.libres.android.rest.LocationService;
import app.libres.android.rest.RetrofitClient;
import app.libres.android.service.NotificationService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.ACCESS_NETWORK_STATE;
import static android.location.LocationManager.GPS_PROVIDER;
import static com.google.android.gms.maps.CameraUpdateFactory.newCameraPosition;
import static com.google.android.material.snackbar.BaseTransientBottomBar.LENGTH_LONG;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, LocationListener {

    private static final String TAG = MapsActivity.class.getSimpleName();
    private static final int DEFAULT_ZOOM = 14;
    public static final int ONE_KM_RADIUS = 1000;
    private static final int AUTOCOMPLETE_REQUEST_CODE = 1;
    private static final int PERMISSION_REQUEST_CODE = 2;
    private static final int HTTP_STATUS_CREATED = 201;


    private LocationManager locationManager;
    private GoogleMap mMap;
    final Location gpsLocation = new Location("GPS");
    private LatLng mDefaultLocation = new LatLng(40.4378698, -3.8196207); //Madrid as default location.
    private Place userPlace;
    private TileOverlay mOverlay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        String apiKey = getString(R.string.google_maps_key);
        if (!Places.isInitialized()) Places.initialize(this, apiKey);
        PlacesClient placesClient = Places.createClient(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("");

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        final FloatingActionButton fab = findViewById(R.id.places_button);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                List<Place.Field> fields = Arrays.asList(Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG);
                Intent intent = new Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields)
                        .setCountry("ES")
                        .setHint("¿Dónde vives?")
                        .build(getApplicationContext());
                startActivityForResult(intent, AUTOCOMPLETE_REQUEST_CODE);
            }
        });

        final FloatingActionButton broadcast = findViewById(R.id.broadcast_button);
        final boolean[] broadcastFlag = {true};
        broadcast.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (broadcastFlag[0]) {
                    if (ActivityCompat.checkSelfPermission(getApplicationContext(), ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                            || ActivityCompat.checkSelfPermission(getApplicationContext(), ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                            || ActivityCompat.checkSelfPermission(getApplicationContext(), ACCESS_NETWORK_STATE) != PackageManager.PERMISSION_GRANTED
                            || !locationManager.isProviderEnabled(GPS_PROVIDER)) {
                        Toast.makeText(getApplicationContext(), "Tu ubicación no está habilitada", Toast.LENGTH_SHORT).show();
                    } else {
                        broadcast.setBackgroundTintList(ColorStateList.
                                valueOf(getResources().getColor(R.color.colorAccent)));
                        startUpdatingHeatMap();
                        broadcastFlag[0] = false;
                    }
                } else {
                    broadcast.setBackgroundTintList(ColorStateList.
                            valueOf(getResources().getColor(R.color.colorGrey)));
                    stopUpdatingHeatMap();
                    broadcastFlag[0] = true;
                }
            }
        });

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        ImageView libresLogo = findViewById(R.id.libres_logo);
        libresLogo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://libres.app/"));
                v.getContext().startActivity(browserIntent);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == AUTOCOMPLETE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                userPlace = Autocomplete.getPlaceFromIntent(data);
                if (userPlace.getLatLng() != null) setHomeMarker(userPlace.getLatLng(), userPlace.getAddress());
            } else if (resultCode == AutocompleteActivity.RESULT_ERROR) {
                Log.e(TAG, "An error occurred: " + resultCode);
                Status status = Autocomplete.getStatusFromIntent(data);
                Log.i(TAG, status.getStatusMessage());
            }
        }
    }

    private void setHomeMarker(LatLng home, String address) {
        mMap.clear();

        String radiusColour = "#5090EE90";
        Location current = getCurrentLocation();
        Location homeLocation = new Location("home");
        homeLocation.setLatitude(home.latitude);
        homeLocation.setLongitude(home.longitude);

        if (current != null && current.getLatitude() != 0 && current.distanceTo(homeLocation) > 1000) {
            View parentLayout = findViewById(android.R.id.content);
            Snackbar snackbar = Snackbar.make(parentLayout, "Estás a más de 1 KM de casa! #quedateEnCasa", LENGTH_LONG);
            View snackBarView = snackbar.getView();
            snackBarView.setBackgroundColor(getResources().getColor(R.color.colorRed));
            TextView textView = snackBarView.findViewById(R.id.snackbar_text);
            textView.setTextColor(getResources().getColor(R.color.colorWhite));
            textView.setTextSize(14);
            snackbar.show();
        }

        mMap.addCircle(new CircleOptions()
                .center(home)
                .radius(ONE_KM_RADIUS)
                .strokeWidth(2)
                .strokeColor(Color.parseColor("#00CC00"))
                .fillColor(Color.parseColor(radiusColour)));

        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(home)
                .zoom(DEFAULT_ZOOM).build();

        mMap.addMarker(new MarkerOptions().position(home)
                .title(address))
                .setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_home_blue_24));

        mMap.animateCamera(newCameraPosition(cameraPosition));

        if (mOverlay != null) {
            fetchHeatMap();
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        boolean success = googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.style_json));
        if (!success) {
            Log.e(TAG, "Style parsing failed.");
        }
        mMap.setOnCameraIdleListener(new GoogleMap.OnCameraIdleListener() {
            @Override
            public void onCameraIdle() {
                float zoomLevel = mMap.getCameraPosition().zoom;
                if (mOverlay != null && zoomLevel > 14) {
                    mOverlay.setVisible(false);
                } else if (mOverlay != null && !mOverlay.isVisible()) {
                    mOverlay.setVisible(true);
                }
            }
        });
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mDefaultLocation, 5.5f));
        getCurrentLocation();
    }

    private Location getCurrentLocation() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (ActivityCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, ACCESS_NETWORK_STATE) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{ACCESS_FINE_LOCATION,
                    ACCESS_COARSE_LOCATION, ACCESS_NETWORK_STATE}, PERMISSION_REQUEST_CODE);
            Log.d(TAG, "Requesting location access again.");
            return null;

        } else {
            Location location = locationManager.getLastKnownLocation(GPS_PROVIDER);
            mMap.setMyLocationEnabled(true);
            if (location != null) {
                gpsLocation.setLatitude(location.getLatitude());
                gpsLocation.setLongitude(location.getLongitude());
            }
        }
        return gpsLocation;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions,
                                           int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 1
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (userPlace != null && userPlace.getLatLng() != null) {
            Intent intent = new Intent(this, NotificationService.class);
            intent.putExtra("latitude", userPlace.getLatLng().latitude);
            intent.putExtra("longitude", userPlace.getLatLng().longitude);
            startService(intent);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        stopService(new Intent(this, NotificationService.class));
        if (ActivityCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, ACCESS_NETWORK_STATE) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{ACCESS_FINE_LOCATION,
                    ACCESS_COARSE_LOCATION, ACCESS_NETWORK_STATE}, PERMISSION_REQUEST_CODE);
            Log.d(TAG, "Requesting location access again.");
        } else {
            locationManager.requestLocationUpdates(GPS_PROVIDER, 200, 1f, this);
            if (userPlace != null && userPlace.getLatLng() != null) {
                setHomeMarker(userPlace.getLatLng(), userPlace.getAddress());
            }
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        gpsLocation.setLatitude(location.getLatitude());
        gpsLocation.setLongitude(location.getLongitude());
        mMap.setMyLocationEnabled(true);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {
        mMap.setMyLocationEnabled(true);
    }

    @Override
    public void onProviderDisabled(String provider) {
        mMap.setMyLocationEnabled(false);
        stopUpdatingHeatMap();

        final Context context = new ContextThemeWrapper(MapsActivity.this, R.style.AppTheme2);
        new MaterialAlertDialogBuilder(context)
                .setMessage("Tu ubicación no está habilitada")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("¿Quieres activar tu ubicación?", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                        context.startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                }).show();
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        locationManager.removeUpdates(this);
    }

    private void fetchHeatMap() {
        final LocationService locationService = RetrofitClient.getInstance(getApplicationContext()).create(LocationService.class);
        locationService.getLocations().enqueue(new Callback<LocationResponse>() {
            @Override
            public void onResponse(Call<LocationResponse> call, Response<LocationResponse> response) {
                if (response.body() != null) {
                    List<LocationModel> locationModels = response.body().getLocations();
                    if (!locationModels.isEmpty()) addHeatMap(locationModels);
                }
            }

            @Override
            public void onFailure(Call<LocationResponse> call, Throwable t) {
                Log.e(TAG, "Error occurred fetching locations");
            }
        });
    }

    private void pushLocation(Location location) {
        if (location != null && location.getLatitude() != 0 && location.getLongitude() != 0) {
            LocationModel locationModel = new LocationModel(location.getLatitude(), location.getLongitude());
            final LocationService locationService = RetrofitClient.getInstance(getApplicationContext())
                    .create(LocationService.class);

            locationService.pushLocation(locationModel).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    if (response.code() == HTTP_STATUS_CREATED) {
                        Log.i(TAG, "Location successfully sent");
                    }
                }

                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    Log.e(TAG, "Error occurred pushing your location");
                }
            });
        }
    }

    private void addHeatMap(List<LocationModel> locationModels) {
        if (!locationModels.isEmpty()) {
            List<LatLng> list = new ArrayList<>();
            for (LocationModel loc : locationModels) {
                list.add(new LatLng(loc.getLatitude(), loc.getLongitude()));
            }
            HeatmapTileProvider provider = new HeatmapTileProvider.Builder()
                    .data(list)
                    .build();
            if (mOverlay != null) {
                mOverlay.clearTileCache();
                mOverlay.remove();
                mOverlay = null;
            }
            mOverlay = mMap.addTileOverlay(new TileOverlayOptions().tileProvider(provider));
        }
    }

    private final Handler handler = new Handler();
    private Timer heatMapTimer;
    private TimerTask heatMapTimerTask;

    public void startUpdatingHeatMap() {
        fetchHeatMap(); //initial fetch
        heatMapTimer = new Timer();
        initializeTimerTask();
        heatMapTimer.schedule(heatMapTimerTask, 5000, 60 * 1000); //
    }

    public void stopUpdatingHeatMap() {
        if (mOverlay != null) {
            mOverlay.clearTileCache();
            mOverlay.remove();
            mOverlay = null;
        }
        if (heatMapTimer != null) {
            heatMapTimer.cancel();
            heatMapTimer = null;
        }
    }

    public void initializeTimerTask() {
        heatMapTimerTask = new TimerTask() {
            public void run() {
                handler.post(new Runnable() {
                    public void run() {
                        fetchHeatMap();
                        pushLocation(getCurrentLocation());
                    }
                });
            }
        };
    }
}
