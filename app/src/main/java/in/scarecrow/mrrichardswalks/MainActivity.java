package in.scarecrow.mrrichardswalks;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.location.places.AutocompletePrediction;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import in.scarecrow.mrrichardswalks.libs.DataParser;
import in.scarecrow.mrrichardswalks.libs.PlaceAutoCompleteAdapter;
import in.scarecrow.mrrichardswalks.models.ParsedData;
import in.scarecrow.mrrichardswalks.models.WayPointAdapter;
import in.scarecrow.mrrichardswalks.utils.DirectionUrlUtils;
import in.scarecrow.mrrichardswalks.utils.GpsAnimUtil;
import in.scarecrow.mrrichardswalks.utils.Statics;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleApiClient.OnConnectionFailedListener, WayPointAdapter.WayPointClickListener {

    private static final String TAG = "MainActivity";
    private static final int RC_LOCATION_PERMS = 2389;
    private static final float CAMERA_ZOOM_LEVEL = 15f;
    private static final String KEY_REQUESTING_LOCATION_UPDATES = "req-loc-update-key";
    private static final int RC_LOCATION_RESOLUTION = 9382;
    private static final LatLng DEFAULT_MAP_LOCATION = new LatLng(12, 77);
    private static final LatLngBounds LAT_LNG_BOUNDS = new LatLngBounds(
            new LatLng(12.8739, 77.756),
            new LatLng(13.0939, 77.45244)
    );
    private static final float DEFAULT_MARKER_HUE = BitmapDescriptorFactory.HUE_YELLOW;
    RecyclerView rv_waypoints;
    WayPointAdapter wayPointAdapter;
    CardView cv_locate_me;
    RequestQueue queue;
    private LocationRequest mLocationRequest;
    private AutoCompleteTextView actv_addrSearch;
    private TextView tv_addr_add;
    private ImageView iv_add_icon;
    private CardView cv_recent_pin_addr, cv_get_route;
    private ImageView iv_gps;
    private TextView tv_waypoint_count;
    private GoogleMap googleMap;
    private FusedLocationProviderClient fusedLpClient;
    private LocationCallback mLocationCallback;
    private boolean mRequestingLocationUpdates = true;
    private Location mCurrentLocation;
    private boolean presentLocationTracedOnce = false;
    private PlaceAutoCompleteAdapter autoCompleteAdapter;
    private GoogleApiClient mGoogleApiClient;
    private ArrayList<Marker> markersList = new ArrayList<>();
    private Marker recentMarker;
    private int markerNumber = 1;
    private ResultCallback<PlaceBuffer> mUpdatePlaceDetailsCallback = new ResultCallback<PlaceBuffer>() {
        @Override
        public void onResult(@NonNull PlaceBuffer places) {
            if (!places.getStatus().isSuccess()) {
                Log.d(TAG, "onResult: Place query did not complete successfully: " + places.getStatus().toString());
                places.release();
                return;
            }
            final Place place = places.get(0);

            dropPin(new LatLng(Objects.requireNonNull(place.getViewport()).getCenter().latitude,
                    place.getViewport().getCenter().longitude), CAMERA_ZOOM_LEVEL, place.getName().toString());

            places.release();
        }
    };
    private AdapterView.OnItemClickListener mAutocompleteClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            hideSoftKeyboard();

            final AutocompletePrediction item = autoCompleteAdapter.getItem(i);
            if (item != null) {
                final String placeId = item.getPlaceId();

                PendingResult<PlaceBuffer> placeResult = Places.GeoDataApi
                        .getPlaceById(mGoogleApiClient, placeId);
                placeResult.setResultCallback(mUpdatePlaceDetailsCallback);
            }
        }
    };

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
                    GpsAnimUtil.setGpsStatus(iv_gps, GpsAnimUtil.GPS_STATUS_OFFLINE);
                    return;
                }
                //Log.e(TAG, "onLocationResult: callback location found : " + locationResult.getLocations());
                for (Location location : locationResult.getLocations()) {
                    // Update current location data
                    mCurrentLocation = location;
                    GpsAnimUtil.setGpsStatus(iv_gps, GpsAnimUtil.GPS_STATUS_LOCATED);
                    if (!presentLocationTracedOnce) {
                        presentLocationTracedOnce = true;
                        moveToCurrentLocation();
                    }
                }
            }
        };

        initMap();
        initViews();
        GpsAnimUtil.setGpsStatus(iv_gps, GpsAnimUtil.GPS_STATUS_OFFLINE);
        initAutoComplete();

        // finally check required permissions and init map on permissions found
        checkLocationPermissions();
    }

    private void initAutoComplete() {
        mGoogleApiClient = new GoogleApiClient
                .Builder(MainActivity.this)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .enableAutoManage(MainActivity.this, MainActivity.this)
                .build();

        autoCompleteAdapter = new PlaceAutoCompleteAdapter(MainActivity.this, mGoogleApiClient, LAT_LNG_BOUNDS, null);
        actv_addrSearch.setAdapter(autoCompleteAdapter);
        actv_addrSearch.setOnItemClickListener(mAutocompleteClickListener);
    }

    private void initViews() {
        actv_addrSearch = findViewById(R.id.actv_address);
        actv_addrSearch.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH
                        || actionId == EditorInfo.IME_ACTION_DONE
                        || keyEvent.getAction() == KeyEvent.ACTION_DOWN
                        || keyEvent.getAction() == KeyEvent.KEYCODE_ENTER) {
                    hideSoftKeyboard();
                    locateAddr();
                }
                return false;
            }
        });
        hideSoftKeyboard();

        iv_gps = findViewById(R.id.iv_gps_location);
        cv_locate_me = findViewById(R.id.cv_locate_me);
        cv_locate_me.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mCurrentLocation != null) {
                    moveToCurrentLocation();
                } else {
                    checkLocationPermissions();
                }
            }
        });

        tv_addr_add = findViewById(R.id.tv_address_add);
        iv_add_icon = findViewById(R.id.iv_add_icon);
        cv_recent_pin_addr = findViewById(R.id.cv_recent_pin_addr);

        cv_get_route = findViewById(R.id.cv_get_route);
        cv_get_route.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getRoute();
            }
        });

        tv_waypoint_count = findViewById(R.id.tv_waypoint_count);
        String placeholder = "Waypoints: " + markersList.size();
        tv_waypoint_count.setText(placeholder);

        rv_waypoints = findViewById(R.id.rv_waypoints);

        rv_waypoints.setLayoutManager(new LinearLayoutManager(MainActivity.this));

        wayPointAdapter = new WayPointAdapter(markersList, this);
        rv_waypoints.setAdapter(wayPointAdapter);
    }

    private void promptForAddrAdding(Marker marker) {
        if (recentMarker != null) {
            recentMarker.remove();
        }
        recentMarker = marker;
        String placeholder = recentMarker.getTitle() == null ? "Marker #" + markerNumber : recentMarker.getTitle();
        recentMarker.setTitle(placeholder);
        tv_addr_add.setText(placeholder);
        iv_add_icon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addWaypoint(recentMarker);
                cv_recent_pin_addr.setVisibility(View.GONE);
                recentMarker = null;
            }
        });

        cv_recent_pin_addr.setVisibility(View.VISIBLE);
    }

    private void addWaypoint(Marker marker) {
        switch (markersList.size()) {
            case 0:
                marker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
                break;
            case 1:
                marker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ROSE));
                break;
            default:
                marker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ROSE));
                Marker prevDest = markersList.get(markersList.size() - 1);
                prevDest.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
        }

        markersList.add(marker);
        markerNumber++;

        if (markersList.size() >= 2) {
            cv_get_route.setCardBackgroundColor(Color.BLUE);
            cv_get_route.setClickable(true);
        } else {
            cv_get_route.setCardBackgroundColor(Color.DKGRAY);
            cv_get_route.setClickable(false);
        }

        String placeholder = "Waypoints: " + markersList.size();
        tv_waypoint_count.setText(placeholder);
        wayPointAdapter.notifyDataSetChanged();
    }

    private void removeWaypoint(int position) {
        markersList.get(position).remove();
        markersList.remove(position);

        if (markersList.size() != 0) {
            markersList.get(markersList.size() - 1).setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ROSE));
            markersList.get(0).setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
        }

        if (markersList.size() >= 2) {
            cv_get_route.setCardBackgroundColor(Color.BLUE);
            cv_get_route.setClickable(true);
        } else {
            cv_get_route.setCardBackgroundColor(Color.DKGRAY);
            cv_get_route.setClickable(false);
        }

        wayPointAdapter.notifyDataSetChanged();

        String placeholder = "Waypoints: " + markersList.size();
        tv_waypoint_count.setText(placeholder);
    }

    private void locateAddr() {
        String addr = actv_addrSearch.getText().toString();
        if (addr.isEmpty()) return;

        Geocoder geocoder = new Geocoder(MainActivity.this);
        List<Address> addrList = new ArrayList<>();

        try {
            addrList = geocoder.getFromLocationName(addr, 1);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (addrList.size() > 0) {
            Address address = addrList.get(0);
            dropPin(new LatLng(address.getLatitude(), address.getLongitude()), CAMERA_ZOOM_LEVEL, address.getAddressLine(0));
        } else {
            Statics.peekSnackbar(MainActivity.this, "No Address found for: " + addr);
        }
    }

    private void hideSoftKeyboard() {
        // TODO: 19-10-2018
        MainActivity.this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    @SuppressWarnings("SameParameterValue")
    private void dropPin(LatLng latLng, float zoom, String title) {
        moveCamera(latLng, zoom);
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latLng)
                .title(title);
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(DEFAULT_MARKER_HUE));
        promptForAddrAdding(googleMap.addMarker(markerOptions));
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
            googleMap.getUiSettings().setMyLocationButtonEnabled(false);
        }

        initMapClickListener();
    }

    private void checkLocationSettings() {

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
        GpsAnimUtil.setGpsStatus(iv_gps, GpsAnimUtil.GPS_STATUS_SEARCHING);
        Log.d(TAG, "getDeviceLocation: getting current device location");

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            GpsAnimUtil.setGpsStatus(iv_gps, GpsAnimUtil.GPS_STATUS_OFFLINE);
            return;
        }

        Task<Location> locTask = fusedLpClient.getLastLocation();
        locTask.addOnSuccessListener(MainActivity.this, new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                // location should not be null
                if (location != null) {
                    mCurrentLocation = location;
                    GpsAnimUtil.setGpsStatus(iv_gps, GpsAnimUtil.GPS_STATUS_LOCATED);
                    moveToCurrentLocation();
                } else {
                    GpsAnimUtil.setGpsStatus(iv_gps, GpsAnimUtil.GPS_STATUS_OFFLINE);
                }
            }
        }).addOnFailureListener(MainActivity.this, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Statics.showSnackbar(MainActivity.this, "Error getting current location.");
                GpsAnimUtil.setGpsStatus(iv_gps, GpsAnimUtil.GPS_STATUS_OFFLINE);
                e.printStackTrace();
            }
        });
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
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
    }

    private void initMapClickListener() {
        googleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng point) {
                MarkerOptions options = new MarkerOptions();
                options.position(point);
                options.icon(BitmapDescriptorFactory.defaultMarker(DEFAULT_MARKER_HUE));
                Marker marker = googleMap.addMarker(options);
                promptForAddrAdding(marker);
            }
        });
    }

    public void getRoute() {
        if (markersList.size() < 2) {
            return;
        }

        LatLng origin = markersList.get(0).getPosition();
        LatLng dest = markersList.get(markersList.size() - 1).getPosition();
        ArrayList<LatLng> checkpoints = new ArrayList<>();
        for (int i = 1; i < markersList.size() - 1; i++) {
            checkpoints.add(markersList.get(i).getPosition());
        }

        String url = DirectionUrlUtils.getMapsUrl(origin, dest, checkpoints);

        queue = Volley.newRequestQueue(this);

        // prepare the Request
        JsonObjectRequest getRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        // display response
                        Log.e("Response", response.toString());
                        ParserTask parserTask = new ParserTask(new UpdateGoogleMaps() {
                            @Override
                            public void drawPolylines(PolylineOptions polylineOptions) {
                                googleMap.addPolyline(polylineOptions);
                            }
                        });
                        // Invokes the thread for parsing the JSON data
                        parserTask.execute(response.toString());
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d("Error.Response", error.toString());
                    }
                }
        );

        queue.add(getRequest);
    }

    @Override
    public void onWayPointClick(View view, int position) {
        removeWaypoint(position);
    }

    private interface UpdateGoogleMaps {
        void drawPolylines(PolylineOptions polylineOptions);
    }

    private static class ParserTask extends AsyncTask<String, Integer, ParsedData> {

        UpdateGoogleMaps updateGoogleMaps;

        ParserTask(UpdateGoogleMaps updateGoogleMaps) {
            this.updateGoogleMaps = updateGoogleMaps;
        }

        // Parsing the data in non-ui thread
        @Override
        protected ParsedData doInBackground(String... jsonData) {

            JSONObject jObject;
            ParsedData parsedData = null;

            try {
                jObject = new JSONObject(jsonData[0]);
                Log.d("ParserTask", jsonData[0]);
                DataParser parser = new DataParser();
                Log.d("ParserTask", parser.toString());

                // Starts parsing data
                parsedData = parser.parse(jObject);
                Log.d("ParserTask", "Executing routes");

            } catch (Exception e) {
                Log.d("ParserTask", e.toString());
                e.printStackTrace();
            }
            return parsedData;
        }

        // Executes in UI thread, after the parsing process
        @Override
        protected void onPostExecute(ParsedData parsedData) {
            List<List<HashMap<String, String>>> result = parsedData.getRoutes();
            ArrayList<LatLng> points;
            PolylineOptions lineOptions = null;

            // Traversing through all the routes
            assert result != null;
            for (int i = 0; i < result.size(); i++) {
                points = new ArrayList<>();
                lineOptions = new PolylineOptions();

                // Fetching i-th route
                List<HashMap<String, String>> path = result.get(i);

                // Fetching all the points in i-th route
                for (int j = 0; j < path.size(); j++) {
                    HashMap<String, String> point = path.get(j);
                    double lat = Double.parseDouble(Objects.requireNonNull(point.get("lat")));
                    double lng = Double.parseDouble(Objects.requireNonNull(point.get("lng")));
                    LatLng position = new LatLng(lat, lng);

                    points.add(position);
                }

                // Adding all the points in the route to LineOptions
                lineOptions.addAll(points);
                lineOptions.width(10);

                String sDist = parsedData.getDistInMeters();
                if (sDist == null) {
                    lineOptions.color(Color.RED);
                } else if (sDist.length() > 5) {
                    // distance obviously > 20 km
                    lineOptions.color(Color.rgb(102, 51, 153));
                } else {
                    int iDistInKm = Integer.parseInt(sDist) / 1000;
                    if (iDistInKm < 4) {
                        lineOptions.color(Color.BLACK);
                    } else if (iDistInKm < 20) {
                        lineOptions.color(Color.BLUE);
                    } else {
                        lineOptions.color(Color.rgb(102, 51, 153));
                    }
                }
                Log.d("onPostExecute", "onPostExecute lineoptions decoded");
            }

            // Drawing polyline in the Google Map for the i-th route
            if (lineOptions != null) {
                updateGoogleMaps.drawPolylines(lineOptions);
            } else {
                Log.d("onPostExecute", "without Polylines drawn");
            }
        }
    }
}