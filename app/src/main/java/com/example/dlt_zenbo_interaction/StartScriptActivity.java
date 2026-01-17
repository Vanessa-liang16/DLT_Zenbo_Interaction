package com.example.dlt_zenbo_interaction;

import static android.Manifest.permission.INTERNET;
import static android.Manifest.permission.RECORD_AUDIO;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.tts.Voice;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
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
import com.microsoft.cognitiveservices.speech.SpeechRecognizer;
import com.microsoft.cognitiveservices.speech.audio.AudioConfig;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class StartScriptActivity extends RobotActivity {

    // characters dictionary(character name, user id)
    private HashMap<String, String> characters;
    // userData dictionary(user id, user data)
    private HashMap<String, JSONObject> userData;
    // voices dictionary(character name, voice id)
    private HashMap<String, Integer> voices;
    // narrator voice id
    private int narratorVoice = 28;
    // JSON Array格式的劇本
    private JSONArray script;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.scriptempty);
        // 隱藏上方狀態列
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // keep screen
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // 取得切換Activity的Intent物件
        Intent intent = getIntent();
        // 取得傳遞過來的資料(HashMap)
        characters = (HashMap<String, String>) intent.getSerializableExtra("characters");
        // get currentLine(int)
        int currentLine = intent.getIntExtra("currentLine", 129);
        // get endLine(int)
        int endLine = intent.getIntExtra("endLine", 0);

        // 取得資料(ApiTask)
        userData = new HashMap<>();
        voices = new HashMap<>();
        for (String key : characters.keySet()) {
            String stuID = characters.get(key);
            if (stuID.equals("")) {
                // 若沒有設定使用者ID，則使用預設voice
                if (key.equals("DishWaiter")) {
                    voices.put(key, 39);
                } else if (key.equals("OrderWaiter")) {
                    voices.put(key, 115);
                } else if (key.equals("CounterWaiter")) {
                    voices.put(key, 137);
                } else if (key.equals("Customer1")) {
                    voices.put(key, 117);
                } else if (key.equals("Customer2")) {
                    voices.put(key, 43);
                }

                // Toast.makeText(this, "userData: " + voices, Toast.LENGTH_SHORT).show();

            } else {
                ApiTask loginTask = new ApiTask(stuID, "Login");
                loginTask.execute();

                // LoginTask執行完後
                try {
                    String response = loginTask.get();
                    JSONObject petJson = new JSONObject(response);
                    // 存入userData
                    userData.put(stuID, petJson);

                    // 存入voices
                    voices.put(key, petJson.getInt("voice"));

                    // Welcome Task
                    ApiTask welcomeTask = new ApiTask(stuID, "Script");
                    welcomeTask.execute();
                    response = welcomeTask.get();
                    JSONObject welcomeJson = new JSONObject(response);
                    String completion = welcomeJson.getString("completion");

                    // completion 為JSON格式的字串
                    JSONObject completionJson = new JSONObject(completion);
                    String welcome = completionJson.getString("completion");
                    String expression = completionJson.getString("expression");

                    // Voice
//                    VoiceTask vt = new VoiceTask(robotAPI, welcome, expression, 0, voices.get(key));
//                    vt.execute();
//                    vt.get();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                // Toast.makeText(this, "userData: " + voices, Toast.LENGTH_SHORT).show();
            }
        }

        // 取得Script (不須student id)
        ApiTask scriptTask = new ApiTask("0", "GetScript");
        scriptTask.execute();
        // 取得回傳的String Array
        String response = null;
        try {
            // JSON Array
            response = scriptTask.get();
            script = new JSONArray(response);
//            Toast.makeText(this, "script: " + script, Toast.LENGTH_SHORT).show();
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // 啟動ReceivingScript
        ReceivingScript receivingScript = new ReceivingScript("即將開始練習劇本，歡迎光臨棒壽司！", "DEFAULT", characters, userData, voices, script, robotAPI, narratorVoice, this, currentLine, endLine);
        Thread receivingScriptThread = new Thread(receivingScript);
        receivingScriptThread.start();
    }
}

//
class VoiceTask extends AsyncTask<Void, Void, Void> {

    RobotAPI robotAPI;
    int petVoice;
    MediaPlayer voicePlayer = new MediaPlayer();

    private String content;
    private String expression;
    private int action;
    private Exception exception;

    public VoiceTask(RobotAPI robotAPI, String content, String expression, int action, int voice) {
        this.robotAPI = robotAPI;
        this.content = content;
        this.expression = expression;
        this.action = action;
        this.petVoice = voice;
    }

    @Override
    protected Void doInBackground(Void... voids) {
        // 若voicePlayer正在播放，則return
        if (voicePlayer.isPlaying()) {
            return null;
        }

        try {
            String encodedContent = URLEncoder.encode(content, "UTF-8");
            String voiceURL = "http://140.115.158.245:23456/voice/vits?text=" + encodedContent + "&id=" + petVoice + "&format=wav&length=1.5";
            try {
                voicePlayer.setDataSource(voiceURL);
            } catch (IOException e) {
                exception = e;
                return null;
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
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    robotAPI.robot.setExpression(RobotFace.DEFAULT);
                    robotAPI.utility.playAction(0);
                    // voicePlayer釋放
                    voicePlayer = new MediaPlayer();
                }
            });
        } catch (Exception e) {
            exception = e;
        }

        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        // 在完成后执行任何UI更新或其他后续操作
        if (exception != null) {
            // 处理异常
            exception.printStackTrace();
        } else {
            // 操作成功完成
        }
    }
}
//

class ReceivingScript implements Runnable {

    // characters dictionary(character name, user id)
    private HashMap<String, String> characters;
    // userData dictionary(user id, user data)
    private HashMap<String, JSONObject> userData;
    // voices dictionary(character name, voice id)
    private HashMap<String, Integer> voices;
    // lines dictionary(character name, lines)
    private HashMap<String, int[]> lines;
    // narrator voice id
    private int narratorVoice = 28;
    // JSON Array格式的劇本
    private JSONArray script;
    boolean isRecognizing = true;
    SpeechConfig speechConfig;
    MediaPlayer voicePlayer;
    MediaPlayer hintPlayer;
    SpeechRecognizer reco = null;
    RobotAPI robotAPI;
    String currentCharacter = "Narrator";
    int currentLine = 129;
    int nextLine = 129;
    int endLine = 0;
    StartScriptActivity startScriptActivity;
    Boolean isSpeaking = false;
    Boolean hintSound = false;

    int[] narratorLines = new int[] {129, 130, 131, 132, 133, 134, 140, 145, 160, 162, 154, 166, 171, 181, 182, 183, 184, 185, 186, 187, 190, 191};
    int[] owLines = new int[] {146, 148, 150, 152, 156, 159};
    int[] dwLines = new int[] {161, 163, 195, 168, 170, 189, 193};
    int[] cwLines = new int[] {139, 141, 144, 173, 175, 176, 178, 180, 188, 192, 194};
    int[] c1Lines = new int[] {135, 137, 147, 149, 151, 155, 167, 169, 172, 177};
    int[] c2Lines = new int[] {136, 138, 142, 153, 157, 164, 174};
    int[] petLines = new int[] {143, 158, 165, 179};

    public ReceivingScript(String welcome, String expression, HashMap<String, String> characters, HashMap<String, JSONObject> userData, HashMap<String, Integer> voices, JSONArray script, RobotAPI robotAPI, int narratorVoice, StartScriptActivity startScriptActivity, int currentLine, int endLine) {
        // init variables
        this.characters = characters;
        this.userData = userData;
        this.voices = voices;
        this.script = script;
        this.robotAPI = robotAPI;
        this.narratorVoice = narratorVoice;
        this.startScriptActivity = startScriptActivity;

        // set currentLine
        this.currentLine = currentLine;
        this.nextLine = currentLine;
        this.endLine = endLine;

        // add lines to hashmap
        lines = new HashMap<>();
        lines.put("Narrator", narratorLines);
        lines.put("OrderWaiter", owLines);
        lines.put("DishWaiter", dwLines);
        lines.put("CounterWaiter", cwLines);
        lines.put("Customer1", c1Lines);
        lines.put("Customer2", c2Lines);
        lines.put("Pet", petLines);

        // add character "Narrator" to characters
        characters.put("Narrator", "");
        characters.put("Pet", "");

        // put narrator voice into voices
        voices.put("Narrator", narratorVoice);
        // pet == customer2
        voices.put("Pet", voices.get("Customer2"));

        // init azure speech settings
        this.speechConfig = SpeechConfig.fromSubscription("ef5ca659d4b7452eb9790e13b4a37fcb", "eastasia");
        this.speechConfig.setSpeechRecognitionLanguage("ja-JP");

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
        // if currentLine = 0 => finish this activity
        // set current character
        // currentLine in lines.get(currentCharacter)
        // 若currentLine在petLines中，則currentCharacter為Pet
        if (currentLine == 143 || currentLine == 158 || currentLine == 165 || currentLine == 179) {
            currentCharacter = "Pet";
        } else {
            for (String key : lines.keySet()) {
                for (int line : lines.get(key)) {
                    if (line == currentLine) {
                        currentCharacter = key;
                        break;
                    }
                }
            }
        }

        // 若currentCharacter有人扮演，啟動speechrecognizer
        if (!characters.get(currentCharacter).equals("") && !isSpeaking) {
            this.isSpeaking = true;
            this.isRecognizing = false;

            // 播放提示音效
            hintPlayer = MediaPlayer.create(startScriptActivity, R.raw.start);
            hintPlayer.start();

            // if finish => release and new MediaPlayer (listener)
            hintPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    hintPlayer.release();
                    hintPlayer = new MediaPlayer();
                }
            });

            // speechrecognizer 啟動
            reco.startContinuousRecognitionAsync();
            return;
        }

        this.isSpeaking = false;

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
            String voiceURL = "http://140.115.158.245:23456/voice/vits?text=" + encodedContent + "&id=" + voices.get(currentCharacter) + "&format=wav&length=1.5";
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
//            robotAPI.utility.playAction(action);

            voicePlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    // 若沒收到音
                    if (content.equals("沒聽清楚可以再說一次嗎~")) {
                        // speechrecognizer 啟動
                        voicePlayer.release();
                        voicePlayer = new MediaPlayer();
                        isSpeaking = true;
                        isRecognizing = false;
                        reco.startContinuousRecognitionAsync();
                        return;
                    }

                    // mediaPlayer結束後，將zenbo face設為DEFAULT
                    // sleep
                    if (currentLine == -1) {
                        // 切換Activity
//                        Intent intent = new Intent(startScriptActivity, MainActivity.class);
//                        startScriptActivity.startActivity(intent);
                        return;
                    }
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    robotAPI.robot.setExpression(RobotFace.DEFAULT);
//                    robotAPI.utility.playAction(0);
                    // voicePlayer釋放
                    voicePlayer.release();
                    voicePlayer = new MediaPlayer();
                    // 啟動speechrecognizer
//                    reco.startContinuousRecognitionAsync();
                    currentLine = nextLine;

                    // end
                    if (currentLine == endLine) {
                        // end
                        currentCharacter = "Narrator";
                        callVoice("劇本結束，謝謝您的參與！", "DEFAULT", 0);
                        currentLine = -1;
                        return;
                    }

                    // 找JSON Array的next_section_id
                    for (int i = 0; i < script.length(); i++) {
                        try {
                            JSONObject scriptObj = script.getJSONObject(i);
                            if (scriptObj.getInt("section_id") == currentLine) {
                                nextLine = scriptObj.getInt("next_section_id");
                                break;
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                    // get next script content
                    for (int i = 0; i < script.length(); i++) {
                        try {
                            JSONObject scriptObj = script.getJSONObject(i);
                            if (scriptObj.getInt("section_id") == currentLine) {
                                callVoice(scriptObj.getString("content"), "DEFAULT", 0);
                                break;
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
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
                    try {
                        hintPlayer = MediaPlayer.create(startScriptActivity, R.raw.finish);
                        hintPlayer.start();
                        hintPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                            @Override
                            public void onCompletion(MediaPlayer mp) {
                                hintPlayer.release();
                                hintPlayer = new MediaPlayer();
                            }
                        });
                        ApiTask response = new ApiTask(characters.get(currentCharacter), "ScriptResponse", currentLine, s);
                        response.execute();
                        String responseJson = response.get();
                        JSONObject completionJson = new JSONObject(responseJson);
                        String responseContent = completionJson.getString("completion");
                        String expression = completionJson.getString("expression");
                        int score = completionJson.getInt("score");
//                        int action = completionJson.getInt("action");
                        callVoice(responseContent, expression, 0);
                    } catch (Exception e) {
                        callVoice("沒聽清楚可以再說一次嗎~", "DOUBTING", 0);
                        e.printStackTrace();
                    }
                }
            }
        });

        final Future<Void> task = reco.startContinuousRecognitionAsync();
        return "";
    }
}