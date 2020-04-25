package app.libres;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

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
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.Arrays;
import java.util.List;

import static com.google.android.gms.maps.CameraUpdateFactory.newCameraPosition;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "app.libres.MapsActivity";
    private static final int DEFAULT_ZOOM = 14;
    public static final int ONE_KM_RADIUS = 1000;
    private static final int AUTOCOMPLETE_REQUEST_CODE = 1;

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
        mMap.addCircle(new CircleOptions()
                .center(home)
                .radius(ONE_KM_RADIUS)
                .strokeWidth(2)
                .strokeColor(Color.GREEN)
                .fillColor(Color.parseColor("#50C1FCC1")));

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
    }
}
