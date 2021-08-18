
package org.tensorflow.lite.examples.detection;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Join;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.util.TypedValue;


import androidx.appcompat.app.AppCompatActivity;

import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Queue;

import org.tensorflow.lite.examples.detection.env.BorderedText;
import org.tensorflow.lite.examples.detection.env.ImageUtils;
import org.tensorflow.lite.examples.detection.env.Logger;
import org.tensorflow.lite.examples.detection.tflite.Detector.Recognition;

/**
 * A tracker that handles non-max suppression and matches existing objects to new detections.
 * This class is the core class of application, it detects the object and draws bounding box across it,
 * along with the direction that user has to navigate to.
 */
public class MultiBoxTracker extends AppCompatActivity implements TextToSpeech.OnInitListener {
    TextToSpeech tts;
    private boolean ttsOk;

    public static final String EXTRA_MESSAGE = "org.tensorflow.lite.examples.detection.tracking.extra.MESSAGE";
    private static final float TEXT_SIZE_DIP = 18;
    private static final float MIN_SIZE = 16.0f;
    private static final int[] COLORS = {
            Color.BLUE,
            Color.RED,
            Color.GREEN,
            Color.YELLOW,
            Color.CYAN,
            Color.MAGENTA,
            Color.WHITE,
            Color.parseColor("#55FF55"),
            Color.parseColor("#FFA500"),
            Color.parseColor("#FF8888"),
            Color.parseColor("#AAAAFF"),
            Color.parseColor("#FFFFAA"),
            Color.parseColor("#55AAAA"),
            Color.parseColor("#AA33AA"),
            Color.parseColor("#0D0068")
    };

    final List<Pair<Float, RectF>> screenRects = new LinkedList<Pair<Float, RectF>>();
    private final Logger logger = new Logger();
    private final Queue<Integer> availableColors = new LinkedList<Integer>();
    private final List<TrackedRecognition> trackedObjects = new LinkedList<TrackedRecognition>();
    private final Paint boxPaint = new Paint();
    private final float textSizePx;
    private final BorderedText borderedText;
    private Matrix frameToCanvasMatrix;
    private int frameWidth;
    private int frameHeight;
    private int sensorOrientation;
    public String label;
    public int pixel_height, pixel_width;
    public TextToSpeech t1;
    public String dir;
    public double Xp, Xm, Yp, Ym;
    public double scale;

    public MultiBoxTracker(final Context context, String label, int pixel_height, int pixel_width) {
        this.label = label;
//        this.pixel_height = pixel_height * 5/6;
        this.pixel_height = 1380;
        this.pixel_width = pixel_width;
//        setContentView(R.layout.dummy);

        this.scale = 0.1;


        this.Xp = this.pixel_width / 2 + this.scale * this.pixel_width;
        this.Xm = this.pixel_width / 2 - this.scale * this.pixel_width;

        this.Yp = this.pixel_height / 2 + this.scale * this.pixel_height;
        this.Ym = this.pixel_height / 2 - this.scale * this.pixel_height;

        for (final int color : COLORS) {
            availableColors.add(color);
        }

        boxPaint.setColor(Color.RED);
        boxPaint.setStyle(Style.STROKE);
        boxPaint.setStrokeWidth(10.0f);
        boxPaint.setStrokeCap(Cap.ROUND);
        boxPaint.setStrokeJoin(Join.ROUND);
        boxPaint.setStrokeMiter(100);

        textSizePx =
                TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, context.getResources().getDisplayMetrics());
        borderedText = new BorderedText(textSizePx);
        tts = new TextToSpeech(context, this);
    }

    /**
     * Initialize Text to Speech class
     */
    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            ttsOk = true;
        } else {
            ttsOk = false;
        }
    }

    /**
     * Function to give voice output of the string parameter passed
     */
    public void speak(String text) {
        tts.setSpeechRate(1.5f);
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
            }
        }, 2000);

    }

    public synchronized void setFrameConfiguration(
            final int width, final int height, final int sensorOrientation) {
        frameWidth = width;
        frameHeight = height;
        this.sensorOrientation = sensorOrientation;
    }

    public synchronized void drawDebug(final Canvas canvas) {
        final Paint textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(60.0f);

        final Paint boxPaint = new Paint();
        boxPaint.setColor(Color.RED);
        boxPaint.setAlpha(200);
        boxPaint.setStyle(Style.STROKE);

        for (final Pair<Float, RectF> detection : screenRects) {
            final RectF rect = detection.second;
            canvas.drawRect(rect, boxPaint);
            canvas.drawText("" + detection.first, rect.left, rect.top, textPaint);
            borderedText.drawText(canvas, rect.centerX(), rect.centerY(), "" + detection.first);
        }
    }

    public synchronized void trackResults(final List<Recognition> results, final long timestamp) {
        logger.i("Processing %d results from %d", results.size(), timestamp);
        processResults(results);
    }

    private Matrix getFrameToCanvasMatrix() {
        return frameToCanvasMatrix;
    }

    /**
     * Draw bounding boxes, coordinattes and direction on screen for detected objects
     */
    public synchronized void draw(final Canvas canvas) {

        final boolean rotated = sensorOrientation % 180 == 90;
        final float multiplier =
                Math.min(
                        canvas.getHeight() / (float) (rotated ? frameWidth : frameHeight),
                        canvas.getWidth() / (float) (rotated ? frameHeight : frameWidth));
        frameToCanvasMatrix =
                ImageUtils.getTransformationMatrix(
                        frameWidth,
                        frameHeight,
                        (int) (multiplier * (rotated ? frameHeight : frameWidth)),
                        (int) (multiplier * (rotated ? frameWidth : frameHeight)),
                        sensorOrientation,
                        false);
        final Paint textPaint = new Paint();
        textPaint.setTextSize(60.0f);
        // Loop over recognised object
        for (final TrackedRecognition recognition : trackedObjects) {
            // if the recognised object is in objects list in our asset
            if (recognition.title.equals(this.label)) {
                final RectF trackedPos = new RectF(recognition.location);
                getFrameToCanvasMatrix().mapRect(trackedPos);
                boxPaint.setColor(recognition.color);

                float posX = 300;
                float posY = 1550;
                float cornerSize = Math.min(trackedPos.width(), trackedPos.height()) / 8.0f;
                canvas.drawRoundRect(trackedPos, cornerSize, cornerSize, boxPaint);
                float center_x, center_y, radius;
                radius = 100;

                final Paint CenterPaint = new Paint();

                CenterPaint.setARGB(1, 255, 255, 255);
                center_x = frameWidth;
                center_y = frameHeight;

                float screen_x = pixel_width / 2;
                float screen_y = pixel_height / 2;
                float obj_x = trackedPos.centerX();
                float obj_y = trackedPos.centerY();

                // Plot the center of object and screen
                canvas.drawCircle(screen_x, screen_y, radius / 10, boxPaint);
                canvas.drawCircle(obj_x, obj_y, radius / 100, boxPaint);
                borderedText.drawText(canvas, screen_x, screen_y, "Center of Screen", textPaint);
                borderedText.drawText(canvas, obj_x, obj_y, "Center of Object", textPaint);

                // to give directions to user based on the coordinates
                if ((obj_x < Xp && obj_x > Xm) && (obj_y < Yp && obj_y > Ym)) {
                    dir = "Straight";
                    borderedText.drawText(canvas, posX, posY, "Direction : Straight Ahead", textPaint);
                } else if ((obj_y > Yp)) {
                    dir = "tilt Down";
                    borderedText.drawText(canvas, posX, posY, "Direction : Tilt Down", textPaint);
                } else if ((obj_y < Ym)) {
                    dir = "tilt Up";
                    borderedText.drawText(canvas, posX, posY, "Direction : Tilt Up", textPaint);
                } else if ((obj_x > Xp)) {
                    dir = "Right";
                    borderedText.drawText(canvas, posX, posY, "Direction : Right", textPaint);
                } else {
                    dir = "Left";
                    borderedText.drawText(canvas, posX, posY, "Direction : Left", textPaint);
                }
                // voice output of directions
                this.speak(dir);

                String str = recognition.title;
                Log.i("RECOG TITLE ", str);
                final String labelString =
                        !TextUtils.isEmpty(recognition.title)
                                ? String.format("%s %.2f", recognition.title, (100 * recognition.detectionConfidence))
                                : String.format("%.2f", (100 * recognition.detectionConfidence));
            }
//            else {
//                final Paint error = new Paint();
//                error.setTextSize(60.0f);
//                error.setColor(Color.RED);
//
//                float posX = 300;
//                float posY = 1550;
//                this.speak("object not detected");
//                borderedText.drawText(canvas, posX, posY, "OBJECT NOT DETECTED", error);
//            }
        }
    }

    private void processResults(final List<Recognition> results) {
        final List<Pair<Float, Recognition>> rectsToTrack = new LinkedList<Pair<Float, Recognition>>();

        screenRects.clear();
        final Matrix rgbFrameToScreen = new Matrix(getFrameToCanvasMatrix());

        for (final Recognition result : results) {
            if (result.getLocation() == null) {
                continue;
            }
            final RectF detectionFrameRect = new RectF(result.getLocation());

            final RectF detectionScreenRect = new RectF();
            rgbFrameToScreen.mapRect(detectionScreenRect, detectionFrameRect);

            logger.v(
                    "Result! Frame: " + result.getLocation() + " mapped to screen:" + detectionScreenRect);

            screenRects.add(new Pair<Float, RectF>(result.getConfidence(), detectionScreenRect));

            if (detectionFrameRect.width() < MIN_SIZE || detectionFrameRect.height() < MIN_SIZE) {
                logger.w("Degenerate rectangle! " + detectionFrameRect);
                continue;
            }

            rectsToTrack.add(new Pair<Float, Recognition>(result.getConfidence(), result));
        }

        trackedObjects.clear();
        if (rectsToTrack.isEmpty()) {
            logger.v("Nothing to track, aborting.");
            return;
        }

        for (final Pair<Float, Recognition> potential : rectsToTrack) {
            final TrackedRecognition trackedRecognition = new TrackedRecognition();
            trackedRecognition.detectionConfidence = potential.first;
            trackedRecognition.location = new RectF(potential.second.getLocation());
            trackedRecognition.title = potential.second.getTitle();
            trackedRecognition.color = COLORS[trackedObjects.size()];
            trackedObjects.add(trackedRecognition);

            if (trackedObjects.size() >= COLORS.length) {
                break;
            }
        }
    }


    private static class TrackedRecognition {
        RectF location;
        float detectionConfidence;
        int color;
        String title;
    }
}
