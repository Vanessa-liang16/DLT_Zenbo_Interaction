package com.example.dlt_zenbo_interaction;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

import com.asus.robotframework.API.MotionControl;
import com.asus.robotframework.API.RobotAPI;
import com.asus.robotframework.API.RobotFace;
import com.asus.robotframework.API.WheelLights;
//2026
import com.asus.robotframework.API.RobotCallback;
import com.asus.robotframework.API.RobotErrorCode;
import com.asus.robotframework.API.RobotCmdState;
import com.asus.robotframework.API.results.DetectFaceResult;
import com.asus.robotframework.API.results.DetectPersonResult;
import com.asus.robotframework.API.results.FaceResult;
import com.asus.robotframework.API.results.GesturePointResult;
import com.asus.robotframework.API.results.TrackingResult;
import com.asus.robotframework.API.Utility;

import android.os.Handler;
import java.util.Random;


import java.util.List;


public class RobotDatabase {

    // singleton
    public static RobotDatabase instance;
    private String API_URL = "http://140.115.158.245:5173/api/";
    private RobotAPI robotAPI;
    private int petVoice = 0;
    private String studentID = "";

    //2026
    // 增加動作常數
    public static final int ACTION_HAPPY = 1;
    public static final int ACTION_DANCE = 2;
    public static final int ACTION_SHY = 3;
    public static final int ACTION_FOLLOW_OBJECT = 4;
    public static final int ACTION_STOP_FOLLOW = 5;
    private Context context;

    // 修改建構函數或 setter 方法來設置 Context
    public void setContext(Context context) {
        this.context = context;
    }
    private RobotDatabase() {
        // initialize
    }

    // get instance
    public static RobotDatabase getInstance() {
        if (instance == null) {
            instance = new RobotDatabase();
        }
        return instance;
    }

    public void setData(RobotAPI robotAPI, int petVoice, String studentID) {
        // set data
        this.robotAPI = robotAPI;
        this.petVoice = petVoice;
        this.studentID = studentID;
        // log
        Log.d("RobotDatabase", "setData: " + this.studentID + " " + this.petVoice + " " + this.robotAPI);
    }

    public RobotAPI getRobotAPI() {
        // get robotAPI
        return this.robotAPI;
    }

    public int getPetVoice() {
        // get petVoice
        return this.petVoice;
    }

    public String getStudentID() {
        // get studentID
        return this.studentID;
    }

    //2026
    // 新增 WebAppInterface 類別
    public class WebAppInterface {
        @JavascriptInterface
        public void triggerZenboAction(String action) {
            RobotDatabase db = RobotDatabase.getInstance();
            switch(action) {
                case "happy":
                    db.performZenboAction(RobotDatabase.ACTION_HAPPY);
                    break;
                case "dance":
                    db.performZenboAction(RobotDatabase.ACTION_DANCE);
                    break;
                case "shy":
                    db.performZenboAction(RobotDatabase.ACTION_SHY);
                    break;
                case "followObject":
                    db.performZenboAction(RobotDatabase.ACTION_FOLLOW_OBJECT);
                    break;
                case "stopFollow":
                    db.performZenboAction(RobotDatabase.ACTION_STOP_FOLLOW);
                    break;
            }
        }
    }

    public void performZenboAction(int actionType) {
        if (robotAPI != null) {
            switch (actionType) {
                case ACTION_HAPPY:
                    try {
                        // 直接原地旋轉
                        robotAPI.motion.moveBody(
                                0f,       // x軸移動距離
                                0f,       // y軸移動距離
                                360,      // 旋轉角度(度)
                                MotionControl.SpeedLevel.Body.L3  // 最快速度
                        );

                        Toast.makeText(context, "Happy spinning!", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Toast.makeText(context, "Error in happy action: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                    break;

                case ACTION_DANCE:
                    try {
                        // 保持原本的 dance 換顏色程式碼不變
                        int[] colors = {
                                0xFF0000,   // 红色
                                0x00FF00,   // 绿色
                                0x0000FF,   // 蓝色
                                0xFFFF00,   // 黄色
                                0xFF00FF    // 紫色
                        };

                        // 创建一个handler来实现颜色快速变换
                        Handler handler = new Handler();
                        final int[] currentIndex = {0};

                        Runnable colorChangeRunnable = new Runnable() {
                            @Override
                            public void run() {
                                if (currentIndex[0] < colors.length) {
                                    robotAPI.wheelLights.setColor(
                                            WheelLights.Lights.SYNC_BOTH,
                                            0xFF,  // 激活所有LED
                                            colors[currentIndex[0]]
                                    );
                                    currentIndex[0]++;

                                    // 每200毫秒变一次颜色
                                    handler.postDelayed(this, 200);
                                } else {
                                    // 7秒后关闭
                                    handler.postDelayed(() -> {
                                        robotAPI.wheelLights.turnOff(WheelLights.Lights.SYNC_BOTH, 0xFF);
                                    }, 7000);
                                }
                            }
                        };

                        // 开始变换
                        colorChangeRunnable.run();

                        Toast.makeText(context, "Dance action with wheel lights initiated", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Toast.makeText(context, "Error in dance action: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                    break;
                case ACTION_SHY:
                    try {
                        final int[] shyCount = {0};

                        Runnable shyAction = new Runnable() {
                            @Override
                            public void run() {
                                if (shyCount[0] < 2) {  // 重複2次
                                    // 低頭，幅度更大，速度更快
                                    robotAPI.motion.moveHead(0, -20, MotionControl.SpeedLevel.Head.L3);  // 增加幅度到-20，速度到最快

                                    // 延遲後抬頭
                                    new Handler().postDelayed(() -> {
                                        // 快速回正
                                        robotAPI.motion.moveHead(0, 0, MotionControl.SpeedLevel.Head.L3);

                                        // 計數器增加
                                        shyCount[0]++;

                                        // 如果還沒完成2次，繼續
                                        if (shyCount[0] < 2) {
                                            new Handler().postDelayed(this, 500);  // 縮短間隔到0.5秒
                                        } else {
                                            // 完全回正
                                            robotAPI.motion.moveHead(0, 0, MotionControl.SpeedLevel.Head.L3);
                                        }
                                    }, 500);  // 縮短延遲時間
                                }
                            }
                        };

                        // 開始執行
                        shyAction.run();

                        Toast.makeText(context, "Feeling shy!", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Toast.makeText(context, "Error in shy action: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                    break;
                case ACTION_FOLLOW_OBJECT:
                    try {
                        // 首先讓機器人看向使用者（方向預設為0）
                        robotAPI.utility.lookAtUser(0);

                        // 延遲後開始跟隨
                        new Handler().postDelayed(() -> {
                            robotAPI.utility.followUser();

                            // 10秒後停止
                            new Handler().postDelayed(() -> {
                                robotAPI.motion.stopMoving();  // 停止移動
                            }, 10000);
                        }, 1000);  // 1秒延遲，確保先看向使用者

                        Toast.makeText(context, "Looking at and following user!", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Toast.makeText(context, "Error in follow user action: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                    break;
                case ACTION_STOP_FOLLOW:
                    try {
                        // 停止移動
                        robotAPI.motion.stopMoving();

                        // 重置頭部位置
                        robotAPI.motion.moveHead(0, 0, MotionControl.SpeedLevel.Head.L3);

                        Toast.makeText(context, "Stopped following!", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Toast.makeText(context, "Error stopping follow: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                    break;
            }
        } else {
            Toast.makeText(context, "RobotAPI is null", Toast.LENGTH_LONG).show();
        }
    }
}
