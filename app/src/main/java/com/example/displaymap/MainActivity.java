package com.example.displaymap;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity  {
    Button user_report_btn, display_map_btn, trend_map_btn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportActionBar().hide();
        //getSupportActionBar().setTitle("Covid-19 Report");

        user_report_btn = (Button) findViewById(R.id.user_report_button);
        display_map_btn = (Button) findViewById(R.id.display_map_button);
        trend_map_btn = (Button) findViewById(R.id.display_trend_map_button);

        user_report_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, user_report.class);
                System.out.println("click on User Report");
                startActivity(intent);
            }
        });

        display_map_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, DisplayVulnerabilityMap.class);
                System.out.println("click on Display Map");
                startActivity(intent);
            }
        });

        trend_map_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, DisplayTrendMap.class);
                startActivity(intent);
            }
        });
    }
}