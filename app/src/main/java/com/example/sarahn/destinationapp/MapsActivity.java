package com.example.sarahn.destinationapp;

import android.animation.ObjectAnimator;
import android.graphics.Color;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Looper;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener{

    private GoogleMap mMap;
    private static double destlatitude;
    private static double destlongitude;
    DatabaseReference data;
    private FusedLocationProviderClient mFusedLocationClient;
    private GoogleApiClient mGoogleApiClient;
    CameraPosition CurrentLocation;
    private double sourceLat;
    private double sourceLng;
    private final String KEY = "AIzaSyCUtKJsalxBuAds5kpf1byHv2-JKIQO0qs";
    private List<LatLng> polyline = new ArrayList<LatLng>();
    PolylineOptions lineOptions;
    private boolean ispolylineDrawn = false;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        lineOptions = new PolylineOptions();
        setupFirebase();
        setUpMapIfNeeded();
    }

    private void setupFirebase(){
        data = FirebaseDatabase.getInstance()
                .getReference("latlng");

        data.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {


                destlatitude = Double.parseDouble(dataSnapshot.child("lat").getValue().toString());
                destlongitude = Double.parseDouble(dataSnapshot.child("lng").getValue().toString());

                Log.i("mainactivity ", "destination latlng in on datasnapshot " + dataSnapshot.child("lat").getValue().toString() + " " +
                        destlongitude);

                buildClient();
                setupDirectionAPI();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        //setUpMapIfNeeded();
    }

    private void setUpMapIfNeeded() {

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        SupportMapFragment mapFragment = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map));
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMyLocationEnabled(true);
        mMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
        getCurrentLocation();
    }

    private void getCurrentLocation() {

        mFusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(final Location location) {

                        Log.i("mainactivity ","onsuccess get current location ");
                        if (location != null) {

                            CurrentLocation = new CameraPosition.Builder().target(new LatLng(location.getLatitude(),
                                    location.getLongitude()))
                                    .zoom(15.5f)
                                    .bearing(0)
                                    .tilt(25)
                                    .build();

                            changeCameraPosition(CameraUpdateFactory.newCameraPosition(CurrentLocation)
                                    , new GoogleMap.CancelableCallback() {
                                @Override
                                public void onFinish() {
                                    mMap.addMarker(new MarkerOptions().position(new LatLng(location.getLatitude(),
                                            location.getLongitude())).title("Marker"));
                                }

                                @Override
                                public void onCancel() {

                                }
                            });

                        }
                    }
                });
    }

    private void changeCameraPosition(CameraUpdate update, GoogleMap.CancelableCallback callback){

        mMap.animateCamera(update, 5, callback);
    }


    private void buildClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnected(Bundle bundle) {

        mFusedLocationClient.requestLocationUpdates(requestLocation(), new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                for (final Location location : locationResult.getLocations()) {

                    sourceLat = location.getLatitude();
                    sourceLng = location.getLongitude();

                    setupDirectionAPI();

                    changeCameraPosition(CameraUpdateFactory.newLatLngZoom(
                            new LatLng(location.getLatitude(), location.getLongitude()),
                            16), new GoogleMap.CancelableCallback() {


                        @Override
                        public void onFinish() {

                            mMap.addMarker(new MarkerOptions().position(new LatLng(location.getLatitude(),
                                    location.getLongitude())).title("Marker"));

                        }

                        @Override
                        public void onCancel() {

                        }
                    });

                }
            }
        }, Looper.myLooper());
    }

    private LocationRequest requestLocation() {

        LocationRequest mLocationRequest = new LocationRequest();

        mLocationRequest.setSmallestDisplacement(3.81f);
//        mLocationRequest.setInterval(120000);
//        mLocationRequest.setFastestInterval(120000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        return mLocationRequest;
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    private void setupDirectionAPI(){
        DownloadTask downloadTask = new DownloadTask();
        downloadTask.execute(createURL());
    }

    private String createURL(){
        Log.i("mainactivity " , "destination latlng create url" + destlatitude + " " +
                destlongitude );
       return "https://maps.googleapis.com/maps/api/directions/json?origin="+ sourceLat + "," +sourceLng + "&destination="+destlatitude
        +"," + destlongitude +"&key=" + KEY ;
    }

    private class DownloadTask extends AsyncTask<String, Void, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            ParserTask parserTask = new ParserTask();
            parserTask.execute(s);
        }

        @Override
        protected String doInBackground(String... params) {
            String stringUrl = params[0];

            Log.i("mainactivity ", "do in backgroung stringurl " + stringUrl);

            String data = "";
            InputStream iStream;
            BufferedReader br;
            HttpURLConnection urlConnection;

            try {

                URL url = new URL(stringUrl);
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.connect();
                iStream = urlConnection.getInputStream();
                br = new BufferedReader(new InputStreamReader(iStream));

                StringBuffer sb = new StringBuffer();

                String line = "";
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }
                data = sb.toString();

                Log.d("mainactivity ","downloadUrl "+ data.toString());
                br.close();

            } catch (Exception e) {

            }

            return data;

        }
    }


    private class ParserTask extends AsyncTask<String, Integer, List<List<HashMap<String, String>>>> {


        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... jsonData) {

            Log.i("mainactivity ", "second phase parser task do in backgrnd");

            JSONObject jObject;
            List<List<HashMap<String, String>>> routes = null;


            try {
                jObject = new JSONObject(jsonData[0]);
                Log.d("mainactivity ", "ParserTask" + jsonData[0].toString());
                DataParser parser = new DataParser();

                // Starts parsing data
                routes = parser.parse(jObject);

                Log.d("mainactivity ", "ParserTask" + routes.toString());

            } catch (Exception e) {
                e.printStackTrace();
            }
            return routes;
        }

        // Executes in UI thread, after the parsing process
        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> result) {

            Log.i("mainactivity ","postexecute ");
            ArrayList<LatLng> points;


            for (int i = 0; i < result.size(); i++) {



                List<HashMap<String, String>> path = result.get(i);

                Log.i("mainactivity ","postexecute result.get " + path);
                // Fetching all the points in i-th route
                for (int j = 0; j < path.size(); j++) {
                    HashMap<String, String> point = path.get(j);

                    double lat = Double.parseDouble(point.get("lat"));
                    double lng = Double.parseDouble(point.get("lng"));
                    LatLng position = new LatLng(lat, lng);


                    polyline.add(position);


                }

                lineOptions.addAll(polyline);
                lineOptions.width(10);
                lineOptions.color(Color.RED);

                Log.d("onPostExecute", "onPostExecute lineoptions decoded");

            }

            // Drawing polyline in the Google Map for the i-th route
            if (lineOptions != null) {
                mMap.clear();
                mMap.addPolyline(lineOptions);
            } else {
                Log.d("onPostExecute", "without Polylines drawn");
            }
        }
    }
}
