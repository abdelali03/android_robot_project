package com.hbrs;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.hbrs.Bluetooth.BT_DeviceListActivity;
import com.hbrs.ORB.ORB;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;


public class MainActivity extends AppCompatActivity {
    ORB orb;
    TextView text1;
    TextView text2;
    ImageView joystickKnob;
    int currentSpeed = 2500; //startspeed entspricht der Mitte des Balkens




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        orb = new ORB( this );
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mytextview), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        // === Kamera-Permission prüfen ===
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.CAMERA}, 101);
        } else {
            // === CameraX Setup ===
            ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                    ProcessCameraProvider.getInstance(this);

            cameraProviderFuture.addListener(new Runnable() {
                @Override
                public void run() {
                    try {
                        ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                        MainActivity.this.bindPreview(cameraProvider);
                    } catch (ExecutionException | InterruptedException e) {
                        Log.e("CameraX", "Error starting camera", e);
                    }
                }
            }, ContextCompat.getMainExecutor(this));
        }

        //Display-Textfelder ansprechen
        text1 = findViewById(R.id.infotext);
        text1.setText("Hello world");
        text2 = findViewById(R.id.infotext2);
        text2.setText("Speed 2500");


        // Button finden (in dem Fall selbst programmiertes View "selbstprogrammierte Darstellung"
        View joystickContainer = findViewById(R.id.joystick_container);
        joystickKnob = findViewById(R.id.joystick_knob);
        //Button mit Listener-Klasse verbinden
        joystickContainer.setOnTouchListener(new myOnTouchListener());


        //Speed Seekbar Code-------------------------
        SeekBar speedSeekBar = findViewById(R.id.speed_seekbar);

        speedSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // neuen Wertebereich berechnen(3500 - 1500 = 2000)
                int range = 2000;

                // Wert abhängig von der Reglerposition berechnen
                // (progress / seekBar.getMax()) gibt einen Wert zwischen 0.0 und 1.0 diesen * range + 1500 start ergibt somit korrekten speed für den motor
                currentSpeed = 1500 + (int) (((float) progress / seekBar.getMax()) * range);
                text2.setText("Speed: " + currentSpeed);
                Log.i("Test","Button Speedbar moved");

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // beim drücken der Speedbar
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // beim loslassen der Speedbar
            }
        });


    }
    void bindPreview(ProcessCameraProvider cameraProvider) {
        // Kamera auswählen (hinten)
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        // Preview Use Case
        Preview preview = new Preview.Builder().build();
        PreviewView previewView = findViewById(R.id.previewView);
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        try {
            // Alte Use Cases unbinden
            cameraProvider.unbindAll();

            // Neue Use Cases binden (hier nur Preview)
            Camera camera = cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview
            );
        } catch (Exception e) {
            Log.e("CameraX", "Binding failed", e);
        }
    }

    public void onClickConnect(View view){
        text1.setText("Button Connect clicked");
        Log.i("Test","Button Connect clicked");
        BT_DeviceListActivity.start(this, 50);
    }
    public void onClickForward(View view){
        text1.setText("Button Forward clicked");
        Log.i("Test", "Button Forward clicked");
        speed(currentSpeed,currentSpeed);


    }
    public void onClickLeft(View view){
        text1.setText("Button Left clicked");
        Log.i("Test", "Button Left clicked");
        speed(-currentSpeed,currentSpeed);

    }
    public void onClickRight(View view){
        text1.setText("Button Right clicked");
        Log.i("Test", "Button Right clicked");
        speed(currentSpeed,-currentSpeed);


    }
    public void onClickBackward(View view){
        text1.setText("Button Backward clicked");
        Log.i("Test", "Button Backward clicked");
        speed(-currentSpeed,-currentSpeed);
    }

    class myOnTouchListener implements View.OnTouchListener
    {
        @Override
        public boolean onTouch(View v, MotionEvent e) {
            //Postion des Bewegten Buttons relativ zu seiner Mitte berechnen
            // Y: -1.0 (oben / vorne) bis +1.0 (unten / hinten)
            // X: -1.0 (links) bis +1.0 (rechts)
            //intern ist nähmlich link:0.0 mitte:0.5 rechts:1.0, Umrechnung so das die Mitte bei 0.0 ist und von -1 bis 1 geht
            float x = (e.getX() / v.getWidth()) * 2 - 1;
            float y = (e.getY() / v.getHeight()) * 2 - 1;
            switch(e.getAction())//Fallunterscheidung wie man den Button drückt
            {
                case MotionEvent.ACTION_DOWN:
                    text1.setText("Joystick clicked");
                    Log.i("Test", "Joystick clicked");
                    break;
                case MotionEvent.ACTION_MOVE:
                    //------nur Visuell: Bewegung des roten Punktes
                    float containerWidth = v.getWidth();
                    float containerHeight = v.getHeight();

                    // Berechne die neue Position des Knopfs, zentriert unter dem Finger
                    float knobX = e.getX() - (joystickKnob.getWidth() / 2f);
                    float knobY = e.getY() - (joystickKnob.getHeight() / 2f);

                    // Verhindere, dass der Knopf den Container verlässt
                    knobX = Math.max(0, Math.min(containerWidth - joystickKnob.getWidth(), knobX));
                    knobY = Math.max(0, Math.min(containerHeight - joystickKnob.getHeight(), knobY));

                    // Setze die neue Position des visuellen Knopfs
                    joystickKnob.setX(knobX);
                    joystickKnob.setY(knobY);
                    //-----bis hier nur visuell roter knopf
                    //Ab hier Bewegung des Roboters
                    float forward = -y * currentSpeed;
                    float turn = x * currentSpeed;
                    int leftSpeed = (int) (forward + turn);
                    int rightSpeed = (int) (forward - turn);
                    //cap Speed auf Maximalwert
                    leftSpeed = Math.max(-currentSpeed, Math.min(currentSpeed, leftSpeed));
                    rightSpeed = Math.max(-currentSpeed, Math.min(currentSpeed, rightSpeed));
                    //Bewegung an Roboter zum ausführen senden
                    speed(leftSpeed, rightSpeed);

                    //Werte am Display ausgeben
                    text1.setText("Links: " + leftSpeed + " Rechts: " + rightSpeed);
                    Log.i("Test", "Joystick moved");
                    break;
                case MotionEvent.ACTION_UP:
                    //Roten Knopf in die Mitte zurückspringen lassen
                    float centerX = v.getWidth() / 2f - joystickKnob.getWidth() / 2f;
                    float centerY = v.getHeight() / 2f - joystickKnob.getHeight() / 2f;
                    joystickKnob.setX(centerX);
                    joystickKnob.setY(centerY);
                    //Motor stoppen bei loslassen des Joysticks
                    speed(0,0);
                    text1.setText("Joystick released");
                    Log.i("Test", "Joystick released");
                    break;
                default:
                    return false;
            }
            return true;
        }
    };


    public void speed(int links, int rechts){
        orb.setMotor( ORB.M1, ORB.SPEED_MODE, -links, 0);
        orb.setMotor( ORB.M4, ORB.SPEED_MODE, rechts, 0);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode,resultCode,data);
        switch(requestCode)
        {
            case 50:
                switch(resultCode)
                {
                    case BT_DeviceListActivity.RESULT_OK:
                        Log.i("Test",BT_DeviceListActivity.getDeviceFromIntent(data).toString());
                        orb.openBT( BT_DeviceListActivity.getDeviceFromIntent(data));
                        orb.configMotor(ORB.M1, 144, 50, 50, 30);
                        orb.configMotor(ORB.M4, 144, 50, 50, 30);


                        break;
                    case BT_DeviceListActivity.RESULT_CANCELED:
                        Log.i("Test","canceled");
                        break;
                    default:
                        Log.i("Test", "Enable permissons first");
                        break;
                }
                break;
        }
    }
    @Override
    public void onDestroy()
    {
        orb.close();
        super.onDestroy();
    }


}