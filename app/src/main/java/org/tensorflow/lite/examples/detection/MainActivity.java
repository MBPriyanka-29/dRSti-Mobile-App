package org.tensorflow.lite.examples.detection;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.Toast;

import java.util.Locale;

/**
 * This is the main activity of application which loads the activity_main.xml layout
 * Gives an intro msg through voice
 * On tapping on launch button it redirects to second activity
 */
public class MainActivity extends AppCompatActivity {
    public int width;
    public int height;
    static MainActivity INSTANCE;
    TextToSpeech t1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Toast.makeText(this, "Welcome to dRSHti ", Toast.LENGTH_SHORT).show();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        t1 = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.ERROR) {
                    t1.setLanguage(Locale.ENGLISH);
                }
            }
        });

        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                t1.speak("welcome to Drishti!", TextToSpeech.QUEUE_FLUSH, null);
            }
        }, 100);
    }

    /**
     * Launches second activity on clicking the button
     */
    public void launchApp(View view) {
        // Start input_tap Activity
        Toast.makeText(this, "you Just Launched Our APP! ", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(this, input_tap.class);
        startActivity(intent);
    }


}