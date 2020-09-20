package com.example.bikebuddy;

import androidx.annotation.NonNull;


import com.google.android.gms.common.api.Status;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;


import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private static final String TAG = MapsActivity.class.getSimpleName();

    public FetchWeather fw;

    private GoogleMap mMap;

    private float zoomLevel = 10.0f;
    private LatLng currentLocation;
    private List<Address> locationsList;
    private Bitmap smallMarker;


    private CameraPosition cameraPosition;
    private FusedLocationProviderClient fusedLocationProviderClient;

    // A default location (Auckland, New Zealand) and default zoom to use when location permission is
    // not granted.
    private final LatLng defaultLocation = new LatLng(-36.8483, 174.7625);
    private static final int DEFAULT_ZOOM = 15;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private boolean locationPermissionGranted;

    // The geographical location where the device is currently located. That is, the last-known
    // location retrieved by the Fused Location Provider.
    private Location lastKnownLocation;

    // Keys for storing activity state.
    private static final String KEY_CAMERA_POSITION = "camera_position";
    private static final String KEY_LOCATION = "location";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Retrieve location and camera position from saved instance state.
        if (savedInstanceState != null) {
            lastKnownLocation = savedInstanceState.getParcelable(KEY_LOCATION);
            cameraPosition = savedInstanceState.getParcelable(KEY_CAMERA_POSITION);
        }

        setContentView(R.layout.activity_maps);

        // Construct a FusedLocationProviderClient.
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }


    /**
     * Saves the state of the map when the activity is paused.
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (mMap != null) {
            outState.putParcelable(KEY_CAMERA_POSITION, mMap.getCameraPosition());
            outState.putParcelable(KEY_LOCATION, lastKnownLocation);
        }
        super.onSaveInstanceState(outState);
    }

    // init data for autocomplete to store
    private LatLng autoCompleteLatLng;
    // initialise places API
    private void initPlaces() {
        // Initialize Places.
        Places.initialize(getApplicationContext(), getResources().getString(R.string.google_maps_key));

        // Create a new Places client instance.
        PlacesClient placesClient = Places.createClient(this);
    }
    // initialise autocomplete search bar
    private void initAutoComplete() {
        final AutocompleteSupportFragment autocompleteSupportFragment =
                (AutocompleteSupportFragment) getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment);

        // restrict place field results to ID, Address, LatLng, and Name (basic data, no extra fees)
        autocompleteSupportFragment.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.ADDRESS, Place.Field.LAT_LNG, Place.Field.NAME));

        // restrict results to nz --- could be changed to grab the user's geolocated country.
        autocompleteSupportFragment.setCountry("nz");

        autocompleteSupportFragment.setOnPlaceSelectedListener((new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(@NonNull Place place) {
                // grab found location data from 'place'
                // currently just grabbing LatLng for marker making
                autoCompleteLatLng = place.getLatLng();

                // display found lat long (for debugging)
                //Toast.makeText(MapsActivity.this, "LAT"+autoCompleteLatLng.latitude+"\nLONG"+autoCompleteLatLng.longitude, Toast.LENGTH_LONG).show();

                // remove existing markers (get rid of this in final so markers added by other things aren't removed)
                mMap.clear();
                // go to found location
                mMap.animateCamera(CameraUpdateFactory.newLatLng(autoCompleteLatLng));
                // make marker
                MarkerOptions searchedLocationMarker = new MarkerOptions().position(autoCompleteLatLng).title(place.getAddress());
                mMap.addMarker(searchedLocationMarker);
            }

            @Override
            public void onError(@NonNull Status status) {

            }
        }));

    }



    @Override
    public void onMapReady(GoogleMap googleMap) {

        this.mMap = googleMap;
        smallMarker = generateIcons();

        fw = new FetchWeather(this);

        // stock google maps UI buttons
        mMap.getUiSettings().setZoomControlsEnabled(true);

        // call initialisations
        initPlaces();
        initAutoComplete();

        // start the camera above nz
       // mMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(-42, 172)));
       // mMap.moveCamera(CameraUpdateFactory.zoomTo(5));

        this.mMap.setOnCameraIdleListener(onCameraIdleListener);

        // Use a custom info window adapter to handle multiple lines of text in the
        // info window contents.
        this.mMap.setInfoWindowAdapter(customInfoWindowAdapter);


        // Prompt the user for permission.
        getLocationPermission();

        // Turn on the My Location layer and the related control on the map.
        updateLocationUI();

        // Get the current location of the device and set the position of the map.
        getDeviceLocation();
    }

    /**
     * Gets the current location of the device, and positions the map's camera.
     */
    private void getDeviceLocation() {
        /*
         * Get the best and most recent location of the device, which may be null in rare
         * cases when a location is not available.
         */
        try {
            if (locationPermissionGranted) {
                Task<Location> locationResult = fusedLocationProviderClient.getLastLocation();
                locationResult.addOnCompleteListener(this, new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        if (task.isSuccessful()) {
                            // Set the map's camera position to the current location of the device.
                            lastKnownLocation = task.getResult();
                            if (lastKnownLocation != null) {
                                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                        new LatLng(lastKnownLocation.getLatitude(),
                                                lastKnownLocation.getLongitude()), DEFAULT_ZOOM));
                            }
                        } else {
                            Log.d(TAG, "Current location is null. Using defaults.");
                            Log.e(TAG, "Exception: %s", task.getException());
                            mMap.moveCamera(CameraUpdateFactory
                                    .newLatLngZoom(defaultLocation, DEFAULT_ZOOM));
                            mMap.getUiSettings().setMyLocationButtonEnabled(false);
                        }
                    }
                });
            } else {
                //location services denied, move camera to default location
                mMap.moveCamera(CameraUpdateFactory
                        .newLatLngZoom(defaultLocation, DEFAULT_ZOOM));
                mMap.getUiSettings().setMyLocationButtonEnabled(false);

            }
        } catch (SecurityException e)  {
            Log.e("Exception: %s", e.getMessage(), e);
        }
    }

    /**
     * Prompts the user for permission to use the device location.
     */
    private void getLocationPermission() {
        /*
         * Request location permission, so that we can get the location of the
         * device. The result of the permission request is handled by a callback,
         * onRequestPermissionsResult.
         */
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            locationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    /**
     * Handles the result of the request for location permissions.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        locationPermissionGranted = false;
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    locationPermissionGranted = true;
                }
            }
        }
        updateLocationUI();
    }

    /**
     * Updates the map's UI settings based on whether the user has granted location permission.
     */
    private void updateLocationUI() {
        if (mMap == null) {
            return;
        }
        try {
            if (locationPermissionGranted) {
                mMap.setMyLocationEnabled(true);
                mMap.getUiSettings().setMyLocationButtonEnabled(true);
                getDeviceLocation();
            } else {
                mMap.setMyLocationEnabled(false);
                mMap.getUiSettings().setMyLocationButtonEnabled(false);
                lastKnownLocation = null;
                //getLocationPermission();
            }
        } catch (SecurityException e)  {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    public void testToast (String s) {
        Toast.makeText(this, s, Toast.LENGTH_LONG).show();
    }


    // Use a custom info window adapter to handle multiple lines of text in the
    // info window contents.
    private GoogleMap.InfoWindowAdapter customInfoWindowAdapter =
            new GoogleMap.InfoWindowAdapter() {
                @Override
                // Return null here, so that getInfoContents() is called next.
                public View getInfoWindow(Marker arg0) {
                    return null;
                }

                @Override
                public View getInfoContents(Marker marker) {
                    // Inflate the layouts for the info window, title and snippet.
                    View infoWindow = getLayoutInflater().inflate(R.layout.custom_info_contents,
                            (FrameLayout) findViewById(R.id.map), false);

                    TextView title = infoWindow.findViewById(R.id.title);
                    title.setText(marker.getTitle());

                    TextView snippet = infoWindow.findViewById(R.id.snippet);
                    snippet.setText(marker.getSnippet());

                    return infoWindow;
                }
            };

    private GoogleMap.OnCameraIdleListener onCameraIdleListener =
        new GoogleMap.OnCameraIdleListener() {
            @Override
            public void onCameraIdle() {
                zoomLevel = mMap.getCameraPosition().zoom;
                currentLocation = mMap.getCameraPosition().target;

            // Grid locations
            //generateLocations(currentLocation);
            //displayLocations(smallMarker);

            // Geocoder locations
            //creates new list of locations based on camera centre position.
                locationsList = getAddressListFromLatLong(currentLocation.latitude, currentLocation.longitude);
                //creates marker for each location and adds to list
                //displayLocations(smallMarker);
                getLocationsWeather();
               // addLocationsWeather();

           // checkWeatherIconDisplay();

           // makeToast(String.valueOf(zoomLevel));
            }
        };

    public  List<Address> getAddressListFromLatLong(double lat, double lng) {

        Geocoder geocoder = new Geocoder(this);

        List<Address> list = null;
        try {
            list = geocoder.getFromLocation(lat, lng, 20);

            // 20 is no of address you want to fetch near by the given lat-long

        } catch (Throwable e) {
            e.printStackTrace();
        }

        return list;
    }

/*    public void displayLocations(Bitmap smallMarker) {

        mMap.clear();
        for (Address a : locationsList) {
            System.out.println(a.getLocale());


            LatLng aLatLng = new LatLng(a.getLatitude(), a.getLongitude());
            Marker marker = mMap.addMarker(new MarkerOptions().position(aLatLng).title("M").snippet("000")
                    .icon(BitmapDescriptorFactory.fromBitmap(smallMarker)));
            //markerArray.add(marker);
        }
    }*/

    public void getLocationsWeather() {

        mMap.clear();
/*        for (Address a : locationsList) {
            fw.fetch(a.getLatitude(), a.getLongitude());
            //delete a from list after request?
        }*/

        //had to change to iterator in order to delete
        Iterator<Address> it = locationsList.iterator();
        while (it.hasNext()) {
            Address a = it.next();
            fw.fetch(a.getLatitude(), a.getLongitude());
            it.remove();
        }
    }

    public void addLocationsWeather(double lat, double lon, String description) {
        //option 1 using address object, creates address object with everything else set to null, uses URL as description
        // allows to use the same locationslist but the addresses are nto that useful, could reverse locate to get full address.
      //  Address a = new Address(Locale.ENGLISH);
      //  a.setLatitude(lat);
      //  a.setLongitude(lon);
      //  a.setUrl(description);
       // locationsList.add(a);

        //option 2 just creates a marker with lat, lon, description in a seperate array to the locations
        //similar to the previous displaylocations method
        LatLng latLng = new LatLng(lat, lon);
        //Marker marker =

        //option 3 similar to above but doesn't store the markers just displays them (this could have issues with the weather toggle)
        //LatLng latLng = new LatLng(lat, lon);
        mMap.addMarker(new MarkerOptions().position(latLng).title(description).snippet("000")
                .icon(BitmapDescriptorFactory.fromBitmap(smallMarker)));
    }

    public Bitmap generateIcons() {
        // custom the size of the weather icon
        int height = 100;
        int width = 100;
        BitmapDrawable bitmapdraw=(BitmapDrawable)getResources().getDrawable(R.drawable.lighting);
        Bitmap b=bitmapdraw.getBitmap();
        Bitmap smallMarker = Bitmap.createScaledBitmap(b, width, height, false);

        return smallMarker;

    }
}