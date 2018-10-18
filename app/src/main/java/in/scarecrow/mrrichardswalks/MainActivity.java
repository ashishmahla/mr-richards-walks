package in.scarecrow.mrrichardswalks;

import android.Manifest;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
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
    private GoogleMap googleMap;
    private FusedLocationProviderClient fusedLpClient;
    private boolean locPermGranted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkLocationPermissions();
    }

    private void initMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment == null)
            return;

        mapFragment.getMapAsync(MainActivity.this);
    }

    private void getCurrentLocation() {
        Log.d(TAG, "getDeviceLocation: getting current device location");
        fusedLpClient = LocationServices.getFusedLocationProviderClient(MainActivity.this);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "getCurrentLocation: Not enough permissions");
            return;
        }

        Task<Location> locTask = fusedLpClient.getLastLocation();
        locTask.addOnSuccessListener(MainActivity.this, new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                Statics.showSnackbar(MainActivity.this, "Location found: " + location);
                if (location == null) {
                    createLocationRequest();
                }
            }
        }).addOnFailureListener(MainActivity.this, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Statics.showSnackbar(MainActivity.this, "Error getting current location.");
                e.printStackTrace();
            }
        });

                /*locTask.addOnCompleteListener(new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        if (task.isSuccessful()) {
                            Statics.showSnackbar(MainActivity.this, ""+task.getException()+task.getResult());
                            Location currLoc = task.getResult();
                            if (currLoc != null) {
                                moveCamera(new LatLng(currLoc.getLatitude(), currLoc.getLongitude()), CAMERA_ZOOM_LEVEL);
                            } else {
                                Log.e(TAG, "onComplete: current location found null");
                                //Statics.peekSnackbar(MainActivity.this, "Null Location");
                            }
                        } else {
                            Log.e(TAG, "onComplete: Unable to find current location");
                            Toast.makeText(MainActivity.this, "Unable to get current location.", Toast.LENGTH_LONG).show();
                        }
                    }
                });*/
    }

    private void moveCamera(LatLng latLng, float zoom) {
        Log.d(TAG, "moveCamera: moving the camera");
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));
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
            locPermGranted = true;
            initMap();
        }
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
                locPermGranted = true;
                initMap();
            }
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;
        Toast.makeText(MainActivity.this, "Map is ready to use.", Toast.LENGTH_LONG).show();
        Log.d(TAG, "onMapReady: Map is ready to use.");

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            getCurrentLocation();
            googleMap.setMyLocationEnabled(true);
        }
    }

    protected void createLocationRequest() {
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest);

        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());

        task.addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                // All location settings are satisfied. The client can initialize
                // location requests here.
                // ...
                Log.e(TAG, "onSuccess: success...???" + locationSettingsResponse.getLocationSettingsStates().isLocationUsable());
            }
        });

        task.addOnFailureListener(this, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                if (e instanceof ResolvableApiException) {
                    // Location settings are not satisfied, but this can be fixed
                    // by showing the user a dialog.
                    try {
                        // Show the dialog by calling startResolutionForResult(),
                        // and check the result in onActivityResult().
                        Log.e(TAG, "onFailure: tag01");
                        ResolvableApiException resolvable = (ResolvableApiException) e;
                        resolvable.startResolutionForResult(MainActivity.this, 898);
                    } catch (IntentSender.SendIntentException sendEx) {
                        // Ignore the error.
                        Log.e(TAG, "onFailure: tag02" + sendEx.getMessage());
                    }
                }
            }
        });
    }
}