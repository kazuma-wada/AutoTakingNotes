package com.example.internship.autotakingnotes;


import android.content.DialogInterface;
import android.content.Intent;

import android.hardware.Camera;
import android.media.MediaRecorder;
import android.os.Environment;

import android.os.AsyncTask;
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

import com.microsoft.bing.speech.SpeechClientStatus;
import com.microsoft.cognitiveservices.speechrecognition.DataRecognitionClient;
import com.microsoft.cognitiveservices.speechrecognition.ISpeechRecognitionServerEvents;
import com.microsoft.cognitiveservices.speechrecognition.MicrophoneRecognitionClient;
import com.microsoft.cognitiveservices.speechrecognition.RecognitionResult;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import com.microsoft.cognitiveservices.speechrecognition.RecognitionStatus;
import com.microsoft.cognitiveservices.speechrecognition.SpeechRecognitionMode;
import com.microsoft.cognitiveservices.speechrecognition.SpeechRecognitionServiceFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.concurrent.TimeUnit;

public class CreateNotesActivity extends AppCompatActivity implements ISpeechRecognitionServerEvents {

    private static final String TAG = "CreateNotesActivity";

    private GestureDetector gestureDetector;

    // 画像テキスト化用
    SurfaceView surfaceView;
    SurfaceHolder surfaceHolder;
    Camera camera;

    // 音声テキスト化用
    private int waitSeconds = 200;

    private MicrophoneRecognitionClient micClient = null;
    private FinalResponseStatus isReceivedResponse = FinalResponseStatus.NotReceived;

    private enum FinalResponseStatus { NotReceived, OK, Timeout }

    public String getPrimaryKey() {
        return this.getString(R.string.primaryKey);
    }

    private String getDefaultLocale() {
        return "ja-jp";
    }

    private SpeechRecognitionMode getMode() {
        return SpeechRecognitionMode.LongDictation;
    }

    private String getSaveDirPath() {
        return "/storage/emulated/0/AutoTakingNotes/";
    }

    private String getWaveFile() {
        return "AudioNotes.wav";
    }

    private String getFilePath() {
        return getSaveDirPath() + getWaveFile();
    }

    private String getAuthenticationUri() {
        return this.getString(R.string.authenticationUri);
    }
    
    private String getTextFileName() {
        return "recorded_text.txt";
    }

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
        executeSpeechToText();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (this.micClient != null) {
            this.micClient.endMicAndRecognition();
        }
    }

    public void endCreate(View view) {
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
            executeSpeechToText();
            return super.onDoubleTap(e);
        }

        @Override
        public void onLongPress(MotionEvent e) {
            Log.d(TAG, "onLongPress: ");
            if (null != micClient) {
                micClient.endMicAndRecognition();
            }
            super.onLongPress(e);
        }
    };

    private void executeSpeechToText() {
        Log.d(TAG, "executeSpeechToText: ");
        if (this.micClient == null) {
            this.micClient = SpeechRecognitionServiceFactory.createMicrophoneClient(
                    this,
                    this.getMode(),
                    this.getDefaultLocale(),
                    this,
                    this.getPrimaryKey()
            );
            this.micClient.setAuthenticationUri(this.getAuthenticationUri());
        }
        this.micClient.startMicAndRecognition();
    }

    @Override
    public void onPartialResponseReceived(String response) {
        Log.d(TAG, "onPartialResponseReceived: " + response);
    }

    @Override
    public void onFinalResponseReceived(RecognitionResult recognitionResult) {
        Log.d(TAG, "onFinalResponseReceived: start" );
        boolean isFinalDictationMessage = (recognitionResult.RecognitionStatus == RecognitionStatus.EndOfDictation ||
                                                recognitionResult.RecognitionStatus == RecognitionStatus.DictationEndSilenceTimeout);

        if (null != this.micClient && isFinalDictationMessage) {
            Log.d(TAG, "onFinalResponseReceived: endmic");
            this.micClient.endMicAndRecognition();
        }

        if (isFinalDictationMessage) {
            this.isReceivedResponse = FinalResponseStatus.OK;
        }

        if (!isFinalDictationMessage) {
            Log.d(TAG, "********* Final n-BEST Results *********");
            for (int i = 0; i < recognitionResult.Results.length; i++) {
                Log.d(TAG, "onFinalResponseReceived: " + "[" + i + "]" + " Confidence=" + recognitionResult.Results[i].Confidence +
                        " Text=\"" + recognitionResult.Results[i].DisplayText + "\"");
            }
            if (recognitionResult.Results.length>=1) {
                saveTextFile(getSaveDirPath() + getTextFileName(), "(" + recognitionResult.Results[0].DisplayText + ")\n");
            }
        }
        Log.d(TAG, "onFinalResponseReceived: end");
    }

    @Override
    public void onIntentReceived(String payload) {}

    @Override
    public void onError(int i, String response) {
        Log.d(TAG, "onError: error code: " + SpeechClientStatus.fromInt(i));
        Log.d(TAG, "onError: " + response);
    }

    @Override
    public void onAudioEvent(boolean recording) {
        if (recording) {
            Toast.makeText(this,"Start Record", Toast.LENGTH_SHORT).show();
        }

        if (!recording) {
            this.micClient.endMicAndRecognition();
            Toast.makeText(this, "Stop Record", Toast.LENGTH_SHORT).show();
        }
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
            Log.d(TAG, "onPictureTaken: ");
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
                e.printStackTrace();
            }
            executeSpeechToText();
        }
    };

    private void saveTextFile(String filepath, String inputText) {
        String message = "";
        try {
            FileOutputStream outStream = new FileOutputStream(filepath, true);
            OutputStreamWriter outWriter = new OutputStreamWriter(outStream);
            BufferedWriter bufferedWriter = new BufferedWriter(outWriter);
            bufferedWriter.write(inputText);
            bufferedWriter.flush();
            bufferedWriter.close();
            
            message = "テキストを保存しました。";
        } catch (FileNotFoundException e) {
            message = e.getMessage();
        } catch (IOException e) {
            message = e.getMessage();
        }
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private String readTextFile(String file) {
        String text = null;
        try {
            FileInputStream fileInputStream = openFileInput(file);
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
