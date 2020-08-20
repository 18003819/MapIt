package com.mapit.UI;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.maps.DirectionsApiRequest;
import com.google.maps.GeoApiContext;
import com.google.maps.PendingResult;
import com.google.maps.internal.PolylineEncoding;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.DirectionsRoute;
import com.google.maps.model.TravelMode;
import com.google.maps.model.Unit;
import com.mapit.Objects.PolyLineData;
import com.mapit.Objects.Trip;
import com.mapit.R;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class MapFragment extends Fragment implements OnMapReadyCallback,
                                                     GoogleMap.OnInfoWindowClickListener,
                                                     GoogleMap.OnPolylineClickListener,
                                                     GoogleApiClient.OnConnectionFailedListener
{

    //constant variable
    private static final String MAPVIEW_BUNDLE_KEY = "MapViewBundleKey";

    //the fused client is used to pick up th users live location
    private FusedLocationProviderClient client;

    //widgets
    private MapView mMapView;
    private GoogleMap googleMap;

    //Latitude and Longitude of current location
    private  static Double LAT, LON;
    private String TAG = "MapFragment";

    //creating API object that helps work out Directions
    private  GeoApiContext geoApiContext = null;

    //Array list for Polyline Object
    private ArrayList<PolyLineData> arrayPolylineData = new ArrayList<>();

    //declaring a global marker so we know which marker we are dealing with
    private Marker selectedMarker = null;

    private double duration = 99999999;

    //declarations for Transport Mode and Measurement System
    private static TravelMode mode;
    private static Unit system;
    private Login login = new Login();
    private static String email, db_Mode, db_system;

    //Firebase
    private FirebaseDatabase database = FirebaseDatabase.getInstance();

    //declarations for custom pop up
    private TextView destNameTxb, durationTxb, distanceTxb;
    private Dialog popUpDialog;
    private ImageView modeImg;
    private Button startTripBtn, cancelTripBtn;

    //Int used to store latest Trip ID
    int tripID;
    boolean found = true;

    HistoryFragment historyFragment = new HistoryFragment();

    @Nullable
    @Override public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {

        View v = inflater.inflate(R.layout.fragment_map, container, false);

        if (!Places.isInitialized()) {
            Places.initialize(getContext(), getString(R.string.google_maps_api_key));
        }

        popUpDialog = new Dialog(getActivity());
        popUpDialog.setContentView(R.layout.custompopup);

        //retrieving Transport Mode
        email = login.G_email;
        // Read from the database
        RetrieveFromDatabase();
        Log.d("TAG", "HI");

        //Code for Search Function
        // Initialize the AutocompleteSupportFragment.
        AutocompleteSupportFragment autocompleteFragment = (AutocompleteSupportFragment)
                getChildFragmentManager().findFragmentById(R.id.autocomplete_fragment);
        autocompleteFragment.setCountry("za");
        autocompleteFragment.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG));
        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                googleMap.clear();
                LatLng searchedLatLng = new LatLng(place.getLatLng().latitude, place.getLatLng().longitude);
                Marker marker = googleMap.addMarker(new MarkerOptions().position(searchedLatLng).title("Go Here?"));
                selectedMarker = marker;
                calculateDirections(marker);
            }

            @Override
            public void onError(Status status) {
            }

        });
        //code for MapView
        mMapView = (MapView) v.findViewById(R.id.mapView);
        mMapView.onCreate(savedInstanceState);
        initGoogleMap(savedInstanceState);

        //assigning client to location Services
        client = LocationServices.getFusedLocationProviderClient(getActivity());
        getLatAndLon();

        //checks to see if a trip from history has been clicked
        if(historyFragment.fromSavedHistory){
            calculateSavedHistoryDirections(historyFragment.selectedTrip);
        }

        return v;
    }

    private void initGoogleMap(Bundle savedInstanceState){
        Bundle mapViewBundle = null;
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle(MAPVIEW_BUNDLE_KEY);
        }

        mMapView.onCreate(mapViewBundle);
        mMapView.getMapAsync(this);

        //checks API context and assigns it, if it is null
        if(geoApiContext == null){
            geoApiContext = new GeoApiContext.Builder().
                    apiKey(getString(R.string.google_maps_api_key)).build();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        Bundle mapViewBundle = outState.getBundle(MAPVIEW_BUNDLE_KEY);
        if (mapViewBundle == null) {
            mapViewBundle = new Bundle();
            outState.putBundle(MAPVIEW_BUNDLE_KEY, mapViewBundle);
        }

        mMapView.onSaveInstanceState(mapViewBundle);
    }

    @Override
    public void onResume() {
        super.onResume();
        mMapView.onResume();
    }

    @Override
    public void onStart() {
        super.onStart();
        mMapView.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
        mMapView.onStop();
    }

    @Override
    public void onMapReady(final GoogleMap map){

        googleMap = map;
        //Asking for permissions
        if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        //wherever the user taps, a Marker will be placed
        map.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng point) {
                map.clear();
                map.addMarker(new MarkerOptions().position(point).title("Go Here?"));
            }
        });

        //sets the users current location
        map.setMyLocationEnabled(true);

        //enables these on click listeners for the Map View
        map.setOnInfoWindowClickListener(this);
        map.setOnPolylineClickListener(this);
    }

    @Override
    public void onPause() {
        mMapView.onPause();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        mMapView.onDestroy();
        super.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mMapView.onLowMemory();
    }

    @Override
    public void onInfoWindowClick(final Marker marker) {
        //method to display alert for user to check if they want to go to the marker they have selected
        if(marker.getTitle().equals("Go Here?"))
        {
            final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
            alertDialogBuilder.setMessage("Are you sure you would like to go here?")
                               .setCancelable(true)
                                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(@SuppressWarnings ("unused") final DialogInterface dialog,@SuppressWarnings ("unused")  final int id) {
                                        //assigns the marker to a global marker to be used later on
                                        selectedMarker = marker;
                                        dialog.dismiss();
                                        calculateDirections(marker);
                                    }
                                })
                                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(final DialogInterface dialog,@SuppressWarnings ("unused") final int id) {

                                    }
                                });

            final AlertDialog alert = alertDialogBuilder.create();
            alert.show();

        }
    }

    private void getLatAndLon(){
        //this method picks up your live geometric location and uses the lattitude and longitude of it
        if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        client.getLastLocation().addOnSuccessListener(getActivity(), new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location)
            {
                if (location != null)
                {
                    LAT = location.getLatitude();
                    LON = location.getLongitude();
                }
            }
        });

    }

    private void calculateDirections(Marker marker){
        //mthod to work out the directions which are then used to calculate the polylines
        Log.d(TAG, "calculateDirections: calculating directions.");

        com.google.maps.model.LatLng destination = new com.google.maps.model.LatLng(
                marker.getPosition().latitude,
                marker.getPosition().longitude
        );
        DirectionsApiRequest directions = new DirectionsApiRequest(geoApiContext);

        directions.mode(SetTransportMode(db_Mode));
        directions.units(SetTransportUnitSystem(db_system));
        directions.alternatives(true);
        directions.origin(
                new com.google.maps.model.LatLng(
                        LAT, LON
                )
        );
        Log.d(TAG, "calculateDirections: destination: " + destination.toString());
        directions.destination(destination).setCallback(new PendingResult.Callback<DirectionsResult>() {
            @Override
            public void onResult(DirectionsResult result) {
                addPolylinesToMap(result);
            }

            @Override
            public void onFailure(Throwable e) {
                Log.e(TAG, "calculateDirections: Failed to get directions: " + e.getMessage() );

            }
        });
    }

    private void addPolylinesToMap(final DirectionsResult result){
        //Method to add the Polylines to the MapView
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "run: result routes: " + result.routes.length);

                //clearing existing polyline data from the object and from array
                if(arrayPolylineData.size() > 0)
                {
                    for (int i = 0; i < arrayPolylineData.size(); i++)
                    {
                        arrayPolylineData.clear();
                        arrayPolylineData = new ArrayList<>();
                    }
                }
                for(DirectionsRoute route: result.routes){
                    Log.d(TAG, "run: leg: " + route.legs[0].toString());
                    List<com.google.maps.model.LatLng> decodedPath = PolylineEncoding.decode(route.overviewPolyline.getEncodedPath());

                    List<LatLng> newDecodedPath = new ArrayList<>();

                    // This loops through all the LatLng coordinates of ONE polyline.
                    for(com.google.maps.model.LatLng latLng: decodedPath){

                        newDecodedPath.add(new LatLng(
                                latLng.lat,
                                latLng.lng
                        ));
                    }
                    //adding the polyline to the map
                    Polyline polyline = googleMap.addPolyline(new PolylineOptions().addAll(newDecodedPath));
                    polyline.setColor(ContextCompat.getColor(getActivity(), R.color.grey));
                    polyline.setClickable(true);

                    //adding the polyline data to an object
                    arrayPolylineData.add(new PolyLineData(polyline, route.legs[0]));

                    //sends the shortest Duration polyline to be checked immediately when searched
                    double tempDuration = route.legs[0].duration.inSeconds;
                    if(tempDuration < duration){
                        duration = tempDuration;
                        onPolylineClick(polyline);
                        ZoomRoute(polyline.getPoints());
                    }
                    //hides the current marker
                    if(selectedMarker!=null){
                        selectedMarker.setVisible(false);
                    }

                }
            }
        });
        duration = 9999999;
    }

    @Override
    public void onPolylineClick(Polyline polyline) {
        for(PolyLineData polylineData: arrayPolylineData)
        {
            Log.d(TAG, "onPolylineClick: toString: " + polylineData.toString());
            if(polyline.getId().equals(polylineData.getPolyline().getId()))
            {
                polylineData.getPolyline().setColor(ContextCompat.getColor(getActivity(), R.color.colordark));
                polylineData.getPolyline().setZIndex(1);

                LatLng endLocation = new LatLng(
                        polylineData.getLeg().endLocation.lat,
                        polylineData.getLeg().endLocation.lng);

                Marker detailedMarker = googleMap.addMarker(new MarkerOptions()
                        .title("Destination")
                        .position(endLocation));

                DisplayDialogue(polylineData);

                detailedMarker.showInfoWindow();

                ZoomRoute(polyline.getPoints());

            }
            else
            {
                polylineData.getPolyline().setColor(ContextCompat.getColor(getActivity(), R.color.grey));

                polylineData.getPolyline().setZIndex(0);
            }
        }
    }

    public void ZoomRoute(List<LatLng> lstLatLnRoute){
        if(googleMap == null || lstLatLnRoute == null || lstLatLnRoute.isEmpty()) return;

        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
        for (LatLng latLngPoint: lstLatLnRoute)
            boundsBuilder.include(latLngPoint);

        int routePadding = 120;
        LatLngBounds latLngBounds = boundsBuilder.build();

        googleMap.animateCamera(
                CameraUpdateFactory.newLatLngBounds(latLngBounds, routePadding),
                600,
                null
        );
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    public TravelMode SetTransportMode(String DB_Mode){
        Log.d("TAG", "Mode: " + db_Mode);
        popUpDialog = new Dialog(getActivity());
        popUpDialog.setContentView(R.layout.custompopup);
        modeImg = popUpDialog.findViewById(R.id.travelMode_Img);
        switch(DB_Mode){
            case "Car":
                mode = TravelMode.DRIVING;
                modeImg.setImageResource(R.drawable.car);
                break;
            case "Walking":
                mode = TravelMode.WALKING;
                modeImg.setImageResource(R.drawable.walking);
                break;
            case "Public Transport":
                mode=TravelMode.DRIVING;
                modeImg.setImageResource(R.drawable.bus);
                break;
        }
        return mode;
    }

    public Unit SetTransportUnitSystem(String DB_System){
        switch(DB_System)
        {
            case "Imperial":
                system = Unit.IMPERIAL;
                break;
            case "Metric":
                system = Unit.METRIC;
                break;
        }
        return system;
    }

    public void DisplayDialogue(final PolyLineData p){
        //declaring Custom Pop Up items
        destNameTxb = popUpDialog.findViewById(R.id.destinationName_Txb);
        durationTxb = popUpDialog.findViewById(R.id.duration_Txb);
        distanceTxb =popUpDialog.findViewById(R.id.distance_Txb);
        startTripBtn = popUpDialog.findViewById(R.id.startTrip_Btn);
        cancelTripBtn = popUpDialog.findViewById(R.id.cancelTrip_Btn);

        destNameTxb.setText(p.getLeg().endAddress);
        durationTxb.setText(p.getLeg().duration.toString());
        distanceTxb.setText(p.getLeg().distance.toString());
        startTripBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                SaveTripDetails(p);
                popUpDialog.dismiss();
            }
        });
        popUpDialog.show();

        cancelTripBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                popUpDialog.dismiss();
                googleMap.clear();
            }
        });
    }

    private void SaveTripDetails(final PolyLineData p) {
        //adding to RealTime Database
        if(!historyFragment.fromSavedHistory){
            found = true;
            DatabaseReference myRef = database.getReference("User");
            myRef.addValueEventListener(new ValueEventListener()
            {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot)
                {
                    if(found == true)
                    {
                        for (DataSnapshot ds: dataSnapshot.getChildren())
                        {
                            if(ds.child("Email").getValue().toString().equals(email))
                            {
                                if(ds.child("SavedTrips").exists())
                                {
                                    for(DataSnapshot ds2: ds.child("SavedTrips").getChildren())
                                    {
                                        if(Integer.parseInt(ds2.child("tripID").getValue().toString()) > tripID)
                                        {
                                            tripID = Integer.parseInt(ds2.child("tripID").getValue().toString());
                                        }
                                    }
                                }
                                else
                                {
                                    tripID = 0;
                                }

                            }
                        }
                        tripID = tripID + 1;
                        Trip trip = new Trip(String.valueOf(tripID), p.getLeg().startAddress.toString(),
                                destNameTxb.getText().toString(), durationTxb.getText().toString(),
                                distanceTxb.getText().toString(), new LatLng(LAT, LON), new LatLng(p.getLeg().endLocation.lat, p.getLeg().endLocation.lng));
                        DatabaseReference Ref = database.getReference("User");
                        Ref.child(EncodeString(email)).child("SavedTrips").child("Trip" + trip.getTripID()).setValue(trip);
                        Toast.makeText(getActivity(), "Trip has been saved Successfully", Toast.LENGTH_SHORT).show();
                        found = false;
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });

        }
        else{
            popUpDialog.dismiss();
        }


    }

    public static String EncodeString(String string) {
        return string.replace(".", "_");
    }

    public void calculateSavedHistoryDirections(Trip savedTrip){
        //method to work out the directions which are then used to calculate the polyline
        Log.d(TAG, "calculateDirections: calculating directions.");
        Log.d(TAG, "Start LAT " + savedTrip.getStartPoint().latitude);
        Log.d(TAG, "Start LON " + savedTrip.getStartPoint().longitude);
        Log.d(TAG, "End LAT " + savedTrip.getEndPoint().latitude);
        Log.d(TAG, "End LON " + savedTrip.getEndPoint().longitude);

        com.google.maps.model.LatLng destination = new com.google.maps.model.LatLng(
                savedTrip.getEndPoint().latitude,
                savedTrip.getEndPoint().longitude
        );
        DirectionsApiRequest directions = new DirectionsApiRequest(geoApiContext);

        directions.mode(SetTransportMode(db_Mode));
        directions.units(SetTransportUnitSystem(db_system));
        directions.alternatives(true);
        directions.origin(
                new com.google.maps.model.LatLng(
                        savedTrip.getStartPoint().latitude, savedTrip.getStartPoint().longitude
                )
        );
        Log.d(TAG, "calculateDirections: destination: " + destination.toString());
        directions.destination(destination).setCallback(new PendingResult.Callback<DirectionsResult>() {
            @Override
            public void onResult(DirectionsResult result) {
                addPolylinesToMap(result);
            }

            @Override
            public void onFailure(Throwable e) {
                Log.e(TAG, "calculateDirections: Failed to get directions: " + e.getMessage() );

            }
        });
    }

    public void RetrieveFromDatabase(){
        DatabaseReference myRef = database.getReference("User");
        myRef.addValueEventListener(new ValueEventListener()
        {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot ds: dataSnapshot.getChildren())
                {
                    if(ds.child("Email").getValue().toString().equals(email))
                    {
                        db_Mode = ds.child("TransportMode").getValue().toString();
                        db_system = ds.child("MeasurementUnit").getValue().toString();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

}



