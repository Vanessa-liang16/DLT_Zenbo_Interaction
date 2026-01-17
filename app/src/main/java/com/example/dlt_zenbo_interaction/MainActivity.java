package com.example.dlt_zenbo_interaction;


import static android.Manifest.permission.INTERNET;
import static android.Manifest.permission.RECORD_AUDIO;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import com.asus.robotframework.API.RobotAPI;
import com.asus.robotframework.API.RobotFace;
import com.microsoft.cognitiveservices.speech.ResultReason;
import com.microsoft.cognitiveservices.speech.SpeechConfig;
import com.microsoft.cognitiveservices.speech.SpeechRecognitionResult;
import com.microsoft.cognitiveservices.speech.SpeechRecognizer;
import com.microsoft.cognitiveservices.speech.audio.AudioConfig;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Enumeration;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class MainActivity extends RobotActivity {

    private EditText studentID;
    private Button connectionBtn;
    private Button guessBtn;
    private Button scriptBtn;
    private Button soulBtn;
    private TextView zenboIP;
    private TextView IPAddress;
    private int petVoice;

    Thread receivingThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // 隱藏上方狀態列
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        this.init();
        this.IPAddress.setText(getLocalIPv4Address());

        ActivityCompat.requestPermissions(MainActivity.this, new String[]{RECORD_AUDIO, INTERNET}, 5);

        // 聊天用Button
        connectionBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Login Task
                String stuID = studentID.getText().toString();
                Log.d("stuID", stuID);
                ApiTask loginTask = new ApiTask(stuID, "Login");
                loginTask.execute();

                // LoginTask執行完後
                try {
                    String response = loginTask.get();
                    JSONObject petJson = new JSONObject(response);
                    petVoice = petJson.getInt("voice");

                    // Welcome Task
                    ApiTask welcomeTask = new ApiTask(stuID, "Welcome");
                    welcomeTask.execute();
                    response = welcomeTask.get();
                    JSONObject welcomeJson = new JSONObject(response);
                    String completion = welcomeJson.getString("completion");

                    // completion 為JSON格式的字串
                    JSONObject completionJson = new JSONObject(completion);
                    String welcome = completionJson.getString("completion");
                    String expression = completionJson.getString("expression");

                    // 存入singleton (RobotDatabase)
                    RobotDatabase db = RobotDatabase.getInstance();
                    db.setData(robotAPI, petVoice, stuID);

                    // activate Receiving Thread
                    receivingThread = new Thread(new Receiving(stuID ,robotAPI, petVoice, welcome, expression));
                    receivingThread.start();

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        // 猜謎用Button
        guessBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 取得EditText的內容
                String stuID = studentID.getText().toString();
                // 切換Activity 至 GuessActivity
                Intent intent = new Intent(MainActivity.this, GuessActivity.class);
                intent.putExtra("stuID", stuID);
                startActivity(intent);
            }
        });

        // 劇本用Button
        scriptBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 切換Activity 至 ScriptActivity
                Intent intent = new Intent(MainActivity.this, ScriptActivity.class);
                startActivity(intent);
            }
        });

        //
        // 寵物靈魂用Button
        soulBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setContentView(R.layout.petwebview);

                WebView webView = findViewById(R.id.webView);

                // 加載網頁
                webView.loadUrl("http://140.115.158.199:5173/");

                WebSettings webSettings = webView.getSettings();
                webSettings.setJavaScriptEnabled(true);  // 啟用 JavaScript

                //2026
                // 使用 RobotDatabase 的實例方法創建 WebAppInterface
                RobotDatabase robotDatabase = RobotDatabase.getInstance();
                robotDatabase.setData(robotAPI, 0, ""); // 使用當前的 robotAPI
                robotDatabase.setContext(MainActivity.this); // 設置 Context
                webView.addJavascriptInterface(robotDatabase.new WebAppInterface(), "Android");

                webSettings.setBuiltInZoomControls(false);  // 啟用縮放

                webSettings.setDomStorageEnabled(true);
                webSettings.setUseWideViewPort(true);
                webSettings.setLoadWithOverviewMode(true);
                webSettings.setDisplayZoomControls(false);
                webSettings.setBuiltInZoomControls(false);
                webSettings.setMediaPlaybackRequiresUserGesture(false);
            }
        });
    }

    // 初始化
    private void init() {
        this.studentID = findViewById(R.id.studentID);
        this.connectionBtn = findViewById(R.id.connectionBtn);
        this.guessBtn = findViewById(R.id.guessBtn);
        this.scriptBtn = findViewById(R.id.scriptBtn);
        this.soulBtn = findViewById(R.id.soulBtn);
        this.zenboIP = findViewById(R.id.zenboIP);
        this.IPAddress = findViewById(R.id.IPAddress);
    }

    // 取得本機IPv4
    public static String getLocalIPv4Address() {
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = networkInterfaces.nextElement();
                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress inetAddress = addresses.nextElement();
                    if (!inetAddress.isLoopbackAddress() && inetAddress.getAddress().length == 4) {
                        // IPv4 地址
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


}

class ApiTask extends AsyncTask<Void, Void, String> {

    private static final String API_URL = "http://140.115.158.245:5173/api/zenbo/";
    private static final String TAG = "LoginTask";

    private String username;
    private String command;
    private String msg;

    // for riddle
    private int guessID;
    private String answer;

    public ApiTask(String username, String command) {
        this.username = username;
        this.command = command;
    }

    public ApiTask(String username, String command, String msg) {
        this.username = username;
        this.command = command;
        this.msg = msg;
    }

    public ApiTask(String username, String command, int guessID, String answer) {
        this.username = username;
        this.command = command;
        this.guessID = guessID;
        this.answer = answer;
    }

    // 做 GET 請求
    @Override
    protected String doInBackground(Void... voids) {
        switch (command) {
            case "Login":
                return getData();
            case "Welcome":
                return getWelcome();
            case "Response":
                return getResponse();
            case "Script":
                return getScript();
            case "GetScript":
                return getFullScript();
            case "Guessing":
                return getGuessing();
            case "goGuess":
                return goGuess();
            case "ScriptResponse":
                return getScriptResponse();
            default:
                return null;
        }

    }

    //2026
    // 新增 WebAppInterface 類別
    class WebAppInterface {
        @JavascriptInterface
        public void triggerZenboAction(String action) {
            RobotDatabase db = RobotDatabase.getInstance();
            switch(action) {
                case "happy":
//                    db.performZenboAction(RobotDatabase.ACTION_HAPPY);
                    break;
                case "dance":
                    db.performZenboAction(RobotDatabase.ACTION_DANCE);
                    break;
            }
        }
    }
    private String getResponse() {
        try {
            // 建立連線
            // response encode
            msg = URLEncoder.encode(msg, "UTF-8");
            URL url = new URL(API_URL + "getMsg/" + username + "/" + msg);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();

            // 讀取請求結果
            InputStream inputStream = connection.getInputStream();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

            // 得到json格式的請求結果
            String line;
            StringBuilder response = new StringBuilder();
            while ((line = bufferedReader.readLine()) != null) {
                response.append(line);
            }

            // 關閉連線
            bufferedReader.close();
            inputStream.close();
            connection.disconnect();

            // 將取得的JSON回傳
            return response.toString();

        } catch (Exception e) {
            Log.e(TAG, "Error in doInBackground", e);
            return null;
        }
    }

    private String getData() {
        try {
            // 建立連線
            URL url = new URL(API_URL + "getData/" + username);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();

            // 讀取請求結果
            InputStream inputStream = connection.getInputStream();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

            // 得到json格式的請求結果
            String line;
            StringBuilder response = new StringBuilder();
            while ((line = bufferedReader.readLine()) != null) {
                response.append(line);
            }

            // 關閉連線
            bufferedReader.close();
            inputStream.close();
            connection.disconnect();

            // 將取得的JSON回傳
            return response.toString();

        } catch (Exception e) {
            Log.e(TAG, "Error in doInBackground", e);
            return null;
        }
    }

    private String getWelcome() {
        try {
            // 建立連線
            URL url = new URL(API_URL + "welcome/" + username);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();

            // 讀取請求結果
            InputStream inputStream = connection.getInputStream();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

            // 得到json格式的請求結果
            String line;
            StringBuilder response = new StringBuilder();
            while ((line = bufferedReader.readLine()) != null) {
                response.append(line);
            }

            // 關閉連線
            bufferedReader.close();
            inputStream.close();
            connection.disconnect();

            // 將取得的JSON回傳
            return response.toString();

        } catch (Exception e) {
            Log.e(TAG, "Error in doInBackground", e);
            return null;
        }
    }

    private String getScript() {
        try {
            // 建立連線
            URL url = new URL(API_URL + "welcome/script/" + username);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();

            // 讀取請求結果
            InputStream inputStream = connection.getInputStream();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

            // 得到json格式的請求結果
            String line;
            StringBuilder response = new StringBuilder();
            while ((line = bufferedReader.readLine()) != null) {
                response.append(line);
            }

            // 關閉連線
            bufferedReader.close();
            inputStream.close();
            connection.disconnect();

            // 將取得的JSON回傳
            return response.toString();

        } catch (Exception e) {
            Log.e(TAG, "Error in doInBackground", e);
            return null;
        }
    }

    private String getFullScript() {
        try {
            // 建立連線
            URL url = new URL(API_URL + "getScript");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();

            // 讀取請求結果
            InputStream inputStream = connection.getInputStream();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

            // 得到json格式的請求結果
            String line;
            StringBuilder response = new StringBuilder();
            while ((line = bufferedReader.readLine()) != null) {
                response.append(line);
            }

            // 關閉連線
            bufferedReader.close();
            inputStream.close();
            connection.disconnect();

            // 將取得的JSON回傳
            return response.toString();

        } catch (Exception e) {
            Log.e(TAG, "Error in doInBackground", e);
            return null;
        }

    }
    private String goGuess() {
        try {
            // answer encode
            answer = URLEncoder.encode(answer, "UTF-8");
            // 建立連線
            URL url = new URL(API_URL + "guessRiddle/" + username + "/" + guessID + "/" + answer);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();

            // 讀取請求結果
            InputStream inputStream = connection.getInputStream();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

            // 得到json格式的請求結果
            String line;
            StringBuilder response = new StringBuilder();
            while ((line = bufferedReader.readLine()) != null) {
                response.append(line);
            }

            // 關閉連線
            bufferedReader.close();
            inputStream.close();
            connection.disconnect();

            // 將取得的JSON回傳
            return response.toString();

        } catch (Exception e) {
            Log.e(TAG, "Error in doInBackground", e);
            return null;
        }
    }

    private String getGuessing() {
        try {
            // 建立連線
            URL url = new URL(API_URL + "getGuessing/" + username);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();

            // 讀取請求結果
            InputStream inputStream = connection.getInputStream();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

            // 得到json格式的請求結果
            String line;
            StringBuilder response = new StringBuilder();
            while ((line = bufferedReader.readLine()) != null) {
                response.append(line);
            }

            // 關閉連線
            bufferedReader.close();
            inputStream.close();
            connection.disconnect();

            // 將取得的JSON回傳
            return response.toString();

        } catch (Exception e) {
            Log.e(TAG, "Error in doInBackground", e);
            return null;
        }
    }

    private String getScriptResponse() {
        try {
            // 建立連線
            // response encode
            answer = URLEncoder.encode(answer, "UTF-8");
            URL url = new URL(API_URL + "getScriptResponse/" + username + "/" + guessID + "/" + answer);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();

            // 讀取請求結果
            InputStream inputStream = connection.getInputStream();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

            // 得到json格式的請求結果
            String line;
            StringBuilder response = new StringBuilder();
            while ((line = bufferedReader.readLine()) != null) {
                response.append(line);
            }

            // 關閉連線
            bufferedReader.close();
            inputStream.close();
            connection.disconnect();

            // 將取得的JSON回傳
            return response.toString();

        } catch (Exception e) {
            Log.e(TAG, "Error in doInBackground", e);
            return null;
        }
    }
}

class Receiving implements Runnable {

    String username;
    RobotAPI robotAPI;
    int petVoice;
    boolean isRecognizing = false;
    SpeechConfig speechConfig;
    MediaPlayer voicePlayer;
    SpeechRecognizer reco = null;


    public Receiving(String username, RobotAPI robotAPI, int petVoice, String welcome, String expression) {
        this.username = username;
        this.robotAPI = robotAPI;
        this.petVoice = petVoice;
        this.speechConfig = SpeechConfig.fromSubscription("ef5ca659d4b7452eb9790e13b4a37fcb", "eastasia");
        this.speechConfig.setSpeechRecognitionLanguage("zh-TW");

        // init MediaPlayer
        voicePlayer = new MediaPlayer();

        // welcome
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

            voicePlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    // mediaPlayer結束後，將zenbo face設為DEFAULT
                    // sleep
//                    try {
//                        Thread.sleep(10);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
                    robotAPI.robot.setExpression(RobotFace.DEFAULT);
                    robotAPI.utility.playAction(0);
                    // voicePlayer釋放
                    voicePlayer.release();
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
                    ApiTask response = new ApiTask(username, "Response", s);
                    response.execute();
                    try {
                        String responseJson = response.get();
                        JSONObject responseJsonObj = new JSONObject(responseJson);
                        String completion = responseJsonObj.getString("completion");
                        JSONObject completionJson = new JSONObject(completion);
                        String responseContent = completionJson.getString("completion");
                        String expression = completionJson.getString("expression");
                        int action = completionJson.getInt("action");
                        callVoice(responseContent, expression, action);
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
    }}
