package com.directions.benesposito.directions;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Cache;
import com.android.volley.Network;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

import static android.Manifest.permission_group.LOCATION;
import static android.R.attr.key;
import static android.provider.Contacts.SettingsColumns.KEY;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener
{
    private static final String TAG = MapsActivity.class.getSimpleName();

    private GoogleMap mMap;
    private PlaceAutocompleteFragment origin;
    private PlaceAutocompleteFragment destination;

    private LatLng originLatLng;
    private LatLng destinationLatLng;

    private GoogleApiClient mGoogleApiClient;

    private boolean mLocationPermissionGranted;

    private float mCurrentZoom;

    private static final String KEY_CAMERA_POSITION = "camera_position";
    private static final String KEY_LAST_LOCATION = "location";
    private static final String KEY_CURRENT_ZOOM = "current_zoom";

    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private static final int DEFAULT_ZOOM = 15;

    private final LatLng DEFAULT_LATLNG = new LatLng(39.952339,-75.1634177);
    private final Location DEFAULT_LOCATION = new Location("Default Location");

    RequestQueue mRequestQueue;

    private BluetoothAdapter bluetooth;

    //Testing
    private TextView printText;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        mCurrentZoom = DEFAULT_ZOOM;
        DEFAULT_LOCATION.setLatitude(DEFAULT_LATLNG.latitude);
        DEFAULT_LOCATION.setLongitude(DEFAULT_LATLNG.longitude);

        setContentView(R.layout.activity_maps);

        printText = (TextView) findViewById(R.id.debugText);

        Cache cache = new DiskBasedCache(getCacheDir(), 1024 * 1024);
        Network network = new BasicNetwork(new HurlStack());
        mRequestQueue = new RequestQueue(cache, network);
        mRequestQueue.start();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this /* FragmentActivity */,
                        this /* OnConnectionFailedListener */)
                .addConnectionCallbacks(this)
                .addApi(LocationServices.API)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .build();
        mGoogleApiClient.connect();

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        if(mMap != null)
        {
            outState.putParcelable(KEY_LAST_LOCATION, getDeviceLocation());
            outState.putParcelable(KEY_CAMERA_POSITION, mMap.getCameraPosition());
            outState.putFloat(KEY_CURRENT_ZOOM, mCurrentZoom);
        }
        System.out.println("ON SAVE INSTANCE STATE CALLED!" + outState);

        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState)
    {
        if(savedInstanceState != null)
        {
            mCurrentZoom = savedInstanceState.getFloat(KEY_CURRENT_ZOOM);
        }
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        // Build the map.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult result)
    {
        Log.d(TAG, "Play services connection failed: ConnectionResult.getErrorCode() = "
                + result.getErrorCode());
    }

    @Override
    public void onConnectionSuspended(int cause)
    {
        Log.d(TAG, "Play services connection suspended");
    }

    protected void findCurrentLocation(View v)
    {
        origin.setText("Current Location");
        originLatLng = new LatLng(getDeviceLocation().getLatitude(), getDeviceLocation().getLongitude());
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(getDeviceLocation().getLatitude(), getDeviceLocation().getLongitude()), mCurrentZoom));
    }

    protected Location getDeviceLocation()
    {
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
            mLocationPermissionGranted = true;
        else
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);

        Location currentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

        if(currentLocation == null)
            return DEFAULT_LOCATION;
        else
            return LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
    }

    protected void setMap()
    {
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(getDeviceLocation().getLatitude(), getDeviceLocation().getLongitude()), DEFAULT_ZOOM));
    }

    protected void onDirections(View v)
    {
        System.out.println("ONDIRECTIONS CALLED");

        if(originLatLng == null)
        {
            origin.setText("Current Location");
            originLatLng = new LatLng(getDeviceLocation().getLatitude(), getDeviceLocation().getLongitude());
        }
        else if(destinationLatLng == null)
            System.out.println("PLEASE ENTER DESTINATION ADDRESS!!");
        else
            getDirections(originLatLng, destinationLatLng);
    }

    protected void getDirections(LatLng origin, LatLng destination)
    {
        System.out.println("GET DIRECTIONS CALLED: " + origin);
        RequestQueue queue = Volley.newRequestQueue(this);
        String url = "https://maps.googleapis.com/maps/api/directions/json?origin=" + originLatLng.latitude + "," + originLatLng.longitude + "&destination=" + destinationLatLng.latitude + "," + destinationLatLng.longitude + "&key=AIzaSyDECJ0OTFFTpl5wb3r6dhSgXLIc8uy0Ru8\n";

        System.out.println(url);

        JsonObjectRequest jsObjRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>()
                {
                    @Override
                    public void onResponse(JSONObject response)
                    {
                        System.out.println("ONRESPONSE CALLED" + response.toString());
                        /*try
                        {
                            System.out.println("second attempt at place_id");


                        }
                        catch (JSONException e)
                        {
                            e.printStackTrace();
                        }*/
                    }
                }, new Response.ErrorListener()
                {
                    @Override
                    public void onErrorResponse(VolleyError error)
                    {
                        error.printStackTrace();
                    }
                });

                mRequestQueue.add(jsObjRequest);

    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(final GoogleMap googleMap)
    {
        mMap = googleMap;

        if (ContextCompat.checkSelfPermission(this.getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
            mMap.setMyLocationEnabled(true);

        setMap();

        origin = (PlaceAutocompleteFragment) getFragmentManager().findFragmentById(R.id.origin);
        origin.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(place.getLatLng(), 18));
                originLatLng = place.getLatLng();
            }

            @Override
            public void onError(Status status) {

            }
        });

        destination = (PlaceAutocompleteFragment) getFragmentManager().findFragmentById(R.id.destination);
        destination.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(place.getLatLng(), 18));
                destinationLatLng = place.getLatLng();
            }

            @Override
            public void onError(Status status) {

            }
        });

        mMap.setOnCameraMoveListener(new GoogleMap.OnCameraMoveListener()
        {
            @Override
            public void onCameraMove()
            {
                mCurrentZoom = googleMap.getCameraPosition().zoom;
            }
        });
    }
}
