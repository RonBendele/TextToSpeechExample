package net.bendele.tts.example;

import java.util.HashMap;

import android.annotation.SuppressLint;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.os.Build;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.speech.tts.TextToSpeech.OnUtteranceCompletedListener;
import android.util.Log;

public class SpeechIntentService extends IntentService implements
        OnInitListener, OnAudioFocusChangeListener {

    // Logging constants
    private static final boolean DEBUG = true;
    private static final String BENDELE = "BENDELE";
    private static final String CLASS = "SpeechIntentService - ";

    // KEY_PARAM_UTTERANCE_ID
    private static final String DONE = "done";

    protected static final String TEXT = "text";

    private String text2speak;
    private TextToSpeech tts;
    private AudioManager audioManager;

    private void myLog (String msg){
        if (DEBUG){
            if (msg != ""){
                msg = " - " + msg;
            }
            String caller = Thread.currentThread().getStackTrace()[3]
                    .getMethodName();
            Log.d(BENDELE, CLASS + caller + msg);
        }
    }

    public SpeechIntentService() {
        super("SpeechIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        myLog ("");
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        text2speak = intent.getStringExtra("text");
        tts = new TextToSpeech(getApplicationContext(), this);
    }

    @Override
    public void onInit(int status) {
        myLog ("");
        if (status == TextToSpeech.SUCCESS) {
            // the call to set the utterance listener must be in the
            // onInit method (inside setTts()), in the SUCCESS check.
            setTts();
            HashMap<String, String> myHashParams = new HashMap<String, String>();
            myHashParams.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, DONE);
            audioManager.requestAudioFocus(this,
                    AudioManager.STREAM_NOTIFICATION,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
            myLog ("text2speak = " + text2speak);
            tts.speak(text2speak, TextToSpeech.QUEUE_FLUSH, myHashParams);
        } else {
            myLog (" - FAILED");
        }
    }

    @SuppressLint("NewApi")
    @SuppressWarnings("deprecation")
    public void setTts() {
        myLog ("");
        if (Build.VERSION.SDK_INT >= 15) {
            myLog ("Build.VERSION.SDK_INT >= 15");
            tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override
                public void onDone(String utteranceId) {
                    onDoneSpeaking(utteranceId);
                }

                @Override
                public void onError(String utteranceId) {
                }

                @Override
                public void onStart(String utteranceId) {
                }
            });
        } else {
            myLog ("Build.VERSION.SDK_INT < 15");
            tts.setOnUtteranceCompletedListener(new OnUtteranceCompletedListener() {
                @Override
                public void onUtteranceCompleted(String utteranceId) {
                    onDoneSpeaking(utteranceId);
                }
            });
        }
    }

    private void onDoneSpeaking(String utteranceId) {
        myLog ("");
        if (utteranceId.equals(DONE) || utteranceId == DONE) {
            audioManager.abandonAudioFocus(this);
            stopSelf();
        }
    }

    @Override
    public void onDestroy() {
        myLog ("-----");
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }

    @Override
    public void onAudioFocusChange(int focusChange) {

    }

}
