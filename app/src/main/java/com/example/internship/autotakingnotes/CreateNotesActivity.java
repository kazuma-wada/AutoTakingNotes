package com.example.internship.autotakingnotes;


import android.content.DialogInterface;
import android.content.Intent;

import android.hardware.Camera;
import android.media.MediaRecorder;
import android.os.Environment;

import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import android.widget.Toast;


import com.microsoft.cognitiveservices.speechrecognition.ISpeechRecognitionServerEvents;
import com.microsoft.cognitiveservices.speechrecognition.RecognitionResult;
import java.io.File;
import java.io.FileOutputStream;


public class CreateNotesActivity extends AppCompatActivity implements ISpeechRecognitionServerEvents {

    private static final String TAG = "CreateNotesActivity:";

    private GestureDetector gestureDetector;
    private MediaRecorder recorder;

    private static final String saveAudioDirPath = "/storage/emulated/0/media/Audio/AutoTakingNotes/";

    private static final String filename = "AudioNotes.wav";

    private int recordedFileNum = 0;

    SurfaceView surfaceView;
    SurfaceHolder surfaceHolder;
    Camera camera;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_notes);
        gestureDetector = new GestureDetector(this, onGestureListener);
        surfaceView =  (SurfaceView)findViewById(R.id.mySurfaceVIew);
        surfaceView.setVisibility(View.INVISIBLE);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(cameraCallback);

    }

    @Override
    protected void onStart() {
        super.onStart();
        //startMediaRecord();
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
                Toast.makeText(CreateNotesActivity.this, "Yes!!" , Toast.LENGTH_LONG).show();
            }});

        alert.show();
    }

    private void startMediaRecord() {
        try {
            // ディレクトリがなければ作る
            File dir = new File(saveAudioDirPath);
            if (!dir.exists()) {
                Log.d(TAG, "startMediaRecord: not exist dir");
                dir.mkdir();
            }

            File mediaFile = new File(saveAudioDirPath + filename);
            if (mediaFile.exists()) {
                // ファイルが既に存在していたら削除
                mediaFile.delete();
            }
            mediaFile = null;
            recorder = new MediaRecorder();
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
            recorder.setOutputFile(saveAudioDirPath + filename);
            recorder.prepare();
            recorder.start();
            Log.d(TAG, "startMediaRecord: start" );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void stopMediaRecord() {
        if (recorder == null) {
            Toast.makeText(this, "recorder = null", Toast.LENGTH_SHORT).show();
        } else {
            Log.d(TAG, "startMediaRecord: stop" );
            try {
                recorder.stop();
                recorder.reset();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        //recorder.release();
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
            Log.d(TAG, "onDoubleTap: ");
            surfaceView.setVisibility(View.VISIBLE);
            camera.takePicture(null,null,takePictureCallback);
            //stopMediaRecord();
            //startMediaRecord();
            return super.onDoubleTap(e);
        }

        @Override
        public void onLongPress(MotionEvent e) {
            Log.d(TAG, "onLongPress: ");
            //stopMediaRecord();
            super.onLongPress(e);
        }
    };

    @Override
    public void onPartialResponseReceived(String s) {

    }

    @Override
    public void onFinalResponseReceived(RecognitionResult recognitionResult) {

    }

    @Override
    public void onIntentReceived(String s) {

    }


    @Override
    public void onError(int i, String s) {

    }

    @Override
    public void onAudioEvent(boolean b) {

    }




    private SurfaceHolder.Callback cameraCallback = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder surfaceHolder) {

            //CameraOpen
            camera = Camera.open();

            //出力をSurfaceViewに設定
            try{
                camera.setPreviewDisplay(surfaceHolder);
            }catch(Exception e){
                e.printStackTrace();
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i2, int i3) {

            //プレビュースタート（Changedは最初にも1度は呼ばれる）
            camera.startPreview();
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

            //片付け
            camera.release();
            camera = null;
        }
    };

    private Camera.PictureCallback  takePictureCallback = new Camera.PictureCallback(){
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            try {
                File dir = new File(
                        Environment.getExternalStorageDirectory(), "Camera");
                if(!dir.exists()) {
                    dir.mkdir();
                }
                File f = new File(dir, "test1.jpg");
                FileOutputStream fos = new FileOutputStream(f);
                fos.write(data);
                Log.d(TAG,"写真の保存が終了しました");
                Toast.makeText(getApplicationContext(),
                        "写真を保存しました", Toast.LENGTH_LONG).show();
                fos.close();
                camera.startPreview();
                surfaceView.setVisibility(View.INVISIBLE);
            } catch (Exception e) {
            }
        }
    };


}
