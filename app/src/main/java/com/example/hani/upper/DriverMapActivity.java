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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.directions.route.AbstractRouting;
import com.directions.route.Route;
import com.directions.route.RouteException;
import com.directions.route.Routing;
import com.directions.route.RoutingListener;
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
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DriverMapActivity extends FragmentActivity
        implements RoutingListener, OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener {

    private GoogleMap mMap;
    GoogleApiClient mGoogleApiClient;
    Location lastLocation;
    LocationRequest mLocationRequest;
    Button logout , settings_btn , rideStatus ;
    String customerId="";
    Boolean isLogingout = false;

    LinearLayout customerInfo ;
    ImageView customerProfile ;
    TextView customerName , customerPhone , customerDestination;
    SupportMapFragment mapFragment ;

    // for draw rout (line) between to points
    private List<Polyline> polylines;
    private static final int[] COLORS = new int[]{R.color.primary_dark_material_light};

    private int status=0 ;
    private String destination ;
    private LatLng destinationLatLon;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_map);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(DriverMapActivity.this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},LOCATION_REQUEST_CODE);
        }else {
            mapFragment.getMapAsync(this);
        }

        logout=(Button)findViewById(R.id.logout_btn);
        rideStatus=(Button)findViewById(R.id.rideStatus);
        settings_btn=(Button)findViewById(R.id.settings_btn);
        customerInfo=(LinearLayout) findViewById(R.id.customerInfo);
        customerProfile=(ImageView) findViewById(R.id.customerProfile);
        customerName=(TextView) findViewById(R.id.customerName);
        customerPhone=(TextView)findViewById(R.id.customerPhone);
        customerDestination=(TextView)findViewById(R.id.customerDestination);

        rideStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switch (status){
                    case 1 :
                        status=2;
                        if (destinationLatLon.latitude !=0.0  &&  destinationLatLon.longitude !=0.0){
                            getRoutToMarker(destinationLatLon);
                        }
                        rideStatus.setText("drive completed");
                        break;
                    case 2 :
                        endRide();
                        break;
                }
            }
        });
        settings_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(DriverMapActivity.this,DriverSettingsActivity.class));
            }
        });

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

        polylines = new ArrayList<>();
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
      //  Toast.makeText(this,customerId+ "", Toast.LENGTH_SHORT).show();

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
            ActivityCompat.requestPermissions(DriverMapActivity.this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},LOCATION_REQUEST_CODE);
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

    // check the premissions of map

    final int LOCATION_REQUEST_CODE=1;
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case LOCATION_REQUEST_CODE :
            {
                if (grantResults.length>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED){
                    mapFragment.getMapAsync(this);
                }else {
                    Toast.makeText(this, "please provide the permissions", Toast.LENGTH_SHORT).show();
                }
                break;
            }
        }

    }

    // get requests customers id
    private void getAssignedCustomer(){
        String driverId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference assignedCustomerRef = FirebaseDatabase.getInstance()
                .getReference().child("Users").child("Drivers").child(driverId).child("customerRequest").child("CustomerRideId");
        assignedCustomerRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    status=1;
                    customerId = dataSnapshot.getValue().toString();
                    getAssignedCustomerPickupLocation();
                    getAssignedCustomerDestination();
                    getAssignedCustomerInfo();
                }
                // to notic that request is canceled and he is avaliable now
                else{
                   endRide();
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void getAssignedCustomerInfo(){
       DatabaseReference mRef= FirebaseDatabase.getInstance().getReference().child("Users").child("Customer").child(customerId);
        mRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.getChildrenCount()>0) {
                    customerInfo.setVisibility(View.VISIBLE);
                    Map<String,Object> map = (Map<String,Object>) dataSnapshot.getValue();
                    if (map.get("name")!= null){
                        customerName.setText(map.get("name").toString());
                    }
                    if (map.get("phone") != null){
                        customerPhone.setText(map.get("phone").toString());
                }
                    if (map.get("imageProfile") != null){
                        Glide.with(DriverMapActivity.this).load(map.get("imageProfile").toString()).into(customerProfile);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void getAssignedCustomerDestination(){
        String driverId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference assignedCustomerRef = FirebaseDatabase.getInstance()
                .getReference().child("Users").child("Drivers").child(driverId).child("customerRequest");
        assignedCustomerRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){

                    Map<String,Object> map= (Map<String, Object>) dataSnapshot.getValue();
                    if (map.get("destination")!=null){
                        String destination = map.get("destination").toString();
                        customerDestination.setText("Destination : "+destination);
                    } else{
                        customerDestination.setText("Destination : --");
                    }

                    Double destinationLat=0.0 ;
                    Double destinationLon=0.0 ;
                    if (map.get("destinationLat") != null){
                        destinationLat=Double.valueOf(map.get("destinationLat").toString());
                    }
                    if (map.get("destinationLon") != null){
                        destinationLon=Double.valueOf(map.get("destinationLon").toString());
                        destinationLatLon=new LatLng(destinationLat,destinationLon);
                    }


                }
                // to notic that request is canceled and he is avaliable now

            }
            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    // get Assigned Customer Location
    private DatabaseReference assignedCustomerPickupRef ;
    private ValueEventListener assignedCustomerPickupRefListener ;
    private Marker pickupMarker ;
    private void getAssignedCustomerPickupLocation(){
       Toast.makeText(this,customerId+ "", Toast.LENGTH_SHORT).show();
        assignedCustomerPickupRef=FirebaseDatabase.getInstance()
               .getReference().child("CustomerRequestsCustomerRequests").child(customerId).child("l");

       assignedCustomerPickupRefListener = assignedCustomerPickupRef.addValueEventListener(new ValueEventListener() {
           @Override
           public void onDataChange(DataSnapshot dataSnapshot) {
               if (dataSnapshot.exists() && !customerId.equals("")){
                   double locationLat=0;
                   double locationLon=0;
                   List<Object> map= (List<Object>) dataSnapshot.getValue();
                   if (map.get(0)!=null){
                       locationLat=Double.parseDouble(map.get(0).toString());
                   }
                   if (map.get(1) !=null){
                       locationLon=Double.parseDouble(map.get(1).toString());
                   }
                   LatLng pickupLatLng=new LatLng(locationLat,locationLon);

                   // add marker to customer pickup
                   pickupMarker = mMap.addMarker(new MarkerOptions().position(pickupLatLng).title("Pickup Location")
                           .icon(BitmapDescriptorFactory.fromResource(R.mipmap.pickup_marker)));
                   getRoutToMarker(pickupLatLng);
               }
           }

           @Override
           public void onCancelled(DatabaseError databaseError) {

           }
       });
   }


    private void endRide() {
        rideStatus.setText("picked customer");
        erasePolyLines();
        // remove children of driver id in Drivers and back vaalue to true
            String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            DatabaseReference driverRef=FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(userId).child("customerRequest");
            driverRef.removeValue();


        // remove request from CustomerRequests

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("CustomerRequests");
        GeoFire geoFire = new GeoFire(reference);
        geoFire.removeLocation(customerId);
        customerId="";

        // remove pickup marker from map
        if (pickupMarker != null){
            pickupMarker.remove();
        }
        customerInfo.setVisibility(View.GONE);
        erasePolyLines();
        customerId="";
        if (pickupMarker != null){
            pickupMarker.remove();
        }
        if (assignedCustomerPickupRefListener != null) {
            assignedCustomerPickupRef.removeEventListener(assignedCustomerPickupRefListener);
        }

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



    //  draw line between to points

    private void getRoutToMarker(LatLng pickupLatLng) {
        Routing routing = new Routing.Builder()
                .travelMode(AbstractRouting.TravelMode.DRIVING)
                .withListener(this)
                .alternativeRoutes(false)
                .waypoints(new LatLng(lastLocation.getLatitude(),lastLocation.getLongitude()), pickupLatLng)
                .build();
        routing.execute();
    }


    @Override
    public void onRoutingFailure(RouteException e) {
        if(e != null) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }else {
            Toast.makeText(this, "Something went wrong, Try again", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRoutingStart() {

    }

    @Override
    public void onRoutingSuccess(ArrayList<Route> route, int shortestRouteIndex) {
        if (polylines.size() > 0) {
            for (Polyline poly : polylines) {
                poly.remove();
            }
        }

        polylines = new ArrayList<>();
        //add route(s) to the map.
        for (int i = 0; i < route.size(); i++) {

            //In case of more than 5 alternative routes
            int colorIndex = i % COLORS.length;

            PolylineOptions polyOptions = new PolylineOptions();
            polyOptions.color(getResources().getColor(COLORS[colorIndex]));
            polyOptions.width(10 + i * 3);
            polyOptions.addAll(route.get(i).getPoints());
            Polyline polyline = mMap.addPolyline(polyOptions);
            polylines.add(polyline);

            Toast.makeText(getApplicationContext(), "Route " + (i + 1) + ": distance - " + route.get(i).getDistanceValue() + ": duration - " + route.get(i).getDurationValue(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRoutingCancelled() {

    }

    // clear route from the map
    private void erasePolyLines(){
        for (Polyline line : polylines){
            line.remove();
        }
        polylines.clear();
    }
}
