package in.scarecrow.mrrichardswalks;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import in.scarecrow.mrrichardswalks.utils.Statics;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "MainActivity";
    private static final int RC_LOCATION_PERMS = 2389;
    private static final float CAMERA_ZOOM_LEVEL = 15f;
    private static final String KEY_REQUESTING_LOCATION_UPDATES = "req-loc-update-key";
    private static final int RC_LOCATION_RESOLUTION = 9382;
    private static final LatLng DEFAULT_MAP_LOCATION = new LatLng(12, 77);
    LocationRequest mLocationRequest;
    private GoogleMap googleMap;
    private FusedLocationProviderClient fusedLpClient;
    private LocationCallback mLocationCallback;
    private boolean mRequestingLocationUpdates = true;
    private Location mCurrentLocation;
    private boolean presentLocationTracedOnce = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // handle saved values
        updateValuesFromBundle(savedInstanceState);

        initFusedLpClient();

        // define location request properties
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        // define location callback
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    Log.e(TAG, "onLocationResult: error location is null in callback");
                    return;
                }
                Log.e(TAG, "onLocationResult: callback location found : " + locationResult.getLocations());
                for (Location location : locationResult.getLocations()) {
                    // Update current location data
                    mCurrentLocation = location;
                    if (!presentLocationTracedOnce) {
                        presentLocationTracedOnce = true;
                        moveToCurrentLocation();
                    }

                    updateMapUI();
                }
            }
        };

        initMap();

        // finally check required permissions and init map on permissions found
        checkLocationPermissions();
    }

    private void initFusedLpClient() {
        if (fusedLpClient == null) {
            fusedLpClient = LocationServices.getFusedLocationProviderClient(MainActivity.this);
        }
    }

    private void checkLocationPermissions() {
        String reqPerms[] = new String[]{
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
        };

        boolean gotAllPerms = true;
        for (String perm : reqPerms) {
            if (ContextCompat.checkSelfPermission(this, perm)
                    != PackageManager.PERMISSION_GRANTED) {

                gotAllPerms = false;
                ActivityCompat.requestPermissions(MainActivity.this, reqPerms, RC_LOCATION_PERMS);
            }
        }
        if (gotAllPerms) {
            Log.d(TAG, "checkLocationPermissions: got all perms, init map");
            checkLocationSettings();
        }
    }

    private void initMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment == null)
            return;
        mapFragment.getMapAsync(MainActivity.this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;
        Log.d(TAG, "onMapReady: Map is ready to use.");
        moveCamera(DEFAULT_MAP_LOCATION, 3f);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            googleMap.setMyLocationEnabled(true);
        }
    }

    protected void checkLocationSettings() {

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest);

        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());

        task.addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                // All location settings are satisfied. The client can initialize
                // location requests here.
                Log.e(TAG, "onSuccess: success..??? sometimes not");
                initLocation();

            }
        }).addOnFailureListener(this, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                if (e instanceof ResolvableApiException) {
                    // Location settings are not satisfied, but this can be fixed
                    // by showing the user a dialog.
                    try {
                        // Show the dialog by calling startResolutionForResult(),
                        // and check the result in onActivityResult().
                        Log.e(TAG, "onFailure: found resolvable error with location settings");
                        ResolvableApiException resolvable = (ResolvableApiException) e;
                        resolvable.startResolutionForResult(MainActivity.this, RC_LOCATION_RESOLUTION);
                    } catch (IntentSender.SendIntentException sendEx) {
                        // Ignore the error.
                        Log.e(TAG, "onFailure: error resolving location settings" + sendEx.getMessage());
                    }
                }
            }
        });
    }

    private void initLocation() {
        getCurrentLocation();
    }

    private void getCurrentLocation() {
        Log.d(TAG, "getDeviceLocation: getting current device location");

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        Task<Location> locTask = fusedLpClient.getLastLocation();
        locTask.addOnSuccessListener(MainActivity.this, new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                // TODO: 19-10-2018 debugging line, delete it
                Statics.peekSnackbar(MainActivity.this, "Location found: " + location);

                // location should not be null
                if (location != null) {
                    mCurrentLocation = location;
                    moveToCurrentLocation();
                }
            }
        }).addOnFailureListener(MainActivity.this, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Statics.showSnackbar(MainActivity.this, "Error getting current location.");
                e.printStackTrace();
            }
        });
    }

    private void updateMapUI() {
        // TODO: 19-10-2018
    }

    private void moveToCurrentLocation() {
        moveCamera(new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude()), CAMERA_ZOOM_LEVEL);
    }

    @SuppressWarnings("SameParameterValue")
    private void moveCamera(LatLng latLng, float zoom) {
        Log.d(TAG, "moveCamera: moving the camera");
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == RC_LOCATION_PERMS) {
            if (grantResults.length > 0) {
                for (int result : grantResults) {
                    if (result != PackageManager.PERMISSION_GRANTED) {
                        Log.d(TAG, "onRequestPermissionsResult: permissions rejected");
                        Toast.makeText(MainActivity.this, "Mr. Richard, you didn't provide required permissions. Terminating.", Toast.LENGTH_LONG).show();
                        MainActivity.this.finish();
                    }
                }

                // got all permissions
                Log.d(TAG, "onRequestPermissionsResult: just got all perms, init map");
                checkLocationSettings();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        switch (requestCode) {
            case RC_LOCATION_RESOLUTION:

                // if location service is online, location callback will handle the update. else show warning.
                if (resultCode != Activity.RESULT_OK) {
                    Toast.makeText(MainActivity.this, "Location disabled.", Toast.LENGTH_SHORT).show();
                }
                break;

            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mRequestingLocationUpdates) {
            startLocationUpdates();
        }
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        Log.d(TAG, "startLocationUpdates: Listening to location changes");
        initFusedLpClient();
        fusedLpClient.requestLocationUpdates(mLocationRequest,
                mLocationCallback,
                null);
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
    }

    private void stopLocationUpdates() {
        if (fusedLpClient != null) {
            fusedLpClient.removeLocationUpdates(mLocationCallback);
            Log.d(TAG, "stopLocationUpdates: stopped listening to location changes");
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(KEY_REQUESTING_LOCATION_UPDATES,
                mRequestingLocationUpdates);

        super.onSaveInstanceState(outState);
    }

    private void updateValuesFromBundle(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            return;
        }

        // Update the value of mRequestingLocationUpdates from the Bundle.
        if (savedInstanceState.keySet().contains(KEY_REQUESTING_LOCATION_UPDATES)) {
            mRequestingLocationUpdates = savedInstanceState.getBoolean(
                    KEY_REQUESTING_LOCATION_UPDATES);
        }

        // todo Update UI to match restored state
    }
}