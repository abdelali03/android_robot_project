package com.hbrs;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraActivity extends AppCompatActivity {

    private static final int REQ = 101;
    private ImageView cameraOutput;
    private ExecutorService executor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        cameraOutput = findViewById(R.id.cameraOutput);

        if (!OpenCVLoader.initDebug()) {
            Toast.makeText(this, "OpenCV load failed!", Toast.LENGTH_LONG).show();
            return;
        }

        if (checkSelfPermission(Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQ);
        } else {
            startCamera();
        }
    }

    private void startCamera() {
        executor = Executors.newSingleThreadExecutor();
        ListenableFuture<ProcessCameraProvider> future =
                ProcessCameraProvider.getInstance(this);

        future.addListener(() -> {
            try {
                ProcessCameraProvider provider = future.get();

                ImageAnalysis analysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                analysis.setAnalyzer(executor, new Analyzer(cameraOutput));

                provider.unbindAll();
                provider.bindToLifecycle(
                        (LifecycleOwner) this,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        analysis
                );

            } catch (Exception e) {
                Log.e("Cam", "Error binding camera", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private static class Analyzer implements ImageAnalysis.Analyzer {

        private final ImageView target;
        private Bitmap bitmap;

        private final Mat mat = new Mat();
        private final Mat hsv = new Mat();
        private final Mat mask = new Mat();
        private final Mat hierarchy = new Mat();
        private final List<MatOfPoint> contours = new ArrayList<>();

        private final Mat kernel = Imgproc.getStructuringElement(
                Imgproc.MORPH_ELLIPSE,
                new org.opencv.core.Size(7, 7)
        );

        Analyzer(ImageView v) { this.target = v; }

        @Override
        public void analyze(@NonNull ImageProxy image) {

            try {
                Bitmap bm = image.toBitmap();
                if (bm == null) { image.close(); return; }

                if (bitmap == null ||
                        bitmap.getWidth() != bm.getWidth() ||
                        bitmap.getHeight() != bm.getHeight()) {

                    bitmap = Bitmap.createBitmap(
                            bm.getWidth(), bm.getHeight(),
                            Bitmap.Config.ARGB_8888
                    );
                }

                Utils.bitmapToMat(bm, mat);
                Imgproc.cvtColor(mat, hsv, Imgproc.COLOR_BGR2HSV);

                // BLUE RANGE

                Scalar lowBlue  = new Scalar(110, 180, 100);
                Scalar highBlue = new Scalar(130, 255, 255);

                Core.inRange(hsv, lowBlue, highBlue, mask);

                Imgproc.morphologyEx(mask, mask, Imgproc.MORPH_OPEN, kernel);
                Imgproc.morphologyEx(mask, mask, Imgproc.MORPH_CLOSE, kernel);

                contours.clear();
                Imgproc.findContours(mask, contours, hierarchy,
                        Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

                int imgArea = mat.width() * mat.height();
                int minArea = imgArea / 600;
                int maxArea = imgArea / 3;

                MatOfPoint best = null;
                double bestArea = 0;

                for (MatOfPoint cnt : contours) {
                    double area = Imgproc.contourArea(cnt);
                    if (area > minArea && area < maxArea && area > bestArea) {
                        bestArea = area;
                        best = cnt;
                    }
                }

                if (best != null) {

                    Rect r = Imgproc.boundingRect(best);
                    int cx = r.x + r.width / 2;
                    int cy = r.y + r.height / 2;

                    // draw
                    Imgproc.rectangle(mat, r.tl(), r.br(), new Scalar(0,255,0), 3);
                    Imgproc.circle(mat, new Point(cx, cy),
                            Math.min(r.width, r.height)/2,
                            new Scalar(255,0,0), 3);

                    // --- LOGGING ---
                    Log.i("CAMERA", "OBJECT DETECTED:");
                    Log.i("CAMERA", " - cx = " + cx);
                    Log.i("CAMERA", " - cy = " + cy);
                    Log.i("CAMERA", " - width = " + r.width);
                    Log.i("CAMERA", " - area = " + bestArea);

                    // robot control
                    controlRobot(cx, r.width);
                } else {
                    Log.i("CAMERA", "NO OBJECT FOUND");
                    ORBManager.speed(0, 0);  // optional
                }

                Utils.matToBitmap(mat, bitmap);
                target.post(() -> target.setImageBitmap(bitmap));

            } catch (Exception e) {
                Log.e("Analyzer", "error", e);
            } finally {
                image.close();
            }
        }

        private void controlRobot(int cx, int objectWidth) {

            int center = mat.width() / 2;
            int threshold = mat.width() / 10;

            // ==== LOGGING ====
            Log.i("ROBOT", "CONTROL INPUT:");
            Log.i("ROBOT", " - cx = " + cx);
            Log.i("ROBOT", " - center = " + center);
            Log.i("ROBOT", " - threshold = " + threshold);
            Log.i("ROBOT", " - objectWidth = " + objectWidth);

            if (objectWidth > mat.width() / 3) {
                Log.w("ROBOT", "ACTION: STOP (object too close)");
                ORBManager.speed(0, 0);
                return;
            }

            if (cx < center - threshold) {
                Log.w("ROBOT", "ACTION: TURN LEFT");
                ORBManager.speed(-ORBManager.getCurrentSpeed(), ORBManager.getCurrentSpeed());
            }
            else if (cx > center + threshold) {
                Log.w("ROBOT", "ACTION: TURN RIGHT");
                ORBManager.speed(ORBManager.getCurrentSpeed(), -ORBManager.getCurrentSpeed());
            }
            else {
                Log.w("ROBOT", "ACTION: FORWARD");
                ORBManager.speed(ORBManager.getCurrentSpeed(), ORBManager.getCurrentSpeed());
            }
        }
    }





    public void onClickBack(View v) {
        finish();
    }
}
