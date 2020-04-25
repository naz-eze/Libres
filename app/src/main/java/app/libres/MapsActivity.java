package app.libres;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

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
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.AutocompleteActivity;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.Arrays;
import java.util.List;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.ACCESS_NETWORK_STATE;
import static android.location.LocationManager.GPS_PROVIDER;
import static com.google.android.gms.maps.CameraUpdateFactory.newCameraPosition;
import static com.google.android.material.snackbar.BaseTransientBottomBar.LENGTH_INDEFINITE;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "app.libres.MapsActivity";
    private static final int DEFAULT_ZOOM = 14;
    public static final int ONE_KM_RADIUS = 1000;
    private static final int AUTOCOMPLETE_REQUEST_CODE = 1;
    private static final int PERMISSION_REQUEST_CODE = 2;

    private GoogleMap mMap;
    private LatLng mDefaultLocation = new LatLng(40.4378698, -3.8196207); //Madrid as default location.

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        String apiKey = getString(R.string.google_maps_key);
        if (!Places.isInitialized()) Places.initialize(this, apiKey);
        PlacesClient placesClient = Places.createClient(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("");
        toolbar.inflateMenu(R.menu.main_menu);

        FloatingActionButton fab = findViewById(R.id.places_button);
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

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == AUTOCOMPLETE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                Place place = Autocomplete.getPlaceFromIntent(data);
                setHomeMarker(place.getLatLng(), place.getAddress());
            } else if (resultCode == AutocompleteActivity.RESULT_ERROR) {
                Log.e(TAG, "An error occurred: " + resultCode);
                Status status = Autocomplete.getStatusFromIntent(data);
                Log.i(TAG, status.getStatusMessage());
            } else if (resultCode == RESULT_CANCELED) { /* */}
        }
    }

    private void setHomeMarker(LatLng home, String address) {
        mMap.clear();
        String radiusColour = "#50C1FCC1";
        Location current = getCurrentLocation();
        Location homeLoc = new Location("home");
        homeLoc.setLatitude(home.latitude);
        homeLoc.setLongitude(home.longitude);

        if (current != null && current.distanceTo(homeLoc) > 1000) {
            radiusColour = "#50FF3333";
            View parentLayout = findViewById(android.R.id.content);
            Snackbar snackbar = Snackbar.make(parentLayout, "Estás a más de 1 KM de casa! #quedateEnCasa", LENGTH_INDEFINITE);
            View snackBarView = snackbar.getView();
            snackBarView.setBackgroundColor(getResources().getColor(R.color.colorPrimary));

            TextView textView = snackBarView.findViewById(R.id.snackbar_text);
            textView.setTextColor(getResources().getColor(R.color.colorWhite));
            textView.setTextSize(14);
            snackbar.show();
        }

        mMap.addCircle(new CircleOptions()
                .center(home)
                .radius(ONE_KM_RADIUS)
                .strokeWidth(2)
                .strokeColor(Color.parseColor(radiusColour))
                .fillColor(Color.parseColor(radiusColour)));

        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(home)
                .zoom(DEFAULT_ZOOM).build();

        mMap.addMarker(new MarkerOptions().position(home)
                .title(address))
                .setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_home_blue_24));

        mMap.animateCamera(newCameraPosition(cameraPosition));
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        boolean success = googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.style_json));
        if (!success) {
            Log.e(TAG, "Style parsing failed.");
        }
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mDefaultLocation, 5));
        getCurrentLocation();
    }

    private Location getCurrentLocation() {
        Location gpsLocation;
        double longitude;
        double latitude;

        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (ActivityCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, ACCESS_NETWORK_STATE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{ACCESS_FINE_LOCATION,
                    ACCESS_COARSE_LOCATION, ACCESS_NETWORK_STATE}, PERMISSION_REQUEST_CODE);
            Log.d(TAG, "Requesting location access again.");
            return null;
        } else if (!locationManager.isProviderEnabled(GPS_PROVIDER)) {
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

        try {
            gpsLocation = locationManager.getLastKnownLocation(GPS_PROVIDER);
            if (gpsLocation != null) {
                latitude = gpsLocation.getLatitude();
                longitude = gpsLocation.getLongitude();

                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                        new LatLng(latitude, longitude), 10));

                mMap.setMyLocationEnabled(true);
                return gpsLocation;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
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
}
