package com.mapit.UI;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.mapit.R;

public class ProfileFragment extends Fragment
{
    private Spinner transportMode_Spinner;
    private Switch systemSwitch;
    private String transportModeString, distanceSystem;
    private TextView email_Txt;

    private String loggedUser;
    private static String db_System, db_Mode;

    //creating an instance of Login Activity
    Login login = new Login();

    //declaring Firebase Database
    FirebaseDatabase database = FirebaseDatabase.getInstance();

    @Nullable
    @Override public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.fragment_profile,container,false);

        //Initializing Components
        transportMode_Spinner = (Spinner) view.findViewById(R.id.transportSpinner);
        systemSwitch = (Switch) view.findViewById(R.id.metricImperialSwitch);
        email_Txt = (TextView) view.findViewById(R.id.P_email_txb);

        //creating an adapter to link Array to SPinner
        ArrayAdapter<CharSequence> modeAdpater = new ArrayAdapter<CharSequence>(getActivity(), R.layout.spinner_item, getResources().getStringArray(R.array.transportModes));
        modeAdpater.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        transportMode_Spinner.setAdapter(modeAdpater);

        //sets email text to user that just logged in
        loggedUser = login.G_email;
        DownloadFromDatabase(loggedUser);
        email_Txt.setText(loggedUser);


        //sets the Transport Mode to the database
        transportMode_Spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
            {
                transportModeString = transportMode_Spinner.getSelectedItem().toString();
                //adding to RealTime Database
                DatabaseReference myRef = database.getReference("User");
                myRef.child(EncodeString(loggedUser)).child("TransportMode").setValue(transportModeString);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        //Setting the distance System for the User
        systemSwitch.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if(systemSwitch.isChecked() == true)
                    distanceSystem = "Metric";
                if(systemSwitch.isChecked() == false)
                    distanceSystem = "Imperial";

                //adding to RealTime Database
                DatabaseReference myRef = database.getReference("User");
                myRef.child(EncodeString(loggedUser)).child("MeasurementUnit").setValue(distanceSystem);
            }
        });


        return view;
    }
    public static String EncodeString(String string) {
        return string.replace(".", "_");
    }

    public void DownloadFromDatabase(final String user)
    {
        // Read from the database
        DatabaseReference myRef = database.getReference("User");
        myRef.addValueEventListener(new ValueEventListener()
        {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot ds: dataSnapshot.getChildren())
                {
                    if(ds.child("Email").getValue().toString().equals(user))
                    {
                        db_Mode = ds.child("TransportMode").getValue().toString();
                        db_System = ds.child("MeasurementUnit").getValue().toString();


                        switch (db_Mode){
                            case "Car":
                                transportMode_Spinner.setSelection(0);
                                break;
                            case "Walking":
                                transportMode_Spinner.setSelection(1);
                                break;
                            case "Public Transport":
                                transportMode_Spinner.setSelection(2);
                                break;
                        }

                        switch (db_System){
                            case "Imperial":
                                systemSwitch.setChecked(false);
                                break;
                            case "Metric":
                                systemSwitch.setChecked(true);
                                break;
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }
}
