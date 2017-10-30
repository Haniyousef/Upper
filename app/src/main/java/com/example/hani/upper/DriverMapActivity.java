package com.example.hani.upper;

import android.content.Intent;
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
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DriverMapActivity extends FragmentActivity
        implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener {

    private GoogleMap mMap;
    GoogleApiClient mGoogleApiClient;
    Location lastLocation;
    LocationRequest mLocationRequest;
    Button logout ;
    String customerId="";
    Boolean isLogingout = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_map);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        logout=(Button)findViewById(R.id.logout_btn);
        logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isLogingout=true;
                disconnectDriver();
                FirebaseAuth.getInstance().signOut();
                startActivity(new Intent(DriverMapActivity.this,MainActivity.class));
                finish();
                return;
            }
        });

        // get requests customers id
        getAssignedCustomer();
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
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // check permission
            return;
        }
        buildApiClient();
        mMap.setMyLocationEnabled(true);
    }

    protected synchronized void buildApiClient(){
        mGoogleApiClient=new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    public void onLocationChanged(Location location) {

        lastLocation=location ;
        LatLng latLng=new LatLng(location.getLatitude(),location.getLongitude());
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(11));

        // for save updated location to database
        // to know if driver have pickup requst(Workink) or avaliable
        String userId= FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference referenceAvaliable= FirebaseDatabase.getInstance().getReference("DriversAvaliable");
        DatabaseReference referenceWorking= FirebaseDatabase.getInstance().getReference("DriversWorking");
        GeoFire geoFireAvaliable=new GeoFire(referenceAvaliable);
        GeoFire geoFireWorking=new GeoFire(referenceWorking);
        Toast.makeText(this,customerId+ "", Toast.LENGTH_SHORT).show();

        switch (customerId){
            case "":
                Toast.makeText(this, "aval", Toast.LENGTH_SHORT).show();
                geoFireWorking.removeLocation(userId);
                geoFireAvaliable.setLocation(userId,new GeoLocation(location.getLatitude(),location.getLongitude()));
                break;
            default:
                Toast.makeText(this, "work", Toast.LENGTH_SHORT).show();
                geoFireAvaliable.removeLocation(userId);
                geoFireWorking.setLocation(userId,new GeoLocation(location.getLatitude(),location.getLongitude()));
                break;
        }


    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);            // time between  location update is 1 second
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
           // check permission
            return;
        }
        // update the location i called every 1 second (1000)
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest,  this);
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



    // get requests customers id
    private void getAssignedCustomer(){
        String driverId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference assignedCustomerRef = FirebaseDatabase.getInstance()
                .getReference().child("Users").child("Drivers").child(driverId).child("CustomerRideId");
        assignedCustomerRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    customerId = dataSnapshot.getValue().toString();
                    getAssignedCustomerPickupLocation();
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }


    // get Assigned Customer Location
   private void getAssignedCustomerPickupLocation(){
       DatabaseReference assignedCustomerPickupRef=FirebaseDatabase.getInstance()
               .getReference().child("CustomerRequests").child(customerId).child("l");

       assignedCustomerPickupRef.addValueEventListener(new ValueEventListener() {
           @Override
           public void onDataChange(DataSnapshot dataSnapshot) {
               if (dataSnapshot.exists()){
                   double locationLat=0;
                   double locationLon=0;
                   List<Object> map= (List<Object>) dataSnapshot.getValue();
                   if (map.get(0)!=null){
                       locationLat=Double.parseDouble(map.get(0).toString());
                   }
                   if (map.get(1) !=null){
                       locationLon=Double.parseDouble(map.get(1).toString());
                   }
                   LatLng latLng=new LatLng(locationLat,locationLon);

                   // add marker to customer pickup
                   mMap.addMarker(new MarkerOptions().position(latLng).title("Pickup Location"));
               }
           }

           @Override
           public void onCancelled(DatabaseError databaseError) {

           }
       });
   }


   private void disconnectDriver(){
       LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient,this);
       String userId= FirebaseAuth.getInstance().getCurrentUser().getUid();
       DatabaseReference reference= FirebaseDatabase.getInstance().getReference("DriversAvaliable");
       GeoFire geoFire=new GeoFire(reference);
       geoFire.removeLocation(userId);    // remove data base of current driver
   }

    @Override
    protected void onStop() {
        super.onStop();
        if (!isLogingout){
            disconnectDriver();
        }
        // make driver un avaliable whren he leave this activity


    }



}
