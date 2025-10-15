package com.hbrs;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.content.Intent;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.hbrs.Bluetooth.BT_DeviceListActivity;
import com.hbrs.ORB.ORB;

public class MainActivity extends AppCompatActivity {
    ORB orb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {


        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        orb = new ORB( this );
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

    }
    public void onClickConnect(View view){
        Log.i("Test","button connect clicked");
        BT_DeviceListActivity.start(this, 50);
    }

 public void fahren(int li, int re){
     orb.setMotor( ORB.M1, ORB.SPEED_MODE, -li, 0);
     orb.setMotor( ORB.M4, ORB.SPEED_MODE, re, 0);
 }
    //M1 +500 backward
    //M4 -500 backwards
    public void onClickRunForward(View view){
        Log.i("Test","button Run clicked");
        fahren(+500,+500);




    }
    public void onClickRunLinks(View view){
        Log.i("Test","button Run clicked");
        fahren(-500,+500);
    }
    public void OnclickRunRechts(View view){
        Log.i("Test","button Run clicked");
        fahren(500,-500);


    }
    //M1 +500 forward fahren
    //M4 -500 backward fahren
    //M4 +500 forward fahren
    public void onClickRunBack(View view){
        Log.i("Test","button Run clicked");
        fahren(-500,-500);
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