package com.hbrs;

import android.app.Activity;
import com.hbrs.ORB.ORB;

public class ORBManager {
    static int currentSpeed;
    private static ORB orb;

    public static void init(Activity activity) {
        if (orb == null) {
            orb = new ORB(activity);
            currentSpeed=500;
        }
    }

    public static ORB getORB() {
        return orb;
    }

    public static void speed(int links, int rechts) {
        orb.setMotor(ORB.M1, ORB.SPEED_MODE, -links, 0);
        orb.setMotor(ORB.M4, ORB.SPEED_MODE, rechts, 0);
    }
    public static int getCurrentSpeed(){
        return currentSpeed;
    }
    public static void setCurrentSpeed(int newcurrentSpeed){
        currentSpeed=newcurrentSpeed;
    }



}

