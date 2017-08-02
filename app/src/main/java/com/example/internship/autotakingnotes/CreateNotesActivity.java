package com.example.internship.autotakingnotes;


import android.content.DialogInterface;
import android.content.Intent;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.media.MediaRecorder;
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
import android.view.TextureView;
import android.view.View;

import android.widget.TextView;
import android.widget.Toast;


import com.google.gson.Gson;
import com.microsoft.cognitiveservices.speechrecognition.ISpeechRecognitionServerEvents;
import com.microsoft.cognitiveservices.speechrecognition.RecognitionResult;
import com.microsoft.projectoxford.vision.VisionServiceClient;
import com.microsoft.projectoxford.vision.VisionServiceRestClient;
import com.microsoft.projectoxford.vision.contract.AnalysisResult;
import com.microsoft.projectoxford.vision.contract.HandwritingRecognitionOperation;
import com.microsoft.projectoxford.vision.contract.HandwritingRecognitionOperationResult;
import com.microsoft.projectoxford.vision.contract.HandwritingTextLine;
import com.microsoft.projectoxford.vision.contract.HandwritingTextWord;
import com.microsoft.projectoxford.vision.contract.LanguageCodes;
import com.microsoft.projectoxford.vision.contract.OCR;
import com.microsoft.projectoxford.vision.rest.VisionServiceException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import java.io.InterruptedIOException;
import java.lang.ref.WeakReference;
import java.net.URI;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.http.Header;
import org.json.JSONObject;

import static android.R.attr.bitmap;


public class CreateNotesActivity extends AppCompatActivity implements ISpeechRecognitionServerEvents {

    private static final String TAG = "CreateNotesActivity:";

    private GestureDetector gestureDetector;
    private MediaRecorder recorder;

    private static final String saveAudioDirPath = "/storage/emulated/0/media/Audio/AutoTakingNotes/";
    private static final String filename = "AudioNotes.wav";
    private static final String saveImageDirPath = "/storage/emulated/0/Camera/";

    private int recordedFileNum = 0;

    SurfaceView surfaceView;
    SurfaceHolder surfaceHolder;
    Camera camera;

    public static final String subscriptionKey_vision = "e9253a508b0044459342c3bcef4816b0";
    private VisionServiceClient client;
    private Bitmap bitmap;
    private int retryCountThreshold = 30;
    private  TextView tv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_notes);
        gestureDetector = new GestureDetector(this, onGestureListener);
        surfaceView = (SurfaceView) findViewById(R.id.mySurfaceVIew);
        surfaceView.setVisibility(View.VISIBLE);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(cameraCallback);

        if (client == null) {
            client = new VisionServiceRestClient(subscriptionKey_vision);
        }
        tv = (TextView)findViewById(R.id.textView);

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
        alert.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                //ここにYESの処理
                Intent intent = new Intent(CreateNotesActivity.this, MainActivity.class);
                startActivity(intent);
                Log.d(TAG, "ダイアログ:YES");
            }
        });

        alert.setNegativeButton("No", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                //ここにNOの処理

                Log.d(TAG, ":NO");
                try {
                    readImageFile("test1.jpg");
                }catch(Exception e){

                }
                doRecognize();
                //doDescribe();
                Toast.makeText(CreateNotesActivity.this, "Yes!!", Toast.LENGTH_LONG).show();
            }
        });

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
            Log.d(TAG, "startMediaRecord: start");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void stopMediaRecord() {
        if (recorder == null) {
            Toast.makeText(this, "recorder = null", Toast.LENGTH_SHORT).show();
        } else {
            Log.d(TAG, "startMediaRecord: stop");
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


    public void readImageFile(String imageFilename) throws Exception {
        Log.d(TAG, "readImageが呼ばれました");
        try {
             bitmap = BitmapFactory.decodeFile(saveImageDirPath + imageFilename);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.d(TAG, "ビットマップを表示しますーーーーーーー");
        Log.d(TAG, bitmap.toString());
        Log.d(TAG, "ーーーーーーーーーーーー");
    }

    public void doRecognize() {
        try {
            new doRequest(this).execute();
        } catch (Exception e) {

        }
    }

    public void doDescribe() {
        try {
            new doRequest(this).execute();
        } catch (Exception e) {
        }
    }

    public void writeImageFile(Bitmap image) {


    }

    private String process() throws VisionServiceException, IOException, InterruptedException {

        Gson gson = new Gson();
        String result = "null";
//        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
//            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, output);
//            Log.d(TAG,"input前");
//
//            try (ByteArrayInputStream inputStream = new ByteArrayInputStream(output.toByteArray())) {
//
//                //HandwritingRecognitionOperation operation = client.createHandwritingRecognitionOperationAsync(inputStream);
//                HandwritingRecognitionOperation operation = client.createHandwritingRecognitionOperationAsync(inputStream);
//                HandwritingRecognitionOperationResult operationResult;
//
//
//                int retryCount = 0;
//                do {
//                    if (retryCount > retryCountThreshold) {
//                        throw new InterruptedException("Can't get result after retry in time.");
//                    }
//                    Thread.sleep(1000);
//                    operationResult = client.getHandwritingRecognitionOperationResultAsync(operation.Url());
//                }
//                while (operationResult.getStatus().equals("NotStarted") || operationResult.getStatus().equals("Running"));
//
//                result = gson.toJson(operationResult);
//                Log.d(TAG, result);
//                return result;
//
//            } catch (Exception ex) {
//                ex.printStackTrace();
//            }
//        } catch (Exception ex) {
//            ex.printStackTrace();
//        }

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, output);
            ByteArrayInputStream inputStream = new ByteArrayInputStream(output.toByteArray());

            OCR ocr;
            ocr = this.client.recognizeText(inputStream, LanguageCodes.Japanese, true);

            result = gson.toJson(ocr);
            Log.d("result", result);




        return result;
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

    private static class doRequest extends AsyncTask<String, String, String> {
        // Store error message
        private Exception e = null;

       private WeakReference<CreateNotesActivity> recognitionActivity;

        public doRequest(CreateNotesActivity activity) {
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
        protected void onPostExecute(String data) {
            super.onPostExecute(data);

        }
    }


}
