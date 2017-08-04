package com.example.internship.autotakingnotes;


import android.content.DialogInterface;
import android.content.Intent;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.os.AsyncTask;
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

import com.microsoft.bing.speech.SpeechClientStatus;
import com.google.gson.Gson;
import com.microsoft.cognitiveservices.speechrecognition.ISpeechRecognitionServerEvents;
import com.microsoft.cognitiveservices.speechrecognition.MicrophoneRecognitionClient;
import com.microsoft.cognitiveservices.speechrecognition.RecognitionResult;

import java.io.BufferedWriter;
import com.microsoft.projectoxford.vision.VisionServiceClient;
import com.microsoft.projectoxford.vision.VisionServiceRestClient;
import com.microsoft.projectoxford.vision.contract.LanguageCodes;
import com.microsoft.projectoxford.vision.contract.OCR;
import com.microsoft.projectoxford.vision.rest.VisionServiceException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import com.microsoft.cognitiveservices.speechrecognition.RecognitionStatus;
import com.microsoft.cognitiveservices.speechrecognition.SpeechRecognitionMode;
import com.microsoft.cognitiveservices.speechrecognition.SpeechRecognitionServiceFactory;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;

import java.lang.ref.WeakReference;


import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Objects;

public class CreateNotesActivity extends AppCompatActivity implements ISpeechRecognitionServerEvents {

    private static final String TAG = "CreateNotesActivity";

    private GestureDetector gestureDetector;

    private static final String saveImageDirPath = "/storage/emulated/0/Camera/";

    // 画像テキスト化用
    SurfaceView surfaceView;
    SurfaceHolder surfaceHolder;
    Camera camera;
    public static final String subscriptionKey_vision = "e9253a508b0044459342c3bcef4816b0";
    private VisionServiceClient client;
    private Bitmap bitmap;

    // 音声テキスト化用
    private MicrophoneRecognitionClient micClient = null;
    private FinalResponseStatus isReceivedResponse = FinalResponseStatus.NotReceived;
    private String textFromMic = "";
    private String textFromCamera = "";

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

    private String getAuthenticationUri() {
        return this.getString(R.string.authenticationUri);
    }
    
    private String getTextFileName() {
        return "recorded_text.txt";
    }

    private String getSaveTextPath() {
        return getSaveDirPath() + getTextFileName();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_notes);
        gestureDetector = new GestureDetector(this, onGestureListener);
        surfaceView = (SurfaceView) findViewById(R.id.mySurfaceVIew);
        surfaceView.setVisibility(View.INVISIBLE);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(cameraCallback);

        if (client == null) {
            client = new VisionServiceRestClient(subscriptionKey_vision);
        }

    }

    @Override
    protected void onStart() {
        super.onStart();
        startSpeechToText();
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopSpeechToText();
        if (textFromCamera.equals("") && !textFromMic.equals("")) {
            saveTextFile(getSaveTextPath(), textFromMic);
        }
    }

    public void endCreate(View view) {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("ノート作成を");
        alert.setMessage("ノート作成を終了しますか？");
        alert.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                //ここにYESの処理
                Intent intent = new Intent(CreateNotesActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });


        alert.setNegativeButton("No", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(CreateNotesActivity.this, "Yes!!", Toast.LENGTH_LONG).show();
            }
        });

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
            stopSpeechToText();
            surfaceView.setVisibility(View.VISIBLE);
            camera.autoFocus(mAutoFocusCallback);
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

    public void readImageFile(String imageFilename) throws Exception {
        try {
            bitmap = BitmapFactory.decodeFile(saveImageDirPath + imageFilename);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void doRecognize() {
        try {
            new doRequest(this).execute();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public  void createText(JSONObject json) {
        String err = "";
        try {
            JSONArray json_array = json.getJSONArray("regions");
            if (json_array.length() != 0) {
                JSONArray json_array_line = json_array.getJSONObject(0).getJSONArray("lines");
                JSONArray json_array_line_word;

                for (int i = 0; i < json_array_line.length(); i++) {
                    json_array_line_word = json_array_line.getJSONObject(i).getJSONArray("words");
                    for (int j = 0; j < json_array_line_word.length(); j++) {
                        textFromCamera += json_array_line_word.getJSONObject(j).getString("text");
                    }
                    textFromCamera += "\n";
                }
            }
        } catch (Exception e) {
            err = e.getMessage();
        }

        /**
         * save text file
         */
        new AsyncTask<String, String, String>() {
            @Override
            protected String doInBackground(String... strings) {
                saveTextFile(strings[0],strings[1]);
                return strings[2];
            }

            @Override
            protected void onPostExecute(String s) {
                super.onPostExecute(s);
                if (s.equals("")) {
                    Toast.makeText(getApplicationContext(),"テキスト保存完了",Toast.LENGTH_SHORT).show();
                    textFromCamera = "";
                    textFromMic = "";
                } else {
                    Toast.makeText(getApplicationContext(),s,Toast.LENGTH_SHORT).show();
                }
                startSpeechToText();
            }
        }.execute(getSaveTextPath(), textFromCamera + textFromMic, err);
        
    }

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
        } catch (IOException e) {
            message = e.getMessage();
        }
        Log.d(TAG, "saveTextFile: "+ message);
    }


    private String process() throws VisionServiceException, IOException, InterruptedException {

        Gson gson = new Gson();
        String result;

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, output);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(output.toByteArray());

        OCR ocr;
        ocr = this.client.recognizeText(inputStream, LanguageCodes.AutoDetect, true);
        result = gson.toJson(ocr);
        return result;
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

    private Camera.AutoFocusCallback mAutoFocusCallback = new Camera.AutoFocusCallback() {
        @Override
        public void onAutoFocus(boolean b, Camera camera) {
            Toast.makeText(getApplicationContext(),"撮影中...", Toast.LENGTH_SHORT).show();
            camera.takePicture(null,null,takePictureCallback);
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
                fos.close();
                camera.startPreview();
                surfaceView.setVisibility(View.INVISIBLE);

                readImageFile("test1.jpg");

            } catch (Exception e) {
                e.printStackTrace();
            }

            doRecognize();

        }
    };

    private void startSpeechToText() {
        textFromMic = "";
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

    private void stopSpeechToText() {
        if (this.micClient != null) {
            this.micClient.endMicAndRecognition();
        }
    }

    @Override
    public void onPartialResponseReceived(String response) {
        //Log.d(TAG, "onPartialResponseReceived: " + response);
    }

    @Override
    public void onFinalResponseReceived(RecognitionResult recognitionResult) {
        boolean isFinalDictationMessage = (recognitionResult.RecognitionStatus == RecognitionStatus.EndOfDictation ||
                recognitionResult.RecognitionStatus == RecognitionStatus.DictationEndSilenceTimeout);

        if (null != this.micClient && isFinalDictationMessage) {
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
                //saveTextFile(getSaveDirPath() + getTextFileName(), "(" + recognitionResult.Results[0].DisplayText + ")\n");
                textFromMic+= "(" + recognitionResult.Results[0].DisplayText + ")\n";
            }
        }
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
            Toast.makeText(this,"音声取得開始", Toast.LENGTH_SHORT).show();
        } else {
            this.micClient.endMicAndRecognition();
            Toast.makeText(this, "音声取得終了", Toast.LENGTH_SHORT).show();
        }
    }
    

    private static class doRequest extends AsyncTask<String, String, String> {
        // Store error message
        private Exception e = null;

        private WeakReference<CreateNotesActivity> recognitionActivity;

        doRequest(CreateNotesActivity activity) {
            recognitionActivity = new WeakReference<CreateNotesActivity>(activity);
        }

        @Override
        protected String doInBackground(String... args) {
            try {
                if (recognitionActivity.get() != null) {
                    return recognitionActivity.get().process();
                }
            } catch (Exception e) {
                this.e = e;    // Store error
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            JSONObject json_object = new JSONObject();
            try {
                json_object = new JSONObject(result);
            }catch(Exception e){
                e.printStackTrace();
            }
            recognitionActivity.get().createText(json_object);
        }

    }
}
