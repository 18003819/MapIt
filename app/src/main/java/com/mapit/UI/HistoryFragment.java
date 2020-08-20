package com.mapit.UI;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.mapit.Objects.Trip;
import com.mapit.R;
import com.mapit.Adapters.RecyclerAdapter;

import java.util.ArrayList;
import java.util.List;

import static com.android.volley.VolleyLog.TAG;

public class HistoryFragment extends Fragment {

    RecyclerView recyclerView;
    TextView no_Trips_Txtp;

    public static Trip selectedTrip ;
    public static boolean fromSavedHistory;

    //Firebase
    FirebaseDatabase database = FirebaseDatabase.getInstance();

    //declarations to receive Trip History from database for the specific user
    Login login = new Login();
    String email;

    //For Recycler Adapter
    private RecyclerAdapter mAdapter;

    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, @Nullable final ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_history, container, false);

        no_Trips_Txtp = (TextView) v.findViewById(R.id.no_Trips_TextView);
        recyclerView = (RecyclerView) v.findViewById(R.id.viewTripRecycler);
        final RecyclerView.LayoutManager recyce = new
                LinearLayoutManager(getActivity(),LinearLayoutManager.VERTICAL,false);
        recyclerView.setLayoutManager(recyce);

        fromSavedHistory = false;
        email = login.G_email;
        DatabaseReference myRef = database.getReference("User");
        myRef.addValueEventListener(new ValueEventListener()
        {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot)
            {
                boolean exists = false;
                final List<Trip> tripList = new ArrayList<Trip>();
                for (DataSnapshot ds: dataSnapshot.getChildren())
                {
                    if(ds.child("Email").getValue().toString().equals(email))
                    {
                        if(ds.child("SavedTrips").exists())
                        {
                            no_Trips_Txtp.setVisibility(View.INVISIBLE);
                            for(DataSnapshot ds2: ds.child("SavedTrips").getChildren())
                            {
                                String tripID = ds2.child("tripID").getValue().toString();
                                String startLocation = ds2.child("startLocation").getValue().toString();
                                String endLocation = ds2.child("endLocation").getValue().toString();
                                String duration = ds2.child("duration").getValue().toString();
                                String distance = ds2.child("distance").getValue().toString();
                                double startPointLat = Double.parseDouble(ds2.child("startPoint").child("latitude").getValue().toString());
                                double startPointLon = Double.parseDouble(ds2.child("startPoint").child("longitude").getValue().toString());
                                double endPointLat = Double.parseDouble(ds2.child("endPoint").child("latitude").getValue().toString());
                                double endPointLon = Double.parseDouble(ds2.child("endPoint").child("longitude").getValue().toString());

                                tripList.add(new Trip(tripID,startLocation , endLocation, duration,distance,
                                        new LatLng(startPointLat, startPointLon),
                                        new LatLng(endPointLat,endPointLon)));

                                exists = true;
                            }
                        }

                    }
                }

                mAdapter = new RecyclerAdapter(getContext(), tripList);
                recyclerView.setAdapter(mAdapter);

                mAdapter.setOnItemClickListener(new RecyclerAdapter.OnItemClickListener() {
                    @Override
                    public void onItemClick(int position) {
                        Log.d(TAG, "onClick 2 : " + position);
                        selectedTrip = tripList.get(position);
                        fromSavedHistory = true;
                        getFragmentManager().beginTransaction().replace(R.id.fragment_container, new MapFragment()).commit();

                        //MapFragment mapFragment = new MapFragment();
                       // mapFragment.calculateSavedHistoryDirections(tripList.get(position));


                    }
                });

                if(exists == true)
                {
                    recyclerView.setVisibility(View.VISIBLE);
                    no_Trips_Txtp.setVisibility(View.INVISIBLE);
                }
                else
                {
                    no_Trips_Txtp.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.INVISIBLE);
                }


            }


            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        return v;
    }
}