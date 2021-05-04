package com.example.displaymap;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;
import android.app.DownloadManager;
import android.net.Uri;
import android.os.Environment;

import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polygon;
import com.google.maps.android.data.Feature;
import com.google.maps.android.data.geojson.GeoJsonFeature;
import com.google.maps.android.data.geojson.GeoJsonLayer;
import com.google.maps.android.data.geojson.GeoJsonPolygonStyle;


import org.json.JSONException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.FileReader;

import static java.io.File.createTempFile;
import static java.lang.Double.*;

public class DisplayTrendMap extends AppCompatActivity
        implements
            AdapterView.OnItemSelectedListener,
            OnMapReadyCallback,
            GoogleMap.OnPolygonClickListener {

    private final static String mLogTag = "TrendMap";
    private MapView mapView;
    private GoogleMap gmap;
    private boolean mIsRestore;
    private static final String MAP_VIEW_BUNDLE_KEY = "MapViewBundleKey";

    private String[] mapAreas ={"Houston_super_neighborhoods"};
    private Button updateButton, returnMainMenuButton;

    private FirebaseStorage storage;
    private StorageReference storageRef, geojson_spaceRef, color_spaceRef;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_trend_map);
        getSupportActionBar().setTitle("Covid-19 Trend Map");

        Spinner spin = (Spinner) findViewById(R.id.trend_spinner);
        spin.setOnItemSelectedListener((AdapterView.OnItemSelectedListener) this);

        //Creating the ArrayAdapter instance having the  name list
        ArrayAdapter aa = new ArrayAdapter(this,android.R.layout.simple_spinner_item,mapAreas);
        aa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        //Setting the ArrayAdapter data on the Spinner
        spin.setAdapter(aa);

        returnMainMenuButton = (Button) findViewById(R.id.trend_map_return_to_main_menu);
        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();

        //TO-DO, upload the trend.txt file into FireBase storage
        //"trend.txt" - store the prediction vulnerability levels
        geojson_spaceRef = storageRef.child("images/houston_super_neighborhoods.json");
        color_spaceRef = storageRef.child("images/trend.txt");

        //color_spaceRef = storageRef.child("images/trend_map.png");

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            // do your stuff
        } else {
            signInAnonymously();
        }

        Bundle mapViewBundle = null;
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle(MAP_VIEW_BUNDLE_KEY);
        }

        mapView = findViewById(R.id.mapview);
        mapView.onCreate(mapViewBundle);
        mapView.getMapAsync(this);
        mIsRestore = savedInstanceState != null;

        // Get the SupportMapFragment and request notification when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        returnMainMenuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(DisplayTrendMap.this, MainActivity.class);
                startActivity(intent);
            }
        });

    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        gmap = googleMap;
        gmap.setMinZoomPreference(9);
        gmap.setIndoorEnabled(true);
        UiSettings uiSettings = gmap.getUiSettings();
        uiSettings.setIndoorLevelPickerEnabled(true);
        uiSettings.setMyLocationButtonEnabled(true);
        uiSettings.setMapToolbarEnabled(true);
        uiSettings.setCompassEnabled(true);
        uiSettings.setZoomControlsEnabled(true);

        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(29.68, -95.39), 10));

        googleMap.setOnPolygonClickListener(this);
        retrieveFileFromResource();
    }
    // [END maps_poly_activity_on_map_ready]

    private void retrieveFileFromResource() {
        try {
            GeoJsonLayer layer = new GeoJsonLayer(gmap, R.raw.houston_super_neighborhoods, this);
            color_spaceRef = storageRef.child("images/trend.txt");
            addGeoJsonLayerToMap(layer);
        } catch (IOException e) {
            Log.e(mLogTag, "GeoJSON file could not be read");
        } catch (JSONException e) {
            Log.e(mLogTag, "GeoJSON file could not be converted to a JSONObject");
        }
    }

    private void addGeoJsonLayerToMap(GeoJsonLayer layer) {

        addColorsToMarkers(layer);
        layer.addLayerToMap();
        // Demonstrate receiving features via GeoJsonLayer clicks.
        layer.setOnFeatureClickListener(new GeoJsonLayer.GeoJsonOnFeatureClickListener() {
            @Override
            public void onFeatureClick(Feature feature) {
                Toast.makeText(DisplayTrendMap.this,
                        "Feature clicked: " + feature.getProperty("SNBNAME"),
                        Toast.LENGTH_SHORT).show();
            }

        });
    }

    @SuppressLint("LongLogTag")
    private void addColorsToMarkers(final GeoJsonLayer layer) {
        // Iterate over all the features stored in the layer

        final List<Double> color = new ArrayList<Double>();

        //store the downloaded color ID file "vulnerability.txt"
        final File rootPath = new File(DisplayTrendMap.this.getFilesDir(), "download");
        //Log.e("firebase ","rootpath for the local file created " +rootPath.toString());
        if (!rootPath.exists()) {
            rootPath.mkdir();
        }
        try {
            final File localFile  = new File(rootPath, "trend.txt");

            color_spaceRef.getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                    Log.e("firebase ","local file created " +localFile.toString());

                    if (localFile.canRead()){
                        StringBuilder text = new StringBuilder();
                        BufferedReader bfreader = null;
                        try {
                            bfreader = new BufferedReader(new FileReader(localFile));
                            String line = bfreader.readLine();
                            Log.i("Trend color map areas list: ", line +"");
                            String[] string_array = line.split(",");
                            for(int i = 0; i < string_array.length; i++){
                                color.add(i, parseDouble(string_array[i]));
                            }
                            Log.i("color: ", color.toString() +"");
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        double colorID;
                        Log.i("color size=: ", color.size() +"");
                        for (GeoJsonFeature feature : layer.getFeatures()) {
                            // Check if the magnitude property exists
                            if (feature.getProperty("OBJECTID") != null) {
                                int objectID = Integer.parseInt(feature.getProperty("OBJECTID"));
                                GeoJsonPolygonStyle polygonStyle = new GeoJsonPolygonStyle();
                                polygonStyle.setStrokeColor(Color.BLACK);
                                polygonStyle.setStrokeWidth(1);

                                if (objectID < color.size() && color.size()>0)
                                    colorID = color.get(objectID);
                                else if (color.size()==0)
                                    colorID = 0;
                                else
                                    colorID = 999;

                                if(colorID <= 15)
                                    polygonStyle.setFillColor(Color.parseColor("#DEEBF7"));
                                else if (colorID <= 30 && colorID > 15)
                                    polygonStyle.setFillColor(Color.parseColor("#80D7FF"));
                                else if (colorID <= 45 && colorID > 30)
                                    polygonStyle.setFillColor(Color.parseColor("#8FAADC"));
                                else if (colorID <= 60 && colorID > 45 )
                                    polygonStyle.setFillColor(Color.parseColor("#2E75B6"));
                                else if (colorID <= 75 && colorID > 60)
                                    polygonStyle.setFillColor(Color.parseColor("#2F5597"));
                                else
                                    polygonStyle.setFillColor(Color.parseColor("#203864"));
                                feature.setPolygonStyle(polygonStyle);
                            }
                        }
                    }

                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    Log.e("firebase ",";local tem file not created  created " +exception.toString());
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    //used for update the map image
    public void updateMap() {
        if (!Utilities.isConnectingToInternet(DisplayTrendMap.this)) {
            Toast.makeText(getApplicationContext(),"Internet is not available. Please connect to internet",Toast.LENGTH_LONG).show();
            return;
        }

        File localFile = null;
        try {
            localFile = createTempFile("images", "png");
        } catch (IOException e) {
            e.printStackTrace();
        }

        geojson_spaceRef.getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                // Local temp file has been created
                //vulnerability_imgView.setImageResource(R.drawable.vulnerability_map);
                Log.i("vulnerability map", "** SUCCESS *** Uploaded vulnerability map from firebase");
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@androidx.annotation.NonNull Exception exception) {
                // Handle any errors
                Log.e("vulnerability map", "Failed to upload vulnerability map from firebase");
            }
        });

        color_spaceRef.getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                // Local temp file has been created
                //trend_imgView.setImageResource(R.drawable.trend_map);
                Log.i("trend  map", "** SUCCESS *** Uploaded trend map from firebase");
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle any errors
                Log.e("trend map", "Failed to upload trend map from firebase");
            }
        });

    }

    @Override
    public void onPolygonClick(Polygon polygon) {
        // Flip the values of the red, green, and blue components of the polygon's color.
        int color = polygon.getStrokeColor() ^ 0x00ffffff;
        polygon.setStrokeColor(color);
        color = polygon.getFillColor() ^ 0x00ffffff;
        polygon.setFillColor(color);

        Toast.makeText(this, "Area type " + polygon.getTag().toString(), Toast.LENGTH_SHORT).show();
    }

    private void signInAnonymously() {
        mAuth.signInAnonymously().addOnSuccessListener(this, new  OnSuccessListener<AuthResult>() {
            @SuppressLint("LongLogTag")
            @Override
            public void onSuccess(AuthResult authResult) {
                //updateMap();
                Log.e("signInAnonymously:Success", "authentication ok");
            }
        })
                .addOnFailureListener(this, new OnFailureListener() {
                    @SuppressLint("LongLogTag")
                    @Override
                    public void onFailure(@androidx.annotation.NonNull Exception exception) {
                        Log.e("signInAnonymously:FAILURE", String.valueOf(exception));
                    }
                });
    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        mapView.onStart();
        //updateUI(currentUser);
    }

    //Performing action onItemSelected and onNothing selected
    @Override
    public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long id) {
        //Toast.makeText(getApplicationContext(), mapAreas[position], Toast.LENGTH_LONG).show();
    }

    @Override
    public void onNothingSelected(AdapterView<?> arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        Bundle mapViewBundle = outState.getBundle(MAP_VIEW_BUNDLE_KEY);
        if (mapViewBundle == null) {
            mapViewBundle = new Bundle();
            outState.putBundle(MAP_VIEW_BUNDLE_KEY, mapViewBundle);
        }

        mapView.onSaveInstanceState(mapViewBundle);
    }
    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
    }
    @Override
    protected void onPause() {
        mapView.onPause();
        super.onPause();
    }
    @Override
    protected void onDestroy() {
        mapView.onDestroy();
        super.onDestroy();
    }
    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }
}