package com.example.internship.autotakingnotes;

import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import com.microsoft.bing.speech.SpeechClientStatus;
import com.microsoft.cognitiveservices.speechrecognition.DataRecognitionClient;
import com.microsoft.cognitiveservices.speechrecognition.ISpeechRecognitionServerEvents;
import com.microsoft.cognitiveservices.speechrecognition.MicrophoneRecognitionClient;
import com.microsoft.cognitiveservices.speechrecognition.RecognitionResult;
import com.microsoft.cognitiveservices.speechrecognition.RecognitionStatus;
import com.microsoft.cognitiveservices.speechrecognition.SpeechRecognitionMode;
import com.microsoft.cognitiveservices.speechrecognition.SpeechRecognitionServiceFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

public class CreateNotesActivity extends AppCompatActivity implements ISpeechRecognitionServerEvents {

    private static final String TAG = "CreateNotesActivity";

    private GestureDetector gestureDetector;

    // 音声テキスト化用
    private int waitSeconds = 10;
    private MediaRecorder recorder;
    private DataRecognitionClient audioDataClient = null;
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

    private String getSaveAudioDirPath() {
        return "/storage/emulated/0/AutoTakingNotes/";
    }

    private String getWaveFile() {
        return "AudioNotes.wav";
    }

    private String getFilePath() {
        return getSaveAudioDirPath() + getWaveFile();
    }

    private String getAuthenticationUri() {
        return this.getString(R.string.authenticationUri);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_notes);

        gestureDetector = new GestureDetector(this, onGestureListener);
        CreateNotesActivity This = this;
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

    private void startMediaRecord() {
        try {
            // ディレクトリがなければ作る
            File dir = new File(getSaveAudioDirPath());
            if (!dir.exists()) {
                Log.d(TAG, "startMediaRecord: not exist dir");
                dir.mkdir();
            }

            File mediaFile = new File(getFilePath());
            if (mediaFile.exists()) {
                // ファイルが既に存在していたら削除
                mediaFile.delete();
            }
            mediaFile = null;
            recorder = new MediaRecorder();
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
            recorder.setOutputFile(getFilePath());
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
            Log.d(TAG, "stopMediaRecord:");
            try {
                recorder.stop();
                recorder.reset();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
//        startMediaRecord();
    }

    @Override
    protected void onStop() {
        super.onStop();
//        recorder.release();
        if (this.micClient != null) {
            this.micClient.endMicAndRecognition();
        }
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
//            stopMediaRecord();
//            startMediaRecord();
            if (null != micClient) {
                micClient.endMicAndRecognition();
            }
            return super.onDoubleTap(e);
        }

        @Override
        public void onLongPress(MotionEvent e) {
            Log.d(TAG, "onLongPress: ");
            executeSpeechToText();
//            stopMediaRecord();
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
//        if (null == this.audioDataClient) {
//            this.audioDataClient = SpeechRecognitionServiceFactory.createDataClient(
//                    this,
//                    this.getMode(),
//                    this.getDefaultLocale(),
//                    this, this.getPrimaryKey()
//            );
//            this.audioDataClient.setAuthenticationUri(this.getAuthenticationUri());
//        }
//        this.SendAudioHelper(this.getFilePath());
    }

    private void SendAudioHelper(String filename) {
        RecognitionTask doDataReco = new RecognitionTask(this.audioDataClient, this.getMode(), filename);
        try {
            doDataReco.execute().get(waitSeconds, TimeUnit.SECONDS);
        } catch (Exception e) {
            doDataReco.cancel(true);
            isReceivedResponse = FinalResponseStatus.Timeout;
            Log.d(TAG, "SendAudioHelper: " + isReceivedResponse);
        }
    }


    @Override
    public void onPartialResponseReceived(String response) {
        Log.d(TAG, "onPartialResponseReceived: " + response);
    }

    @Override
    public void onFinalResponseReceived(RecognitionResult recognitionResult) {
        Log.d(TAG, "onFinalResponseReceived: start");
        boolean isFinalDictationMessage = this.getMode() == SpeechRecognitionMode.LongDictation &&
                                            (recognitionResult.RecognitionStatus == RecognitionStatus.EndOfDictation ||
                                                recognitionResult.RecognitionStatus == RecognitionStatus.DictationEndSilenceTimeout);

        if (null != this.micClient && ((this.getMode() == SpeechRecognitionMode.ShortPhrase) || isFinalDictationMessage)) {
            this.micClient.endMicAndRecognition();
        }
        Log.d(TAG, "onFinalResponseReceived: " + isFinalDictationMessage);
        if (isFinalDictationMessage) {
            this.isReceivedResponse = FinalResponseStatus.OK;
        }

        if (!isFinalDictationMessage) {
            Log.d(TAG, "onFinalResponseReceived:********* Final n-BEST Results *********");
            for (int i = 0; i < recognitionResult.Results.length; i++) {
                Log.d(TAG, "onFinalResponseReceived: " + "[" + i + "]" + " Confidence=" + recognitionResult.Results[i].Confidence +
                        " Text=\"" + recognitionResult.Results[i].DisplayText + "\"");
            }
        }
        Log.d(TAG, "onFinalResponseReceived: end");
    }

    @Override
    public void onIntentReceived(String payload) {
        Log.d(TAG, "onIntentReceived: " + payload);
    }


    @Override
    public void onError(int i, String response) {
        Log.d(TAG, "onError: error code: " + SpeechClientStatus.fromInt(i));
        Log.d(TAG, "onError: " + response);
    }

    @Override
    public void onAudioEvent(boolean recording) {
        Log.d(TAG, "onAudioEvent: " + recording);
        if (recording) {
            Log.d(TAG, "onAudioEvent: Please start speaking");
        }

        if (!recording) {
            this.micClient.endMicAndRecognition();
            Log.d(TAG, "onAudioEvent: ");
        }
    }

    /*
     * Speech recognition with data (for example from a file or audio source).
     * The data is broken up into buffers and each buffer is sent to the Speech Recognition Service.
     * No modification is done to the buffers, so the user can apply their
     * own VAD (Voice Activation Detection) or Silence Detection
     *
     * @param dataClient
     * @param recoMode
     * @param filename
     */
    private class RecognitionTask extends AsyncTask<Void, Void, Void> {

        private static final String TAG = "RecognitionTask:";

        private DataRecognitionClient dataClient;
        private SpeechRecognitionMode recoMode;
        private String filepath;

        RecognitionTask(DataRecognitionClient dataClient, SpeechRecognitionMode recoMode, String filepath) {
            this.dataClient = dataClient;
            this.recoMode = recoMode;
            this.filepath = filepath;
        }

        @Override
        protected Void doInBackground(Void... voids) {

            try {
//                FileInputStream fileStream = new FileInputStream(filepath);
                InputStream fileStream = getAssets().open("whatstheweatherlike.wav");
                int bytesRead = 0;
                byte[] buffer = new byte[1024];
                do {
                    // byte bufferに送る音声データを取得する
                    bytesRead = fileStream.read(buffer);
                    if (bytesRead > -1) {
                        // サービスのための音声データの送信
                        dataClient.sendAudio(buffer, bytesRead);
                    }
                } while (bytesRead > 0);

            } catch (Throwable throwable) {
                throwable.printStackTrace();
            } finally {
                Log.d(TAG, "doInBackground: endAudio");
                dataClient.endAudio();
            }
            return null;
        }
    }
}
