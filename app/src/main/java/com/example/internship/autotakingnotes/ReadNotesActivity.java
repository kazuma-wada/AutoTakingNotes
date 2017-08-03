package com.example.internship.autotakingnotes;

import android.content.Intent;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

public class ReadNotesActivity extends AppCompatActivity {

    private static final String TAG = "ReadNotesActivity";
    TextView readTextView;

    private static final int CHOOSE_FILE_CODE = 12345;

    public static final String SAVED_FILE_PATH = "/storage/emulated/0/AutoTakingNotes/recorded_text.txt";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_read_notes);
        readTextView = (TextView) findViewById(R.id.readTextView);
    }

    @Override
    protected void onResume() {
        super.onResume();
        String readText = readTextFile(SAVED_FILE_PATH);
        readTextView.setText(readText);
    }

    private String readTextFile(String file) {
        String text = null;
        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            String lineBuffer = null;
            BufferedReader reader = new BufferedReader(new InputStreamReader(fileInputStream, "UTF-8"));
            while ((lineBuffer = reader.readLine()) != null) {
                text = lineBuffer;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return text;
    }

}
