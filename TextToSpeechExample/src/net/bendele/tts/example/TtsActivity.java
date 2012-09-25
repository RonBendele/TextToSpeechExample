package net.bendele.tts.example;

import java.util.HashMap;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.speech.tts.TextToSpeech.OnUtteranceCompletedListener;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

/*
 * This is a test bed for my Run / Walk Intervals Timer application which
 * uses TextToSpeech.
 *
 * I chose to instantiate a TextToSpeech object each time the user presses
 * the Speak button and then in onDoneSpeaking(), stop/shutdown the object.
 * I don't want to hang on to resources any longer than absolutely necessary.
 * Because of this choice, the speaking actually takes place in onInit(),
 * rather than speak().
 */

public class TtsActivity extends Activity implements OnInitListener,
        OnAudioFocusChangeListener {

    // Logging constants
    private static final boolean DEBUG = true;
    private static final String BENDELE = "BENDELE";
    private static final String CLASS = "TtsActivity - ";

    // KEY_PARAM_UTTERANCE_ID
    private static final String DONE = "done";

    // request codes for intents
    private int REQ_TTS_STATUS_CHECK = 0;

    private TextToSpeech tts;

    private EditText inputText;
    private Button appButton;
    private Button svcButton;
    private Button intentButton;

    private String text2speak;

    protected AudioManager audioManager;

    private void myLog(String msg) {
        if (DEBUG) {
            if (msg != "") {
                msg = " - " + msg;
            }
            String caller = Thread.currentThread().getStackTrace()[3]
                    .getMethodName();
            Log.d(BENDELE, CLASS + caller + msg);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        myLog("");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        inputText = (EditText) findViewById(R.id.input_text);
        appButton = (Button) findViewById(R.id.app_button);
        svcButton = (Button) findViewById(R.id.svc_button);
        intentButton = (Button) findViewById(R.id.intent_button);

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        registerReceiver(ttsNotAvailableReceiver, new IntentFilter(
                SpeechService.BROADCAST_ACTION));

        appButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                text2speak = inputText.getText().toString();
                if (text2speak != null && text2speak.length() > 0) {
                    Toast.makeText(TtsActivity.this,
                            "Application saying: " + text2speak,
                            Toast.LENGTH_LONG).show();
                    speak();
                } else {
                    Toast.makeText(TtsActivity.this,
                            "You MUST have something in the edit box!",
                            Toast.LENGTH_LONG).show();
                }
            }
        });

        svcButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                text2speak = inputText.getText().toString();
                if (text2speak != null && text2speak.length() > 0) {
                    Toast.makeText(TtsActivity.this,
                            "Service saying: " + text2speak, Toast.LENGTH_LONG)
                            .show();
                    svcSpeak(text2speak);
                } else {
                    Toast.makeText(TtsActivity.this,
                            "You MUST have something in the edit box!",
                            Toast.LENGTH_LONG).show();
                }
            }
        });

        intentButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                text2speak = inputText.getText().toString();
                if (text2speak != null && text2speak.length() > 0) {
                    Toast.makeText(TtsActivity.this,
                            "IntentService saying: " + text2speak,
                            Toast.LENGTH_LONG).show();
                    intentSpeak(text2speak);
                } else {
                    Toast.makeText(TtsActivity.this,
                            "You MUST have something in the edit box!",
                            Toast.LENGTH_LONG).show();
                }
            }
        });

        /*
         * The next 3 lines are my way of determining if TTS is properly
         * installed. Call the SpeechService through an Intent, passing it an
         * Initialize variable. If it does not initialize, the service sends out
         * a broadcast, picked up here in a BroadcastReceiver called
         * ttsNotAvailableReceiver. I do this here in the onCreate() of the main
         * activity so I can set a flag to prevent the user from trying to use
         * TTS again.
         */
        Intent intent = new Intent(this, SpeechService.class);
        intent.putExtra(SpeechService.INITIALIZE, true);
        startService(intent);

        /*
         * The next set of lines are the 'proper' way to determine if TTS is
         * installed and running. onActivityResult is the callback for them.
         * This does not work for Jelly Bean 4.1.1 on Nexus 7 as of 9/24/12.
         */
        /*
         * Intent checkIntent = new Intent();
         * checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA); try
         * { startActivityForResult(checkIntent, REQ_TTS_STATUS_CHECK); } catch
         * (ActivityNotFoundException e) {
         *
         * prevents crash from: No Activity found to handle Intent {
         * act=android.speech.tts.engine.CHECK_TTS_DATA }
         *
         * ttsNotAvailable(); myLog(e.getMessage()); }
         */

    } // onCreate

    private BroadcastReceiver ttsNotAvailableReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            ttsNotAvailable();
        }
    };

    /*
     * We would normally set a flag, but for simplicity's sake I just disable
     * all the widgets the user can see.
     */
    private void ttsNotAvailable() {
        inputText.setEnabled(false);
        appButton.setEnabled(false);
        svcButton.setEnabled(false);
        intentButton.setEnabled(false);
    }

    private void speak() {
        myLog("");
        // the actual speaking takes place in onInit
        tts = new TextToSpeech(this, this);
    }

    private void svcSpeak(String what) {
        myLog(what);
        Intent intent = new Intent(this, SpeechService.class);
        intent.putExtra(SpeechService.TEXT, what);
        startService(intent);
    }

    private void intentSpeak(String what) {
        myLog(what);
        Intent intent = new Intent(this, SpeechIntentService.class);
        intent.putExtra(SpeechIntentService.TEXT, what);
        startService(intent);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        myLog("");
        if (requestCode == REQ_TTS_STATUS_CHECK) {
            myLog("MY_DATA_CHECK_CODE");
            switch (resultCode) {
            case TextToSpeech.Engine.CHECK_VOICE_DATA_PASS:
                myLog("CHECK_VOICE_DATA_PASS");
                // success, create the TTS instance
                tts = new TextToSpeech(getApplicationContext(), this);
                break;
            case TextToSpeech.Engine.CHECK_VOICE_DATA_BAD_DATA:
                myLog("CHECK_VOICE_DATA_BAD_DATA");
            case TextToSpeech.Engine.CHECK_VOICE_DATA_MISSING_DATA:
                myLog("CHECK_VOICE_DATA_MISSING_DATA");
            case TextToSpeech.Engine.CHECK_VOICE_DATA_MISSING_VOLUME:
                myLog("CHECK_VOICE_DATA_MISSING_VOLUME");

                AlertDialog.Builder ttsInstallBuilder = new AlertDialog.Builder(
                        this);
                ttsInstallBuilder
                        .setTitle("Install TTS data file?")
                        .setMessage(
                                "A Text-To-Speech data file is not installed.\nDo you want to install it now via Android Market?")
                        .setPositiveButton("Yes", dialogClickListener)
                        .setNegativeButton("No", dialogClickListener).show();

                break;
            case TextToSpeech.Engine.CHECK_VOICE_DATA_FAIL:
            default:
                AlertDialog.Builder ttsAlertBuilder = new AlertDialog.Builder(
                        this);
                ttsAlertBuilder
                        .setCancelable(true)
                        .setTitle("Text-To-Speech Service error")
                        .setMessage(
                                "TTS Engine startup FAILED!\n(It may not be installed)")
                        .setPositiveButton("OK", null).show();
            }
        }
    }

    private void installTTS() {
        Intent installIntent = new Intent();
        installIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
        startActivity(installIntent);
    }

    DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
        /*
         * If TTS is not installed, ask the user to install it
         */
        @Override
        public void onClick(DialogInterface dialog, int which) {
            switch (which) {
            case DialogInterface.BUTTON_POSITIVE:
                installTTS();
                break;
            case DialogInterface.BUTTON_NEGATIVE:
                break;
            }
        }
    };

    @Override
    public void onInit(int status) {
        myLog("");
        if (status == TextToSpeech.SUCCESS) {
            // the call to set the utterance listener must be in the
            // onInit method (inside setTts()), in the SUCCESS check.
            setTts();
            HashMap<String, String> myHashParams = new HashMap<String, String>();
            myHashParams.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, DONE);
            audioManager.requestAudioFocus(this,
                    AudioManager.STREAM_NOTIFICATION,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
            myLog("text2speak = " + text2speak);
            tts.speak(text2speak, TextToSpeech.QUEUE_ADD, myHashParams);
        } else if (status == TextToSpeech.ERROR) {
            Toast.makeText(TtsActivity.this,
                    "Error occurred while initializing Text-To-Speech engine",
                    Toast.LENGTH_LONG).show();
        }
    }

    @SuppressLint("NewApi")
    @SuppressWarnings("deprecation")
    public void setTts() {
        myLog("");
        if (Build.VERSION.SDK_INT >= 15) {
            myLog("Build.VERSION.SDK_INT >= 15");
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
            myLog("Build.VERSION.SDK_INT < 15");
            tts.setOnUtteranceCompletedListener(new OnUtteranceCompletedListener() {
                @Override
                public void onUtteranceCompleted(String utteranceId) {
                    onDoneSpeaking(utteranceId);
                }
            });
        }
    }

    private void onDoneSpeaking(String utteranceId) {
        myLog("");
        if (utteranceId.equals(DONE) || utteranceId == DONE) {
            audioManager.abandonAudioFocus(this);
            // moved stop() & shutdown() from onDestroy to here because the user
            // can press the speak button multiple times. I was getting a memory
            // leak because I didn't have a one-to-one relationship with
            // instantiating the TTS object and stopping/shutting it down. I
            // don't get that leak by calling them here. This is a side-effect
            // of my architecture decision.
            if (tts != null) {
                tts.stop();
                tts.shutdown();
            }
        }
    }

    @Override
    protected void onDestroy() {
        myLog("-----");
        stopService(new Intent(this, SpeechService.class));
        unregisterReceiver(ttsNotAvailableReceiver);
        super.onDestroy();
    }

    /*
     * Need this because of the 2 audioManager calls They require a listener and
     * the listener requires this.
     */
    @Override
    public void onAudioFocusChange(int focusChange) {
        // we would react to other apps requesting focus or releasing or
        // releasing it
    }

}