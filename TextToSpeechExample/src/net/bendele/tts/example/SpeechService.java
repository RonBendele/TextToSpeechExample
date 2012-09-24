package net.bendele.tts.example;

import java.util.HashMap;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.os.Build;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.speech.tts.TextToSpeech.OnUtteranceCompletedListener;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

public class SpeechService extends Service implements OnInitListener,
        OnAudioFocusChangeListener {

    // Logging constants
    private static final boolean DEBUG = true;
    private static final String BENDELE = "BENDELE";
    private static final String CLASS = "SpeechService - ";

    // KEY_PARAM_UTTERANCE_ID
    private static final String DONE = "done";

    // used to set the intent in the calling app
    // as well as get the intent here
    protected static final String TEXT = "text";
    protected static final String INITIALIZE = "initialize";
    protected static final String BROADCAST_ACTION = "net.bendele.runwalk.tts.notavailable";

    private static TextToSpeech tts;
    private static AudioManager audioManager;

    private String text2speak;
    private boolean initialize = false;

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

    @Override
    public void onCreate() {
        myLog ("");
    }

    @Override
    public void onStart(Intent intent, int startid) {
        myLog ("");
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        initialize = intent.getBooleanExtra(INITIALIZE, false);
        text2speak = intent.getStringExtra(TEXT);
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
            if (initialize) {
                // tts.isLanguageAvailable(Locale.getDefault());
                tts.playSilence(1, TextToSpeech.QUEUE_ADD, myHashParams);
            } else {
                audioManager.requestAudioFocus(this,
                        AudioManager.STREAM_NOTIFICATION,
                        AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
                myLog ("text2speak = " + text2speak);
                tts.speak(text2speak, TextToSpeech.QUEUE_FLUSH, myHashParams);
            }
        } else {
            // report back to the caller that TTS is not available
            Intent intent = new Intent(BROADCAST_ACTION);
            sendBroadcast(intent);
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

    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        /*
         * Need this because of the 2 autoManager calls They require a listener
         * and the listener requires this.
         */
    }

    /*
     * Can do more by looking at this link:
     * http://developer.android.com/resources/articles/tts.html
     */
}