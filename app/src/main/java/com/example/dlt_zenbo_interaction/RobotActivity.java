package com.example.dlt_zenbo_interaction;

import android.app.Activity;
import android.os.Bundle;

import com.asus.robotframework.API.RobotAPI;
import com.asus.robotframework.API.RobotCallback;
import com.asus.robotframework.API.RobotCmdState;
import com.asus.robotframework.API.RobotErrorCode;
import com.asus.robotframework.API.RobotFace;

import org.json.JSONObject;


public class RobotActivity extends Activity {
    public RobotAPI robotAPI;
    RobotCallback robotCallback;
    RobotCallback.Listen robotListenCallback;
    protected static int iCurrentCommandSerial;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.robotAPI = new RobotAPI(getApplicationContext(), robotCallback);
    }

    public RobotActivity() {
        this.robotCallback = new RobotCallback() {
            @Override
            public void onResult(int cmd, int serial, RobotErrorCode err_code, Bundle result) {
                super.onResult(cmd, serial, err_code, result);
            }

            @Override
            public void onStateChange(int cmd, int serial, RobotErrorCode err_code, RobotCmdState state) {
                super.onStateChange(cmd, serial, err_code, state);
                if (serial == iCurrentCommandSerial && state != RobotCmdState.ACTIVE) {
                    try {
                        Thread.sleep(1000); //1000為1秒
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    robotAPI.robot.setExpression(RobotFace.DEFAULT);
                }
            }

            @Override
            public void initComplete() {
                super.initComplete();
            }
        };
        this.robotListenCallback = new RobotCallback.Listen() {
            @Override
            public void onFinishRegister() {
            }

            @Override
            public void onVoiceDetect(JSONObject jsonObject) {
            }

            @Override
            public void onSpeakComplete(String s, String s1) {
            }

            @Override
            public void onEventUserUtterance(JSONObject jsonObject) {
            }

            @Override
            public void onResult(JSONObject jsonObject) {
            }

            @Override
            public void onRetry(JSONObject jsonObject) {
            }
        };
    }

    @Override
    protected void onPause() {
        super.onPause();
        robotAPI.robot.unregisterListenCallback();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (robotListenCallback != null)
            robotAPI.robot.registerListenCallback(robotListenCallback);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        robotAPI.release();
    }
}
