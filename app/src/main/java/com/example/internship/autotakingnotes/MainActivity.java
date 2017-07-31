package com.example.internship.autotakingnotes;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.support.design.widget.BottomNavigationView;

import com.microsoft.cognitiveservices.speechrecognition.RecognitionResult;

public class MainActivity extends AppCompatActivity implements  View.OnClickListener {


    public static final String TAG = "mainActivity";

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    Log.d(TAG, "呼ばれました:1");
                    Intent intent = new Intent(MainActivity.this,CreateNotesActivity.class);
                    startActivity(intent);
                    return true;
                case R.id.navigation_dashboard:
                    Log.d(TAG, "呼ばれました:2");

                    //
                    //Intent intent = new Intent(MainActivity.this,CreateActivity.class);
                    //startActivity(intent);
                    return true;

                case R.id.navigation_notifications:
                    Log.d(TAG, "呼ばれました:3");
                    return true;
            }
            return false;
        }


    };



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView navigation = (BottomNavigationView)findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
        Button button = (Button) findViewById(R.id.button);
        button.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {

    }




}
