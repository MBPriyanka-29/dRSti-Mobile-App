package org.tensorflow.lite.examples.detection;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Handler;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;

/*
 * This is the second activity of our application
 * It loads the layout activity_input_tap.xml
 * It takes voice input from user using speech to text API and converts it to text
 * Passes the text recieved as an extra to the intent of DetectorActivity class.
 * */


public class input_tap extends AppCompatActivity {
    int count = 0;
    TextToSpeech t1;

    public static final String CLASS_LABEL = "org.tensorflow.lite.examples.detection.extra.CLASS";
    private static final int REQUEST_CODE_SPEECH_INPUT = 1;

    /**
     * Loads the layout, and initializes the text to speech class
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_input_tap);
//        class_number = findViewById(R.id.classNumber);
        t1 = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.ERROR) {
                    t1.setLanguage(Locale.UK);
                }
            }
        });

        String msg = "Please Tap on the screen and name the object you want to search";
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                t1.speak(msg, TextToSpeech.QUEUE_FLUSH, null);
            }
        }, 100);

    }

    /**
     * On tapping on image button, it takes voice input and converts to text
     */
    public void tapCount(View v) {

        Intent intent
                = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE,
                Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to text");

        try {
            startActivityForResult(intent, REQUEST_CODE_SPEECH_INPUT);
        } catch (Exception e) {
            Toast
                    .makeText(this, " " + e.getMessage(),
                            Toast.LENGTH_SHORT)
                    .show();
        }
    }

    /**
     * Passes the resultant string recieved from voice, to the Detector activity class through Intent
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    @Nullable Intent data) {
        String label;
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SPEECH_INPUT) {
            if (resultCode == RESULT_OK && data != null) {
                ArrayList<String> result = data.getStringArrayListExtra(
                        RecognizerIntent.EXTRA_RESULTS);
                label = Objects.requireNonNull(result).get(0);
//                class_number.setText(label);
                Toast.makeText(this, label, Toast.LENGTH_LONG).show();
                Intent intent = new Intent(this, DetectorActivity.class);
                intent.putExtra(CLASS_LABEL, label);
                startActivity(intent);
            }
        }
    }
}