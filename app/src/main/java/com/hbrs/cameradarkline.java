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

public class cameradarkline extends AppCompatActivity {

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

    // ===================== LINE-FOLLOWING ANALYZER =====================
    private static class Analyzer implements ImageAnalysis.Analyzer {

        private final ImageView target;
        private Bitmap bitmap;

        private final Mat mat = new Mat();
        private final Mat hsv = new Mat();

        // Kernel für Morphology (Rauschen entfernen)
        private final Mat kernel = Imgproc.getStructuringElement(
                Imgproc.MORPH_ELLIPSE,
                new org.opencv.core.Size(5, 5)
        );

        Analyzer(ImageView v) { this.target = v; }

        @Override
        public void analyze(@NonNull ImageProxy image) {

            try {
                Bitmap bm = image.toBitmap();
                // FIX: CameraX liefert rotated Bild → wir korrigieren das


                if (bm == null) { image.close(); return; }

                if (bitmap == null ||
                        bitmap.getWidth() != bm.getWidth() ||
                        bitmap.getHeight() != bm.getHeight()) {

                    bitmap = Bitmap.createBitmap(
                            bm.getWidth(), bm.getHeight(),
                            Bitmap.Config.ARGB_8888
                    );
                }

                // Convert to Mat
                Utils.bitmapToMat(bm, mat);
                Imgproc.cvtColor(mat, hsv, Imgproc.COLOR_BGR2HSV);

                int h = mat.height(); // Höhe des unrotierten Mat (wird zur Anzeige-Breite)
                int w = mat.width();  // Breite des unrotierten Mat (wird zur Anzeige-Höhe)

                // --- 1. Region of Interest (ROI) Berechnung ---
                // Boxen am unteren Rand der ANZEIGE (entspricht der RECHTEN KANTE des unrotierten Mat)
                int roiStart_X = (int)(w * 0.65); // Starte bei 65% der Breite (X-Achse)
                int roiWidth   = (int)(w * 0.35); // Dicke der Boxen (35% der Breite/X-Achse)

                Rect roiRect = new Rect(roiStart_X, 0, roiWidth, h);
                Mat roi = hsv.submat(roiRect);

                // --- 2. Drei Boxen: MITTELBOX BREITER (25% / 50% / 25%) ---
                // Die Boxen teilen die HÖHE (h) des unrotierten Mat.
                int total_height = h;
                int qtr = total_height / 4; // Ein Viertel der Gesamthöhe (h)

                int left_box_height  = qtr;
                int mid_box_height   = 2 * qtr; // Doppelte Höhe = 50%
                int right_box_height = qtr;

                // Definition der Boxen entlang der Y-Achse (0 bis h)
                // Linke Box (erste 25%)
                Rect leftBox  = new Rect(0, 0, roi.cols(), left_box_height);
                // Mittlere Box (von 25% bis 75%)
                Rect midBox   = new Rect(0, qtr, roi.cols(), mid_box_height);
                // Rechte Box (von 75% bis 100%)
                Rect rightBox = new Rect(0, qtr + mid_box_height, roi.cols(), right_box_height);

                Mat L = roi.submat(leftBox);
                Mat M = roi.submat(midBox);
                Mat R = roi.submat(rightBox);

                // --- 3. Black threshold ---
                Scalar lowBlack  = new Scalar(0, 0, 0);
                Scalar highBlack = new Scalar(180, 255, 60);

                Mat maskL = new Mat();
                Mat maskM = new Mat();
                Mat maskR = new Mat();

                Core.inRange(L, lowBlack, highBlack, maskL);
                Core.inRange(M, lowBlack, highBlack, maskM);
                Core.inRange(R, lowBlack, highBlack, maskR);

                // Rauschen entfernen
                Imgproc.morphologyEx(maskL, maskL, Imgproc.MORPH_OPEN, kernel);
                Imgproc.morphologyEx(maskM, maskM, Imgproc.MORPH_OPEN, kernel);
                Imgproc.morphologyEx(maskR, maskR, Imgproc.MORPH_OPEN, kernel);

                int blackL = Core.countNonZero(maskL);
                int blackM = Core.countNonZero(maskM);
                int blackR = Core.countNonZero(maskR);

                int threshold = 400;  // erkennen ab ~400 Pixel

                boolean Ld = blackL > threshold;
                boolean Md = blackM > threshold;
                boolean Rd = blackR > threshold;

                // Debug log
                Log.i("LINE_BOXES",
                        "L=" + blackL + "  M=" + blackM + "  R=" + blackR +
                                " | Det L=" + Ld + " M=" + Md + " R=" + Rd);

                // --- 4. KORRIGIERTE DRAW visualization ---
                // Die Höhe der Rechtecke entspricht der neuen Aufteilung (qtr, 2*qtr)

                // Linke Box (Y-Position: 0)
                Imgproc.rectangle(mat,
                        new Rect(roiStart_X, 0, roiWidth, left_box_height),
                        Ld ? new Scalar(0,255,0) : new Scalar(255,0,0), 3);

                // Mittlere Box (Y-Position: qtr)
                Imgproc.rectangle(mat,
                        new Rect(roiStart_X, qtr, roiWidth, mid_box_height),
                        Md ? new Scalar(0,255,0) : new Scalar(255,0,0), 3);

                // Rechte Box (Y-Position: qtr + mid_box_height)
                Imgproc.rectangle(mat,
                        new Rect(roiStart_X, qtr + mid_box_height, roiWidth, right_box_height),
                        Rd ? new Scalar(0,255,0) : new Scalar(255,0,0), 3);

                // ---- ROBOT CONTROL ----
                controlRobot(Ld, Md, Rd);


                // Show result
                Utils.matToBitmap(mat, bitmap);
                target.post(() -> target.setImageBitmap(bitmap));

            } catch (Exception e) {
                Log.e("Analyzer", "error", e);
            } finally {
                image.close();
            }
        }

        private void controlRobot(boolean L, boolean M, boolean R) {
            int speed = ORBManager.getCurrentSpeed();

            // Verringere diesen Wert (z.B. auf 150), falls er immer noch zu nervös ist.
            int turnIntensity = 250;

            if (M && !L && !R) {
                // Auf der Linie -> Geradeaus
                Log.i("ROBOT", "GERADEAUS");
                ORBManager.speed(speed, speed);
            }
            else if (L && !M && !R) {
                // Linie links erkannt -> Lenke nach RECHTS (Umgedreht)
                Log.i("ROBOT", "KORREKTUR RECHTS (L erkannt)");
                ORBManager.speed(speed + turnIntensity, speed - turnIntensity);
            }
            else if (R && !M && !R) {
                // Linie rechts erkannt -> Lenke nach LINKS (Umgedreht)
                Log.i("ROBOT", "KORREKTUR LINKS (R erkannt)");
                ORBManager.speed(speed - turnIntensity, speed + turnIntensity);
            }
            else if (L && M) {
                // Linie zwischen Mitte und Links -> Sanft nach RECHTS (Umgedreht)
                Log.i("ROBOT", "SANFT RECHTS");
                ORBManager.speed(speed + (turnIntensity / 2), speed - (turnIntensity / 2));
            }
            else if (R && M) {
                // Linie zwischen Mitte und Rechts -> Sanft nach LINKS (Umgedreht)
                Log.i("ROBOT", "SANFT LINKS");
                ORBManager.speed(speed - (turnIntensity / 2), speed + (turnIntensity / 2));
            }
            else if (!L && !M && !R) {
                Log.w("ROBOT", "STOPP");
                ORBManager.speed(0, 0);
            }
            else {
                // Starkes Gegenlenken (Umgedreht)
                if (L) {
                    // L erkannt -> Stark Rechts
                    ORBManager.speed(speed + 400, speed - 400);
                } else {
                    // R erkannt -> Stark Links
                    ORBManager.speed(speed - 400, speed + 400);
                }
            }
        }
    }






    public void onClickBack(View v) {
        finish();
    }
}
