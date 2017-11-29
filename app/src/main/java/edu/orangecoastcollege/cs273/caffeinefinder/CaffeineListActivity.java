package edu.orangecoastcollege.cs273.caffeinefinder;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.List;

//TODO: Implement the following interfaces:
//TODO: GoogleApiClient.ConnectionCallbakcs, GoogleApiClient.OnConnectionFailedListener and LocationListener
public class CaffeineListActivity extends AppCompatActivity
        implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    public static final int COARSE_LOCATION_REQUEST_CODE = 100;
    private DBHelper db;
    private List<CaffeineLocation> mAllCaffeineLocationsList;
    private ListView mCaffeineLocationsListView;
    private LocationListAdapter mCaffeineLocationListAdapter;
    private GoogleMap mMap;


    //TODO: Add member variables for the GoogleApiClient, CaffeineLocation and LocationRequest

    // Google API client is "fused" service for all apps on the device (Location, maps, play store)
    private GoogleApiClient mGoogleApiClient;
    // Last location is the last latitude and longitude reported
    private Location mLastLocation;
    // Location request are made every x seconds (we configure this)
    private LocationRequest mLocationRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_caffeine_list);

        deleteDatabase(DBHelper.DATABASE_NAME);
        db = new DBHelper(this);
        db.importLocationsFromCSV("locations.csv");

        mAllCaffeineLocationsList = db.getAllCaffeineLocations();
        mCaffeineLocationsListView = (ListView) findViewById(R.id.locationsListView);
        mCaffeineLocationListAdapter = new LocationListAdapter(this, R.layout.location_list_item, mAllCaffeineLocationsList);
        mCaffeineLocationsListView.setAdapter(mCaffeineLocationListAdapter);

        //TODO: If the connection to the GoogleApiClient is null, build a new one:
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }

        //TODO: Make a LocationRequest every 10 seconds, with a fastest interval of 1 second with high accuracy
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY)
                .setInterval(30 * 1000)
                .setFastestInterval(1 * 1000);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.caffeineMapFragment);
        mapFragment.getMapAsync(this);
    }

    //TODO: Override the onStart method to connect to the GoogleApiClient

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();

    }

    //TODO: Override the onStop method to disconnect from the GoogleApiClient

    @Override
    protected void onStop() {
        super.onStop();
        mGoogleApiClient.disconnect();
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

    }

    //TODO: Define a new method: private void handleNewLocation(CaffeineLocation newLocation) that will
    //TODO: update the user's location (custom marker) on the Google Map and rebuild all markers for the Caffeine Locations (standard markers)
    private void handleNewLocation(Location newLocation) {
        mLastLocation = newLocation;
        mMap.clear();

        // Add special marker (blue) for "my" location
        LatLng myCoordinate = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
        mMap.addMarker(new MarkerOptions()
                .position(myCoordinate)
                .title("Current Location")
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.my_marker)));
        CameraPosition cameraPosition = new CameraPosition.Builder().target(myCoordinate).zoom(15.0f).build();
        CameraUpdate cameraUpdate = CameraUpdateFactory.newCameraPosition(cameraPosition);
        mMap.moveCamera(cameraUpdate);

        // Add normal markers for all caffeine locations
        for (CaffeineLocation caffeineLocation : mAllCaffeineLocationsList) {
            LatLng coordinate = new LatLng(caffeineLocation.getLatitude(), caffeineLocation.getLongitude());
            mMap.addMarker(new MarkerOptions().position(coordinate).title(caffeineLocation.getName()));
        }
    }


    public void viewLocationDetails(View view) {
        if (view instanceof LinearLayout) {
            LinearLayout selectedLayout = (LinearLayout) view;
            CaffeineLocation selectedCaffeineLocation = (CaffeineLocation) selectedLayout.getTag();

            Log.i("Caffeine Finder", selectedCaffeineLocation.toString());
            Intent detailsIntent = new Intent(this, CaffeineDetailsActivity.class);

            detailsIntent.putExtra("SelectedLocation", selectedCaffeineLocation);
            detailsIntent.putExtra("MyLocation", mLastLocation); // adding intent
            startActivity(detailsIntent);
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

        // Get the last Location from Google services
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            // Don't have either COARSE or FINE permissions, so request them:
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    COARSE_LOCATION_REQUEST_CODE);
            return;
        }
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        handleNewLocation(mLastLocation);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            mLastLocation = new Location("");
            mLastLocation.setLatitude(0.0);
            mLastLocation.setLongitude(0.0);
        }
        else
        {
            mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        }
        handleNewLocation(mLastLocation);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.e("Caffeine Finder",
                "Connection to Location services failed: " + connectionResult.getErrorMessage());

    }

    @Override
    public void onLocationChanged(Location location) {
        handleNewLocation(location);

    }

    //TODO: In overriden onConnected method, get the last location, then request location updates and handle the new location

    //TODO: Override onRequestedPermissionsResult.  If fine/coarse location is granted, updated the last location
    //TODO: Else, initialize mLastLocation to a new location with latitude 0.0 and longitude 0.0
    //TODO: In either case, handle the new location.


    //TODO: Create a new method: public void findClosestCaffeine, which will be invoked when a user clicks on the button
    //TODO: Loop through all the caffeine locations and find the one with the minimum distance.
    //TODO: Then, fire off an Intent to the details page and put both the SelectedLocation and MyLocation
    public void findClosestCaffeine(View v)
    {
        double minDistance = Double.MAX_VALUE;
        CaffeineLocation closestLocation = null;
        double distance;
        Location tempLocation = new Location("");

        // Loop through the list of caffeine souces
        for (CaffeineLocation c : mAllCaffeineLocationsList)
        {
            // convert out caffeine location into a (google) location
            tempLocation.setLatitude(c.getLatitude());
            tempLocation.setLongitude(c.getLongitude());
            distance = tempLocation.distanceTo(mLastLocation);
            if (distance < minDistance)
            {
                minDistance = distance;
                closestLocation = c;
            }
        }

        // Intent to the details page
        Intent detailsIntent = new Intent(this, CaffeineDetailsActivity.class);
        // Sending over to intent: putting both markers on the map
        detailsIntent.putExtra("SelectedLocation", closestLocation);
        detailsIntent.putExtra("MyLocation", mLastLocation);
        startActivity(detailsIntent);
    }

}
