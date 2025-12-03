package com.hbrs;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
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

public class MainActivity extends AppCompatActivity {


    TextView text1;
    TextView text2;
    ImageView joystickKnob;
    int currentSpeed = 2500; // startspeed entspricht der Mitte des Balkens

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        ORBManager.init(this);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mytextview), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        //Display-Textfelder ansprechen
        text1 = findViewById(R.id.infotext);
        text1.setText("Hello world");
        text2 = findViewById(R.id.infotext2);
        text2.setText("Speed 2500");

        //Joystick Setup
        View joystickContainer = findViewById(R.id.joystick_container);
        joystickKnob = findViewById(R.id.joystick_knob);
        joystickContainer.setOnTouchListener(new myOnTouchListener());

        //Speed Seekbar Code-------------------------
        SeekBar speedSeekBar = findViewById(R.id.speed_seekbar);
        speedSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int range = 2000;
                currentSpeed = 1500 + (int) (((float) progress / seekBar.getMax()) * range);
                text2.setText("Speed: " + currentSpeed);
                Log.i("Test", "Speedbar moved");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }



    // === Navigation button handler ===
    public void onClickOpenCamera(View view) {
        Intent intent = new Intent(this, cameradarkline.class);
        startActivity(intent);
    }
    public void onClickOpenCameraDark(View view) {

        Intent intent = new Intent(this, CameraActivity.class);
        startActivity(intent);
    }

    // === Bluetooth and movement ===
    public void onClickConnect(View view) {
        text1.setText("Button Connect clicked");
        Log.i("Test", "Button Connect clicked");
        BT_DeviceListActivity.start(this, 50);

    }

    public void onClickForward(View view) {
        text1.setText("Button Forward clicked");
        ORBManager.speed(currentSpeed, currentSpeed);
    }

    public void onClickLeft(View view) {
        text1.setText("Button Left clicked");
        ORBManager.speed(-currentSpeed, currentSpeed);
    }

    public void onClickRight(View view) {
        text1.setText("Button Right clicked");
        ORBManager.speed(currentSpeed, -currentSpeed);
    }

    public void onClickBackward(View view) {
        text1.setText("Button Backward clicked");
        ORBManager.speed(-currentSpeed, -currentSpeed);
    }

    class myOnTouchListener implements View.OnTouchListener {
        @Override
        public boolean onTouch(View v, MotionEvent e) {
            float x = (e.getX() / v.getWidth()) * 2 - 1;
            float y = (e.getY() / v.getHeight()) * 2 - 1;
            switch (e.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    text1.setText("Joystick clicked");
                    break;
                case MotionEvent.ACTION_MOVE:
                    float containerWidth = v.getWidth();
                    float containerHeight = v.getHeight();
                    float knobX = e.getX() - (joystickKnob.getWidth() / 2f);
                    float knobY = e.getY() - (joystickKnob.getHeight() / 2f);
                    knobX = Math.max(0, Math.min(containerWidth - joystickKnob.getWidth(), knobX));
                    knobY = Math.max(0, Math.min(containerHeight - joystickKnob.getHeight(), knobY));
                    joystickKnob.setX(knobX);
                    joystickKnob.setY(knobY);

                    float forward = -y * currentSpeed;
                    float turn = x * currentSpeed;
                    int leftSpeed = (int) (forward + turn);
                    int rightSpeed = (int) (forward - turn);
                    leftSpeed = Math.max(-currentSpeed, Math.min(currentSpeed, leftSpeed));
                    rightSpeed = Math.max(-currentSpeed, Math.min(currentSpeed, rightSpeed));
                    ORBManager.speed(leftSpeed, rightSpeed);

                    text1.setText("Links: " + leftSpeed + " Rechts: " + rightSpeed);
                    break;
                case MotionEvent.ACTION_UP:
                    float centerX = v.getWidth() / 2f - joystickKnob.getWidth() / 2f;
                    float centerY = v.getHeight() / 2f - joystickKnob.getHeight() / 2f;
                    joystickKnob.setX(centerX);
                    joystickKnob.setY(centerY);
                    ORBManager.speed(0,0);
                    text1.setText("Joystick released");
                    break;
                default:
                    return false;
            }
            return true;
        }
    }

    public void speed(int links, int rechts) {
        ORBManager.getORB().setMotor(ORB.M1, ORB.SPEED_MODE, -links, 0);
        ORBManager.getORB().setMotor(ORB.M4, ORB.SPEED_MODE, rechts, 0);
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
                        ORBManager.getORB().openBT( BT_DeviceListActivity.getDeviceFromIntent(data));
                        ORBManager.getORB().configMotor(ORB.M1, 144, 50, 50, 30);
                        ORBManager.getORB().configMotor(ORB.M4, 144, 50, 50, 30);


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
    public void onDestroy() {

        super.onDestroy();
    }
}
