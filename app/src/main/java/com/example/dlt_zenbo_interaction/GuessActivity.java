package com.example.dlt_zenbo_interaction;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import com.asus.robotframework.API.RobotAPI;
import com.asus.robotframework.API.RobotFace;
import com.microsoft.cognitiveservices.speech.ResultReason;
import com.microsoft.cognitiveservices.speech.SpeechConfig;
import com.microsoft.cognitiveservices.speech.SpeechRecognizer;
import com.microsoft.cognitiveservices.speech.audio.AudioConfig;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.HashSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class GuessActivity extends RobotActivity {

    private String stuID = "";
    private int petVoice = 0;
    private String answer = "";
    private int guessID = 0;
    private String riddle = "";

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("endGuess")) {
                finish();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.scriptempty);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // 取得從上一個Activity傳來的Bundle物件
        Bundle bundle = this.getIntent().getExtras();
        // 取得Bundle物件中的字串
        this.stuID = bundle.getString("stuID");

        // register broadcast receiver
        IntentFilter filter = new IntentFilter("endGuess");
        registerReceiver(receiver, filter);

        // Login Task
        Log.d("stuID", stuID);
        ApiTask loginTask = new ApiTask(stuID, "Login");
        loginTask.execute();

        // LoginTask執行完後
        try {
            String response = loginTask.get();
            JSONObject petJson = new JSONObject(response);
            petVoice = petJson.getInt("voice");

            // Welcome Task
            ApiTask welcomeTask = new ApiTask(stuID, "Guessing");
            welcomeTask.execute();
            response = welcomeTask.get();

            // completion 為JSON格式的字串
            JSONObject completionJson = new JSONObject(response);
            riddle = completionJson.getString("riddle");
            answer = completionJson.getString("answer");
            guessID = completionJson.getInt("guessID");
            String expression = completionJson.getString("expression");

//            Toast.makeText(this, "猜謎遊戲開始\n" + completionJson, Toast.LENGTH_SHORT).show();

            ReceivingGuess receivingGuess = new ReceivingGuess(stuID, robotAPI, petVoice, riddle, expression, completionJson, this);
            Thread receivingGuessThread = new Thread(receivingGuess);
            receivingGuessThread.start();
        } catch (Exception e) {
            this.finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
    }
}

class ReceivingGuess implements Runnable {

    JSONObject riddle;
    String username;
    RobotAPI robotAPI;
    int petVoice;
    boolean isRecognizing = false;
    SpeechConfig speechConfig;
    MediaPlayer voicePlayer;
    SpeechRecognizer reco = null;
    int guessID;
    Context guessActivity;


    public ReceivingGuess(String username, RobotAPI robotAPI, int petVoice, String welcome, String expression, JSONObject completionJson, Context guessActivity) {
        this.username = username;
        this.robotAPI = robotAPI;
        this.petVoice = petVoice;
        this.speechConfig = SpeechConfig.fromSubscription("ef5ca659d4b7452eb9790e13b4a37fcb", "eastasia");
        this.speechConfig.setSpeechRecognitionLanguage("ja-JP");
        this.riddle = completionJson;
        this.guessActivity = guessActivity;

        try {
            this.guessID = this.riddle.getInt("guessID");
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        voicePlayer = new MediaPlayer();

        callVoice(welcome, expression, 0);
    }

    private MicrophoneStream microphoneStream;

    private MicrophoneStream createMicrophoneStream() {
        this.releaseMicrophoneStream();

        microphoneStream = new MicrophoneStream();
        return microphoneStream;
    }

    private void releaseMicrophoneStream() {
        if (microphoneStream != null) {
            microphoneStream.close();
            microphoneStream = null;
        }
    }

    @Override
    public void run() {
        try {
            this.recognizeFromMicrophone();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    public void callVoice(String content, String expression, int action) {
        // 若voicePlayer正在播放，則return
        if (voicePlayer.isPlaying()) {
            return;
        }

        if (content.length() == 0) {
            content += "恭喜你猜對了！";
        }

        // speechrecognizer 停止
        if (reco != null) {
            reco.stopContinuousRecognitionAsync();
        }

        try {
            String encodedContent = URLEncoder.encode(content, "UTF-8");
            String voiceURL = "http://140.115.158.245:23456/voice/vits?text=" + encodedContent + "&id=" + petVoice + "&format=wav&length=1.5";
            try {
                voicePlayer.setDataSource(voiceURL);
            } catch (IOException e) {
                reco.startContinuousRecognitionAsync();
                e.printStackTrace();
            }

            // play VITS audio
            voicePlayer.prepareAsync();
            voicePlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    voicePlayer.start();
                }
            });

            // set error Listener
            voicePlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    Log.e("MediaPlayer", "Error: " + what + ", Extra: " + extra);
                    return false;
                }
            });

            // set zenbo face expression
            robotAPI.robot.setExpression(RobotFace.valueOf(expression));

            // set zenbo action
            robotAPI.utility.playAction(action);

            String finalContent = content;
            voicePlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    if (finalContent.equals("恭喜你猜對了！")) {
                        // send broadcast to end GuessActivity
                        Intent intent = new Intent("endGuess");
                        guessActivity.sendBroadcast(intent);
                        return;
                    }
                    // mediaPlayer結束後，將zenbo face設為DEFAULT
                    // sleep
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    robotAPI.robot.setExpression(RobotFace.DEFAULT);
                    robotAPI.utility.playAction(0);
                    // voicePlayer釋放
                    voicePlayer = new MediaPlayer();
                    // 啟動speechrecognizer
                    reco.startContinuousRecognitionAsync();
                }
            });
        } catch (Exception e) {
            reco.startContinuousRecognitionAsync();
            e.printStackTrace();
        }
    }

    public String recognizeFromMicrophone() throws InterruptedException, ExecutionException {
        AudioConfig audioInput = AudioConfig.fromStreamInput(createMicrophoneStream());
        reco = new SpeechRecognizer(speechConfig, audioInput);

        reco.recognized.addEventListener((o, speechRecognitionResultEventArgs) -> {
            final String s = speechRecognitionResultEventArgs.getResult().getText();
            if (voicePlayer.isPlaying() == false && speechRecognitionResultEventArgs.getResult().getReason() == ResultReason.RecognizedSpeech) {
                if (s.length() > 0 && !isRecognizing && voicePlayer.isPlaying() == false) {
                    Log.i("ed", "Final result received: " + s);
                    isRecognizing = true;
                    ApiTask response = new ApiTask(username, "goGuess", guessID, s);
                    response.execute();
                    try {
                        String responseJson = response.get();
                        JSONObject completionJson = new JSONObject(responseJson);
                        String status = completionJson.getString("status");
                        String content = completionJson.getString("riddle");
                        String expression = completionJson.getString("expression");
                        callVoice(content, expression, 0);
                        isRecognizing = false;
                    } catch (Exception e) {
                        callVoice("沒聽清楚可以再說一次嗎~", "DEFAULT", 0);
                        isRecognizing = false;
                        e.printStackTrace();
                    }
                }
            }
        });

        final Future<Void> task = reco.startContinuousRecognitionAsync();
        return "";
    }
}