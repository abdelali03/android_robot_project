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


        private final Mat kernel = Imgproc.getStructuringElement(
                Imgproc.MORPH_ELLIPSE,
                new org.opencv.core.Size(5, 5)
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

                int h = mat.height();
                int w = mat.width();

                int roiStart_X = (int)(w * 0.65);
                int roiWidth   = (int)(w * 0.35);

                Rect roiRect = new Rect(roiStart_X, 0, roiWidth, h);
                Mat roi = hsv.submat(roiRect);


                int total_height = h;
                int qtr = total_height / 4;

                int left_box_height  = qtr;
                int mid_box_height   = 2 * qtr;
                int right_box_height = qtr;


                Rect leftBox  = new Rect(0, 0, roi.cols(), left_box_height);

                Rect midBox   = new Rect(0, qtr, roi.cols(), mid_box_height);

                Rect rightBox = new Rect(0, qtr + mid_box_height, roi.cols(), right_box_height);

                Mat L = roi.submat(leftBox);
                Mat M = roi.submat(midBox);
                Mat R = roi.submat(rightBox);


                Scalar lowBlack  = new Scalar(0, 0, 0);
                Scalar highBlack = new Scalar(180, 255, 60);

                Mat maskL = new Mat();
                Mat maskM = new Mat();
                Mat maskR = new Mat();

                Core.inRange(L, lowBlack, highBlack, maskL);
                Core.inRange(M, lowBlack, highBlack, maskM);
                Core.inRange(R, lowBlack, highBlack, maskR);

                Imgproc.morphologyEx(maskL, maskL, Imgproc.MORPH_OPEN, kernel);
                Imgproc.morphologyEx(maskM, maskM, Imgproc.MORPH_OPEN, kernel);
                Imgproc.morphologyEx(maskR, maskR, Imgproc.MORPH_OPEN, kernel);

                int blackL = Core.countNonZero(maskL);
                int blackM = Core.countNonZero(maskM);
                int blackR = Core.countNonZero(maskR);

                int threshold = 400;

                boolean Ld = blackL > threshold;
                boolean Md = blackM > threshold;
                boolean Rd = blackR > threshold;


                Log.i("LINE_BOXES",
                        "L=" + blackL + "  M=" + blackM + "  R=" + blackR +
                                " | Det L=" + Ld + " M=" + Md + " R=" + Rd);


                Imgproc.rectangle(mat,
                        new Rect(roiStart_X, 0, roiWidth, left_box_height),
                        Ld ? new Scalar(0,255,0) : new Scalar(255,0,0), 3);


                Imgproc.rectangle(mat,
                        new Rect(roiStart_X, qtr, roiWidth, mid_box_height),
                        Md ? new Scalar(0,255,0) : new Scalar(255,0,0), 3);


                Imgproc.rectangle(mat,
                        new Rect(roiStart_X, qtr + mid_box_height, roiWidth, right_box_height),
                        Rd ? new Scalar(0,255,0) : new Scalar(255,0,0), 3);


                controlRobot(Ld, Md, Rd);



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


            int turnIntensity = 250;

            if (M && !L && !R) {

                Log.i("ROBOT", "GERADEAUS");
                ORBManager.speed(speed, speed);
            }
            else if (L && !M && !R) {

                Log.i("ROBOT", "KORREKTUR RECHTS (L erkannt)");
                ORBManager.speed(speed + turnIntensity, speed - turnIntensity);
            }
            else if (R && !M && !R) {

                Log.i("ROBOT", "KORREKTUR LINKS (R erkannt)");
                ORBManager.speed(speed - turnIntensity, speed + turnIntensity);
            }
            else if (L && M) {

                Log.i("ROBOT", "SANFT RECHTS");
                ORBManager.speed(speed + (turnIntensity / 2), speed - (turnIntensity / 2));
            }
            else if (R && M) {

                Log.i("ROBOT", "SANFT LINKS");
                ORBManager.speed(speed - (turnIntensity / 2), speed + (turnIntensity / 2));
            }
            else if (!L && !M && !R) {
                Log.w("ROBOT", "STOPP");
                ORBManager.speed(0, 0);
            }
            else {

                if (L) {

                    ORBManager.speed(speed + 400, speed - 400);
                } else {

                    ORBManager.speed(speed - 400, speed + 400);
                }
            }
        }
    }






    public void onClickBack(View v) {
        finish();
    }
}
