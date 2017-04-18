package com.psx.pranav.mapsdemo;

import android.*;
import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.identity.intents.Address;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlacePicker;
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

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, LocationListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private GoogleMap mMap;
    private static final int REQUEST_PLACE_PICKER = 1;
    private static final int PLACE_PICKER_REQUEST = 1;
    private GoogleApiClient apiClient;
    private Context context;
    private LocationManager locationManager;
    private Location lastLocation;
    private boolean granted = false;
    private AlertDialog alertDialog_reason;
    private FloatingActionButton floatingActionButton,floatingActionButton2;
    private static final int permission = 0;
    private View mView;
    private LatLng origin, destination;
    private String distance = "", duration = "", addressInText = "";
    private MarkerOptions markerOptions1;
    private MarkerOptions markerOptions2;
    private static final LatLngBounds BOUNDS_MOUNTAIN_VIEW = new LatLngBounds(
            new LatLng(37.398160, -122.180831), new LatLng(37.430610, -121.972090));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        context = this;
        mView = findViewById(R.id.main_container);
        floatingActionButton = (FloatingActionButton) findViewById(R.id.floatingActionButton);
        if (apiClient == null) {
            apiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
        // check for the permisiions here
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            granted = true;
        } else {
            if (ActivityCompat.shouldShowRequestPermissionRationale((Activity) context, Manifest.permission.ACCESS_COARSE_LOCATION) || ActivityCompat.shouldShowRequestPermissionRationale((Activity) context, Manifest.permission.ACCESS_FINE_LOCATION)) {
                // show the  rationale behind asking the permissions
                alertDialog_reason = new AlertDialog.Builder(this).setTitle("Location Permission Required")
                        .setMessage("This app requires location services to run. Please allow the location services to continue.")
                        .setPositiveButton("Open Settings", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //  open the application settings page
                                final Intent intent = new Intent();
                                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                intent.addCategory(Intent.CATEGORY_DEFAULT);
                                intent.setData(Uri.parse("package:" + context.getPackageName()));
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                                intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                                context.startActivity(intent);
                                alertDialog_reason.dismiss();
                            }
                        }).setNegativeButton("Exit App", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                                finish();
                            }
                        }).show();
                alertDialog_reason.setCancelable(false);
                alertDialog_reason.setCanceledOnTouchOutside(false);
            } else {
                // no need to show the rationale
                ActivityCompat.requestPermissions((Activity) context, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, permission);
            }
        }

        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (lastLocation != null) {
                    Snackbar.make(mView, "Requesting Cab", Snackbar.LENGTH_SHORT).show();
                    LatLng tmp = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(tmp));
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(tmp, 16));
                    String url = getDirectionsUrl(origin, destination);
                    DownloadTask downloadTask = new DownloadTask();
                    Log.d("MAPS", "staring async task");
                    downloadTask.execute(url);
                    LatLng lng = new LatLng(lastLocation.getLatitude() + 0.0024, lastLocation.getLongitude() + 0.0026);
                    //displayUpdatedInfo();
                } else {
                    Snackbar.make(mView, "Getting Your Position", Snackbar.LENGTH_SHORT).show();
                }
            }
        });
        floatingActionButton2 = (FloatingActionButton) findViewById(R.id.floatingActionButton_2);
        floatingActionButton2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
               Toast.makeText(context,"Clicked",Toast.LENGTH_SHORT).show();
                try {
                    PlacePicker.IntentBuilder intentBuilder = new PlacePicker.IntentBuilder();
                    Intent intent = intentBuilder.build((Activity)context);
                    // Start the Intent by requesting a result, identified by a request code.
                    startActivityForResult(intent, REQUEST_PLACE_PICKER);

                    // Hide the pick option in the UI to prevent users from starting the picker
                    // multiple times.

                } catch (GooglePlayServicesRepairableException e) {
                    GooglePlayServicesUtil
                            .getErrorDialog(e.getConnectionStatusCode(), (Activity)context, 0);
                } catch (GooglePlayServicesNotAvailableException e) {
                    Toast.makeText(context, "Google Play Services is not available.",
                            Toast.LENGTH_LONG)
                            .show();
                }

                // END_INCLUDE(intent)
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // BEGIN_INCLUDE(activity_result)
        if (requestCode == REQUEST_PLACE_PICKER) {
            // This result is from the PlacePicker dialog.



            if (resultCode == Activity.RESULT_OK) {
                /* User has picked a place, extract data.
                   Data is extracted from the returned intent by retrieving a Place object from
                   the PlacePicker.
                 */
                final Place place = PlacePicker.getPlace(data, (Activity)context);

                /* A Place object contains details about that place, such as its name, address
                and phone number. Extract the name, address, phone number, place ID and place types.
                 */
                final CharSequence name = place.getName();
                final CharSequence address = place.getAddress();
                final CharSequence phone = place.getPhoneNumber();
                final String placeId = place.getId();
                String attribution = PlacePicker.getAttributions(data);
                if (attribution == null) {
                    attribution = "";
                }

                // Print data to debug log
                Log.d("TAG", "Place selected: " + placeId + " (" + name.toString() + ")");

                // Show the card.
                //getCardStream().showCard(CARD_DETAIL);

            } else {
                // User has not selected a place, hide the card.
            //    getCardStream().hideCard(CARD_DETAIL);
            }

        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
/*
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == REQUEST_PLACE_PICKER){

        }

        if (requestCode == PLACE_PICKER_REQUEST && resultCode == RESULT_OK){
            final Place place = PlacePicker.getPlace(this,data);
            final CharSequence name = place.getName();
            final CharSequence address = place.getAddress();
            String attributions  = (String) place.getAttributions();
            if (attributions == null){
                attributions = "";
            }

        }
        else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }*/

    // check the result for permisiion request


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case permission: {
                // check for the cancelled results
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    // both the fine and coarse location permissions were granted
                    granted = true;
                } else {
                    granted = false;
                }
                break;
            }
        }
        return;
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
        // Add a marker in Sydney and move the camera
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        LatLng sydney = new LatLng(-34, 151);
        //mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        //mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.

            Log.d("MAPS", "here");
            return;
        } else if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(true);
            mMap.getUiSettings().setRotateGesturesEnabled(false);
            mMap.getUiSettings().setZoomControlsEnabled(true);
            mMap.getUiSettings().setZoomGesturesEnabled(true);
            mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
                @Override
                public void onMapClick(LatLng latLng) {
                    // funcion to handle clicks on map
                    // required- maybe for future use
                }
            });
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        double lat = (float) location.getLatitude();
        double lon = location.getLongitude();
        Log.d("MAPS", "Location Changed");
        LatLng curr_pos = new LatLng(lat, lon);
        mMap.clear();
        mMap.addMarker(new MarkerOptions().position(curr_pos).title("Your Current Position"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(curr_pos));
        String url = getDirectionsUrl(origin, destination);
        DownloadTask downloadTask = new DownloadTask();
        Log.d("MAPS", "staring async task");
        downloadTask.execute(url);
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }

    GoogleMap.OnMyLocationButtonClickListener onMyLocationButtonClickListener = new GoogleMap.OnMyLocationButtonClickListener() {
        @Override
        public boolean onMyLocationButtonClick() {
            // clear marker from sydney
            // clear the marker from sydney
            //made by pranav sharma
            return true;
        }
    };

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        //noinspection MissingPermission
        markerOptions1 = new MarkerOptions();
        markerOptions2 = new MarkerOptions();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        lastLocation = LocationServices.FusedLocationApi.getLastLocation(apiClient);
        if (lastLocation != null){
            // create a marker n the google maps
            mMap.clear();
            LatLng latLng = new LatLng(lastLocation.getLatitude(),lastLocation.getLongitude());
            markerOptions1.position(latLng).title("Your Current Position").snippet("Getting Location Address");
            mMap.addMarker(markerOptions1);
            mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
            destination = latLng;
            // add a random marker near curr_location
            //mMap.addMarker(new MarkerOptions().position())
            LatLng lng= new LatLng(lastLocation.getLatitude()+0.0024,lastLocation.getLongitude()+0.0026);
            markerOptions2.position(lng).title("Your Cab Position").snippet("Getting Address:Available Cab").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
            mMap.addMarker(markerOptions2);
            //new ReverseGeocode(getBaseContext(),markerOptions2).execute(lng);
            mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
            origin = lng;
            mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
                @Override
                public boolean onMarkerClick(Marker marker) {
                    LatLng lng1 = new LatLng(marker.getPosition().latitude,marker.getPosition().longitude);
                    new ReverseGeocode(getBaseContext()).execute(lng1);
                    marker.setSnippet(addressInText);
                    marker.showInfoWindow();
                    return true;
                }
            });
        }
    }

    private void addMarkerAgain (MarkerOptions markerOptions, String Snippet){
        MarkerOptions options = new MarkerOptions();
        options.position(markerOptions.getPosition()).title(markerOptions.getTitle()).snippet(Snippet).icon(markerOptions.getIcon());
        mMap.addMarker(options);
        Toast.makeText(context,"Updated Markers",Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    protected void onStart() {
        apiClient.connect();
        super.onStart();
    }

    @Override
    protected void onStop() {
        apiClient.disconnect();
        super.onStop();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    private String getDirectionsUrl (LatLng origin, LatLng destination) {
        // Origin of route
        String str_origin = "origin="+origin.latitude+","+origin.longitude;

        // Destination of route
        String str_dest = "destination="+destination.latitude+","+destination.longitude;

        // Sensor enabled
        String sensor = "sensor=false";

        // Building the parameters to the web service
        String parameters = str_origin+"&"+str_dest+"&"+sensor;

        // Output format
        String output = "json";

        // Building the url to the web service
        String url = "https://maps.googleapis.com/maps/api/directions/"+output+"?"+parameters;

        return url;
    }

    private String downloadUrl(String strUrl) throws IOException {
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        try{
            URL url = new URL(strUrl);

            // Creating an http connection to communicate with url
            urlConnection = (HttpURLConnection) url.openConnection();

            // Connecting to url
            urlConnection.connect();

            // Reading data from url
            iStream = urlConnection.getInputStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));

            StringBuffer sb = new StringBuffer();

            String line = "";
            while( ( line = br.readLine()) != null){
                sb.append(line);
            }

            data = sb.toString();

            br.close();

        }catch(Exception e){
            Log.d("E", e.toString());
        }finally{
            iStream.close();
            urlConnection.disconnect();
        }
        return data;
    }

    private class DownloadTask extends AsyncTask<String, Void, String> {

        Snackbar snackbar = Snackbar.make(mView,"Calculating Route",Snackbar.LENGTH_INDEFINITE);
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // display a snackbar before executing
           // Toast.makeText(context,"Calculating",Toast.LENGTH_LONG).show();
             snackbar.show();
        }

        // Downloading data in non-ui thread
        @Override
        protected String doInBackground(String... url) {

            // For storing data from web service
            String data = "";

            try{
                // Fetching the data from web service
                data = downloadUrl(url[0]);
            }catch(Exception e){
                Log.d("Background Task",e.toString());
            }
            return data;
        }

        // Executes in UI thread, after the execution of
        // doInBackground()
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            snackbar.dismiss();
            ParserTask parserTask = new ParserTask();

            // Invokes the thread for parsing the JSON data
            parserTask.execute(result);
        }
    }

    private class ParserTask extends AsyncTask<String, Integer, List<List<HashMap<String,String>>> >{

        // Parsing the data in non-ui thread
        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... jsonData) {

            JSONObject jObject;
            List<List<HashMap<String, String>>> routes = null;

            try{
                jObject = new JSONObject(jsonData[0]);
                DirectionsParser parser = new DirectionsParser();
                // Starts parsing data
                routes = parser.parse(jObject);
            }catch(Exception e){
                e.printStackTrace();
            }
            return routes;
        }

        // Executes in UI thread, after the parsing process
        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> result) {
            ArrayList<LatLng> points = null;
            PolylineOptions lineOptions = null;
            MarkerOptions markerOptions = new MarkerOptions();
            distance = "";
            duration  = "";

            // Traversing through all the routes
            for(int i=0;i<result.size();i++){
                points = new ArrayList<LatLng>();
                lineOptions = new PolylineOptions();

                //  i-th route
                List<HashMap<String, String>> path = result.get(i);

                //  all the points in i-th route
                for(int j=0;j<path.size();j++){
                    HashMap<String,String> point = path.get(j);
                    if (j == 0){
                        distance = point.get("distance");
                        continue;
                    }
                    else if (j == 1){
                        duration  = point.get("duration");
                        continue;
                    }
                    double lat = Double.parseDouble(point.get("lat"));
                    double lng = Double.parseDouble(point.get("lng"));
                    LatLng position = new LatLng(lat, lng);

                    points.add(position);
                }

                // Adding all the points in the route to LineOptions
                lineOptions.addAll(points);
                lineOptions.width(2);
                lineOptions.color(Color.RED);
            }

            // Drawing polyline in the Google Map for the i-th route
            mMap.addPolyline(lineOptions);
            displayUpdatedInfo();
        }


    }

    private  class ReverseGeocode extends AsyncTask <LatLng, Void, String> {

        Context context;
        public ReverseGeocode (Context context){
            this.context = context;
        }

        @Override
        protected String doInBackground(LatLng... latLngs) {
            Geocoder geocoder = new Geocoder(context);
            double latitude = latLngs[0].latitude;
            double longitude = latLngs[0].longitude;
            Log.d("LOCATION EXECUTE","preExecute");

            List<android.location.Address> addresses = null;
            addressInText = "";
            try {
                addresses = geocoder.getFromLocation(latitude,longitude,1);
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (addresses != null && addresses.size() >0){
                android.location.Address address = addresses.get(0);
                addressInText = String.format("%s, %s, %s",address.getMaxAddressLineIndex() > 0 ? address.getAddressLine(0) : "",address.getLocality(),address.getCountryName());
            }
            return addressInText;
        }

        @Override
        protected void onPostExecute(String s) {
            Log.d("LOCATION ADDRESS"," "+addressInText);
            Log.d("LOCATION ADDRESS","POST execute");
            super.onPostExecute(s);
        }
    }

    private void displayUpdatedInfo (){
        // display the distance and duration in a Snackbar
        final Snackbar snackbar = Snackbar.make(mView,"DISTANCE :"+distance+" ETA : "+duration,Snackbar.LENGTH_INDEFINITE);
        snackbar.setAction("DISMISS", new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                snackbar.dismiss();
            }
        });
        View view = snackbar.getView();
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) view.getLayoutParams();
        params.gravity = Gravity.TOP;
        view.setLayoutParams(params);
        snackbar.show();
    }


}// end of class
