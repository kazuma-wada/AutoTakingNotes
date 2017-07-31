package com.example.internship.autotakingnotes;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

public class CreateNotesActivity extends AppCompatActivity {

    private static final String TAG = "CreateNotesActivity:";

    private ConstraintLayout constraintLayout;
    private GestureDetector gestureDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_notes);

        constraintLayout = (ConstraintLayout) findViewById(R.id.constraintLayout);
        gestureDetector = new GestureDetector(this, onGestureListener);
    }

    public void endCreate(View viwe) {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("ノート作成を");
        alert.setMessage("ノート作成を終了しますか？");
        alert.setPositiveButton("Yes", new DialogInterface.OnClickListener(){
          public void onClick(DialogInterface dialog , int which){
              //ここにYESの処理
              Intent intent = new Intent(CreateNotesActivity.this,MainActivity.class);
              startActivity(intent);
              Log.d(TAG, "ダイアログ:YES");
          }});

        alert.setNegativeButton("No", new DialogInterface.OnClickListener(){
            public void onClick(DialogInterface dialog , int which){
                //ここにNOの処理
                Log.d(TAG,":NO");
                //Toast.makeText(CreateNotesActivity.this, "Yes!!" , Toast.LENGTH_LONG).show();
            }});

        alert.show();
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        gestureDetector.onTouchEvent(event);
        return false;
    }

    private final GestureDetector.SimpleOnGestureListener onGestureListener = new GestureDetector.SimpleOnGestureListener() {

        /**
         * ダブルタップされたときの処理
         * @param e motion event
         * @return
         */
        @Override
        public boolean onDoubleTap(MotionEvent e) {
            return super.onDoubleTap(e);
        }
    };
}