package com.example.bikebuddy;

import android.location.Address;
import android.location.Geocoder;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;

/**
 * Class which encapulates and manages google map API objects for one "location"
 */
public class BikeBuddyLocation {

    LatLng coordinate;
    Marker marker;
    GoogleMap mMap;//reference to the map
    Address address;
    boolean isOrigin;//marker will be different depending if its the start or destination
    Geocoder gc;

    public BikeBuddyLocation(boolean isOrigin, Geocoder gc, LatLng autoCompleteLatLang, GoogleMap mMap){
        this.isOrigin = isOrigin;
        this.gc = gc;
        this.mMap = mMap;
        if(address!= null)
             this.address = address;
        coordinate= autoCompleteLatLang;
       // createMarker();
    }

    public void createMarker(){
        this.marker = mMap.addMarker(new MarkerOptions().position(coordinate));
        this.marker.setDraggable(true);
        if(isOrigin){
            this.marker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
            this.marker.setTitle("Origin");
        }else{
            this.marker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE));
            this.marker.setTitle("Destination");
        }
        this.marker.showInfoWindow();
        try {
            address= gc.getFromLocation(coordinate.latitude, coordinate.longitude,1).get(0);
            if(address!= null)
                this.marker.setSnippet(address.getLocality().toString()+ " " + address.getLocale().toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setCoordinate(LatLng coordinate){
        this.coordinate = coordinate;
    }

    //when marker was dragged from map,
    public void update(){
        coordinate = marker.getPosition();
        createMarker();
    }

}
