package com.example.hani.upper;

import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.List;

public class CustomerMapActivity extends FragmentActivity
        implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener {

    private GoogleMap mMap;
    Location lastLocation;
    LocationRequest mLocationRequest;
    GoogleApiClient mGoogleApiClient;
    Button logout_btn , request_btn ;
    LatLng pickupLocation ;
    private int radius=1 ;
    private boolean driverFounded=false ;
    private String driverFoundedId;
    private boolean requestBool =false;
    private GeoQuery geoQuery ;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_map);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        logout_btn=findViewById(R.id.logout_btn);
        request_btn=findViewById(R.id.request_btn);
        request_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                    // for make requests
                    // save customer location to database
                    String userId=FirebaseAuth.getInstance().getCurrentUser().getUid();
                    DatabaseReference reference=FirebaseDatabase.getInstance().getReference("CustomerRequests");
                    GeoFire geoFire=new GeoFire(reference);
                    geoFire.setLocation(userId,new GeoLocation(lastLocation.getLatitude(),lastLocation.getLongitude()));

                    // put marker on taxi which i call to know where it now
                    pickupLocation=new LatLng(lastLocation.getLatitude(),lastLocation.getLongitude());
                    mMap.addMarker(new MarkerOptions().position(pickupLocation).title("pickup here"));
                    request_btn.setText("Getting Your Driver.....");
                    getClossestDriver();



            }
        });
    }

    private void getClossestDriver(){
        DatabaseReference reference=FirebaseDatabase.getInstance().getReference().child("DriversAvaliable");
        GeoFire geoFire=new GeoFire(reference);

        // to get the closest driver around our circle
        geoQuery=geoFire.queryAtLocation(new GeoLocation(pickupLocation.latitude,pickupLocation.longitude),radius);
        geoQuery.removeAllListeners();

        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {

                // if found driver in this raduis make it true and get his id
                Toast.makeText(CustomerMapActivity.this, !driverFounded+"", Toast.LENGTH_SHORT).show();
               if (!driverFounded){
                   driverFounded=true;
                   driverFoundedId=key;

                   // Notic the driver of pickup request
                   DatabaseReference mRef=FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverFoundedId);
                   String customerId=FirebaseAuth.getInstance().getCurrentUser().getUid();
                   HashMap map=new HashMap();
                   map.put("CustomerRideId",customerId);
                   mRef.updateChildren(map);

                   // show driver location of customer map
                   getDriverLocation();
                   request_btn.setText("Looking for Driver Location......");
               }
            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {

                // if no driver in one kilometer radius increse raduis to 2 kilometers and call function again and so on
                if (!driverFounded)
                {
                    radius++ ;
                    getClossestDriver();
                }

            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });

    }

    // notic the driver location on customer map
    private Marker driverMarker ;
    private void getDriverLocation(){
        Toast.makeText(this,"driver location", Toast.LENGTH_SHORT).show();
        Toast.makeText(CustomerMapActivity.this,driverFoundedId+ "", Toast.LENGTH_SHORT).show();
        DatabaseReference mRef=FirebaseDatabase.getInstance().getReference()
                .child("DriversWorking").child(driverFoundedId).child("l");
        mRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    List<Object> map = (List<Object>) dataSnapshot.getValue();

                    double locationLat = 0;
                    double locationLon = 0;
                    request_btn.setText("Driver found");

                    Toast.makeText(CustomerMapActivity.this,map.get(0)+ "", Toast.LENGTH_SHORT).show();
                    Toast.makeText(CustomerMapActivity.this,map.get(1)+ "", Toast.LENGTH_SHORT).show();
                    if (map.get(0)!=null){
                        locationLat=Double.parseDouble(map.get(0).toString());
                    }
                    if (map.get(1)!=null){
                        locationLon=Double.parseDouble(map.get(1).toString());
                    }

                    // add marker to driver postion on map
                    LatLng driverLatLng=new LatLng(locationLat,locationLon);
                    if (driverMarker!=null){
                        driverMarker.remove();
                    }

                    //start of distance between driver location and pickup location

                    // get the lat and lon of pickup location
                    Location loc1=new Location("");
                    loc1.setLatitude(pickupLocation.latitude);
                    loc1.setLongitude(pickupLocation.longitude);

                    //get the lat and lon of driver location
                    Location loc2=new Location("");
                    loc2.setLatitude(driverLatLng.latitude);
                    loc2.setLongitude(driverLatLng.longitude);

                    // distance between them
                    float distance=loc1.distanceTo(loc2);

                    // notic when arrive
                    if (distance<100){
                        request_btn.setText("Driver Here : "+String.valueOf(distance));
                    }else{
                        request_btn.setText("Driver found : "+String.valueOf(distance));
                    }


                    // end of distance between driver location and pickup location
                    driverMarker=mMap.addMarker(new MarkerOptions().position(driverLatLng).title("DriverMarker"));

                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
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
        buildApiClient();
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mMap.setMyLocationEnabled(true);

    }

    protected synchronized void buildApiClient() {
        mGoogleApiClient=new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();

    }

    @Override
    public void onLocationChanged(Location location) {
        lastLocation=location;
        LatLng latLng=new LatLng(location.getLatitude(),location.getLongitude());
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(11));


    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }
}
