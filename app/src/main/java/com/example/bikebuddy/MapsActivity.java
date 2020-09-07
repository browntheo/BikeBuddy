package com.example.bikebuddy;

import androidx.fragment.app.FragmentActivity;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.renderscript.ScriptGroup;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private static final String TAG = "";
    private GoogleMap mMap;
    //push test PK
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);



    }

    // search bar text
    private EditText mSearchText;

    // initialise search bar
    private void initSearchBar() {
        Log.d(TAG, "initSearchBar: initialising");
        mSearchText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionID, KeyEvent keyEvent) {
                if(actionID == EditorInfo.IME_ACTION_SEARCH
                    || actionID == EditorInfo.IME_ACTION_DONE
                    || keyEvent.getAction() == keyEvent.ACTION_DOWN
                    || keyEvent.getAction() == keyEvent.KEYCODE_ENTER
                    || keyEvent.getAction() == keyEvent.KEYCODE_NUMPAD_ENTER)
                {
                    geoLocateBySearch();
                }

                return false;
            }
        });
    }

    // perform search
    private void geoLocateBySearch() {
        Log.d(TAG, "geoLocateBySearch: starting");

        String searchString = mSearchText.getText().toString();

        Geocoder geocoder = new Geocoder(MapsActivity.this);
        List<Address> addressList = new ArrayList<>();
        try {
            // try adding found results into list (currently only 1 result max)
            addressList = geocoder.getFromLocationName(searchString, 1);

        }catch (IOException e) {
            Log.e(TAG, "geoLocateBySearch: Exception caught: " + e.getMessage());
        }

        hideKeyboard();

        // if address results were added to list:
        if(addressList.size() > 0) {
            // grab address
            Address address = addressList.get(0);

            // output address to log and toast
            Toast.makeText(this, address.toString(), Toast.LENGTH_LONG).show();
            Log.d(TAG, "geoLocateBySearch: found address: " + address.toString());

            // store latitude and longitude
            LatLng foundLocation = new LatLng(address.getLatitude(), address.getLongitude());
            // go to found location
            mMap.clear();
            mMap.animateCamera(CameraUpdateFactory.newLatLng(foundLocation));
            // make marker
            MarkerOptions searchedLocationMarker = new MarkerOptions().position(foundLocation).title(address.getAddressLine(0));
            mMap.addMarker(searchedLocationMarker);
        }
    }

    // hide the popup keyboard
    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager inputMethodManager = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // stock google maps UI buttons
        mMap.getUiSettings().setZoomControlsEnabled(true);

        // search bar init stuff
        mSearchText = (EditText) findViewById(R.id.input_search);
        initSearchBar();

        // Add a marker in Sydney and move the camera
        LatLng sydney = new LatLng(-34, 151);
        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
    }
}