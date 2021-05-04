package com.example.displaymap;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import android.content.pm.PackageManager;
import android.location.Location;
import android.widget.TextView;
import android.util.Log;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;

import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.sql.Timestamp;
import java.util.Date;
import org.apache.commons.math3.distribution.LaplaceDistribution;

public class user_report extends AppCompatActivity {

    private FusedLocationProviderClient client;

    private ListView listView;
    private ArrayList<Model> modelArrayList;
    private CustomAdapter customAdapter;
    Button btnSelect, btnDeselect, submitSelect, returnMainMenubtn;
    DatabaseReference rootRef, reportRef;
    private FirebaseAuth mAuth;

    //privacy parameter
    private Double EpsilonDP = 0.8;

    public static String[] itemList = new String[]{
            "Covid-19 positive",
            "Fever or chills",
            "Cough",
            "Shortness of breath or difficulty breathing",
            "Fatigue",
            "Muscle or body aches",
            "Headache",
            "New loss of taste or smell",
            "Sore throat",
            "Congestion or runny nose",
            "Nausea or vomiting",
            "Diarrhea",
            "Trouble breathing",
            "Persistent pain or pressure in the chest",
            "New confusion",
            "Inability to wake or stay awake",
            "Bluish lips or face"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_report);
        getSupportActionBar().setTitle("User Report Symptons");


        requestPermission();
        client = LocationServices.getFusedLocationProviderClient(this);

        listView = findViewById(R.id.listView);
        btnSelect = findViewById(R.id.viewCheckedItem);
        btnDeselect = findViewById(R.id.deselect);
        submitSelect = findViewById(R.id.submitInformation);
        returnMainMenubtn = findViewById(R.id.report_return_to_main);
        modelArrayList = getModel(false);
        customAdapter = new CustomAdapter(user_report.this, modelArrayList);
        listView.setAdapter(customAdapter);

        // TO-DO: create a new firebase account to replace this one
        //https://cloud.google.com/solutions/mobile/mobile-firebase-app-engine-flexible
        mAuth = FirebaseAuth.getInstance();
        // Database reference pointing to root of database
        rootRef = FirebaseDatabase.getInstance().getReference();

        // Database reference pointing to demo node
        // TO-DO: replace with the database reference point
        reportRef = rootRef.child("Covid-19-report");
        final FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            // do your stuff
        } else {
            signInAnonymously();
        }

        FirebaseDatabase database = FirebaseDatabase.getInstance();

        btnSelect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                modelArrayList = getModel(true);
                customAdapter = new CustomAdapter(user_report.this, modelArrayList);
                listView.setAdapter(customAdapter);
                Toast.makeText(getApplicationContext(), "Checked all items",
                        Toast.LENGTH_SHORT).show();
            }
        });
        btnDeselect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                modelArrayList = getModel(false);
                customAdapter = new CustomAdapter(user_report.this,modelArrayList);
                listView.setAdapter(customAdapter);
                Toast.makeText(getApplicationContext(), "Unchecked all items",
                        Toast.LENGTH_SHORT).show();
            }
        });

        returnMainMenubtn.setOnClickListener(new View.OnClickListener()  {
            public void onClick(View v)
            {
                Intent intent = new Intent(user_report.this, MainActivity.class);
                startActivity(intent);
            }
        });

        submitSelect.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (ActivityCompat.checkSelfPermission(user_report.this, ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ) {
                    return;
                }

                //if (!Utilities.isConnectingToInternet(user_report.this)) {
                //    Toast.makeText(getApplicationContext(),"Internet is not available. Please connect to internet",Toast.LENGTH_LONG).show();
                //    return;
                //}

                client.getLastLocation().addOnSuccessListener(user_report.this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            //add laplace noise into user location
                            double longitude = laplaceMechanismCount(location.getLongitude(), EpsilonDP);
                            double latitude = laplaceMechanismCount(location.getLatitude(), EpsilonDP);
                            Log.i("Longitude: orig", location.getLongitude()+"");
                            Log.i("Longitude: new", longitude +"");
                            Log.i("Latitude: orig", location.getLatitude()+"");
                            Log.i("Latitude: new", latitude +"");
                            Toast.makeText(getApplication(), "Submitted user information!",
                                    Toast.LENGTH_SHORT).show();

                            Date date = new Date();
                            Timestamp ts = new Timestamp(date.getTime());

                            String userID = reportRef.push().getKey();
                            reportRef.child(userID).child("timestamp").setValue(ts.toString());
                            reportRef.child(userID).child("Longitude").setValue(String.valueOf(location.getLongitude()));
                            reportRef.child(userID).child("Latitude").setValue(String.valueOf(location.getLatitude()));
                            for(Model sym: modelArrayList) {
                                reportRef.child(userID).child(sym.getItem()).setValue(sym.isSelected());
                            }

                            Log.i("Timestamp:", ts.toString()+"");

                        }
                    }

                });
            }
        });
    }

    private void signInAnonymously() {
        mAuth.signInAnonymously().addOnSuccessListener(this, new  OnSuccessListener<AuthResult>() {
            @Override
            public void onSuccess(AuthResult authResult) {
                Log.i("SUCCESS", "anonymous user sign in");
            }
        })
                .addOnFailureListener(this, new OnFailureListener() {
                    @SuppressLint("LongLogTag")
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        Log.e("signInAnonymously:FAILURE", String.valueOf(exception));
                    }
                });
    }

    private void requestPermission(){
        ActivityCompat.requestPermissions(this, new String[]{ACCESS_FINE_LOCATION}, 1);
        Log.i("invoke", "requestPermission");
    }

    private ArrayList<Model> getModel(boolean isSelect) {
        ArrayList<Model>list = new ArrayList<>();
        for (int i = 0; i < itemList.length; i++) {
            Model model = new Model();
            model.setSelected(isSelect);
            model.setItem(itemList[i]);
            list.add(model);
        }
        return list;
    }

    //LaplaceDistribution(double mu,
    //                   double beta)
    //    mu - the mean for the distribution
    //    beta - scale parameter (must be positive)
    // epsilon - between 0 and 1 and pick delta > 0
    private double laplaceMechanismCount(double realLocation, double epsilon) {

        LaplaceDistribution ld = new LaplaceDistribution(0, 1 / epsilon);
        double noise = ld.sample();
        return realLocation + noise;

    }

}