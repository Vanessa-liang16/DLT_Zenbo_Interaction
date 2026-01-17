package com.example.dlt_zenbo_interaction;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import java.util.HashMap;
import java.util.HashSet;

public class ScriptActivity extends RobotActivity {

    ImageButton counter;
    ImageButton dish;
    ImageButton order;
    ImageButton customer1;
    ImageButton customer2;

    Button ScriptBack;
    Button ScriptNext;

    EditText cID;
    EditText dID;
    EditText oID;
    EditText c1ID;
    EditText c2ID;
    private HashSet<String> selectedCharacters;

    // Button Scene1~Scene8
    Button Scene1;
    Button Scene2;
    Button Scene3;
    Button Scene4;
    Button Scene5;
    Button Scene6;
    Button Scene7;
    Button Scene8;

    Button SceneBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.script);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        this.selectedCharacters = new HashSet<>();
        this.init();
        this.setListeners();
    }

    private void init() {
        // initialize the ImageButtons
        this.counter = findViewById(R.id.CounterWaiter);
        this.dish = findViewById(R.id.DishWaiter);
        this.order = findViewById(R.id.OrderWaiter);
        this.customer1 = findViewById(R.id.Customer1);
        this.customer2 = findViewById(R.id.Customer2);
        // initialize the Buttons
        this.ScriptBack = findViewById(R.id.ScriptBack);
        this.ScriptNext = findViewById(R.id.ScriptNext);
        // initialize the EditTexts
        this.cID = findViewById(R.id.CW_ID);
        this.dID = findViewById(R.id.DW_ID);
        this.oID = findViewById(R.id.OW_ID);
        this.c1ID = findViewById(R.id.C1_ID);
        this.c2ID = findViewById(R.id.C2_ID);
    }

    private void init2() {
        // initialize the Button Scene1~Scene8
        this.Scene1 = findViewById(R.id.Scene1);
        this.Scene2 = findViewById(R.id.Scene2);
        this.Scene3 = findViewById(R.id.Scene3);
        this.Scene4 = findViewById(R.id.Scene4);
        this.Scene5 = findViewById(R.id.Scene5);
        this.Scene6 = findViewById(R.id.Scene6);
        this.Scene7 = findViewById(R.id.Scene7);
        this.Scene8 = findViewById(R.id.Scene8);
        // initialize the Button SceneBack
        this.SceneBack = findViewById(R.id.SceneBack);
    }

    private void setListeners() {
        // set listeners for the ImageButtons
        counter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selectedCharacters.contains("CounterWaiter")) {
                    selectedCharacters.remove("CounterWaiter");
                    counter.setBackgroundColor(Color.LTGRAY);
                } else {
                    selectedCharacters.add("CounterWaiter");
                    counter.setBackgroundColor(Color.CYAN);
                }
            }
        });

        dish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selectedCharacters.contains("DishWaiter")) {
                    selectedCharacters.remove("DishWaiter");
                    dish.setBackgroundColor(Color.LTGRAY);
                } else {
                    selectedCharacters.add("DishWaiter");
                    dish.setBackgroundColor(Color.CYAN);
                }
            }
        });

        order.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selectedCharacters.contains("OrderWaiter")) {
                    selectedCharacters.remove("OrderWaiter");
                    order.setBackgroundColor(Color.LTGRAY);
                } else {
                    selectedCharacters.add("OrderWaiter");
                    order.setBackgroundColor(Color.CYAN);
                }
            }
        });

        customer1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selectedCharacters.contains("Customer1")) {
                    selectedCharacters.remove("Customer1");
                    customer1.setBackgroundColor(Color.LTGRAY);
                } else {
                    selectedCharacters.add("Customer1");
                    customer1.setBackgroundColor(Color.CYAN);
                }
            }
        });

        customer2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selectedCharacters.contains("Customer2")) {
                    selectedCharacters.remove("Customer2");
                    customer2.setBackgroundColor(Color.LTGRAY);
                } else {
                    selectedCharacters.add("Customer2");
                    customer2.setBackgroundColor(Color.CYAN);
                }
            }
        });

        // set listeners for the Buttons
        ScriptBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        ScriptNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // change layout
                setContentView(R.layout.script_scene);
                init2();
                setListener2();
            }
        });
    }

    private void setListener2() {
        SceneBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setContentView(R.layout.script);
                init();
                setListeners();
            }
        });

        // set listeners for the Button Scene1~Scene8
        Scene1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ScriptActivity.this, StartScriptActivity.class);
                intent.putExtra("currentLine", 129);
                intent.putExtra("endLine", 187);
                startScene(intent);
            }
        });

        Scene2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ScriptActivity.this, StartScriptActivity.class);
                intent.putExtra("currentLine", 187);
                intent.putExtra("endLine", 134);
                startScene(intent);
            }
        });

        Scene3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ScriptActivity.this, StartScriptActivity.class);
                intent.putExtra("currentLine", 134);
                intent.putExtra("endLine", 145);
                startScene(intent);
            }
        });

        Scene4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ScriptActivity.this, StartScriptActivity.class);
                intent.putExtra("currentLine", 145);
                intent.putExtra("endLine", 160);
                startScene(intent);
            }
        });

        Scene5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ScriptActivity.this, StartScriptActivity.class);
                intent.putExtra("currentLine", 160);
                intent.putExtra("endLine", 166);
                startScene(intent);
            }
        });

        Scene6.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ScriptActivity.this, StartScriptActivity.class);
                intent.putExtra("currentLine", 166);
                intent.putExtra("endLine", 171);
                startScene(intent);
            }
        });

        Scene7.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ScriptActivity.this, StartScriptActivity.class);
                intent.putExtra("currentLine", 171);
                intent.putExtra("endLine", 181);
                startScene(intent);
            }
        });

        Scene8.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ScriptActivity.this, StartScriptActivity.class);
                intent.putExtra("currentLine", 181);
                intent.putExtra("endLine", 0);
                startScene(intent);
            }
        });
    }

    private void startScene(Intent intent) {
        if (selectedCharacters.size() == 0) {
            // toast message
            Toast.makeText(ScriptActivity.this, "請至少選擇一名角色", Toast.LENGTH_SHORT).show();
        } else {
            // 有角色後，要檢查是否還有填寫學號
            if (selectedCharacters.contains("CounterWaiter") && cID.getText().toString().equals("")) {
                Toast.makeText(ScriptActivity.this, "請填寫櫃台接待學號", Toast.LENGTH_SHORT).show();
                return;
            } else if (selectedCharacters.contains("DishWaiter") && dID.getText().toString().equals("")) {
                Toast.makeText(ScriptActivity.this, "請填寫送餐服務生學號", Toast.LENGTH_SHORT).show();
                return;
            } else if (selectedCharacters.contains("OrderWaiter") && oID.getText().toString().equals("")) {
                Toast.makeText(ScriptActivity.this, "請填寫點餐服務生學號", Toast.LENGTH_SHORT).show();
                return;
            } else if (selectedCharacters.contains("Customer1") && c1ID.getText().toString().equals("")) {
                Toast.makeText(ScriptActivity.this, "請填寫客人1學號", Toast.LENGTH_SHORT).show();
                return;
            } else if (selectedCharacters.contains("Customer2") && c2ID.getText().toString().equals("")) {
                Toast.makeText(ScriptActivity.this, "請填寫客人2學號", Toast.LENGTH_SHORT).show();
                return;
            }

            // 將selectedcharacters與對應學號，做成HashMap(String, String)
            // 並傳遞到下一個Activity
            HashMap<String, String> characters = new HashMap<>();
            if (ScriptActivity.this.selectedCharacters.contains("CounterWaiter")) {
                characters.put("CounterWaiter", cID.getText().toString());
            } else {
                characters.put("CounterWaiter", "");
            }
            if (ScriptActivity.this.selectedCharacters.contains("DishWaiter")) {
                characters.put("DishWaiter", dID.getText().toString());
            } else {
                characters.put("DishWaiter", "");
            }
            if (ScriptActivity.this.selectedCharacters.contains("OrderWaiter")) {
                characters.put("OrderWaiter", oID.getText().toString());
            } else {
                characters.put("OrderWaiter", "");
            }
            if (ScriptActivity.this.selectedCharacters.contains("Customer1")) {
                characters.put("Customer1", c1ID.getText().toString());
            } else {
                characters.put("Customer1", "");
            }
            if (ScriptActivity.this.selectedCharacters.contains("Customer2")) {
                characters.put("Customer2", c2ID.getText().toString());
            } else {
                characters.put("Customer2", "");
            }

            // 切換到下一個Activity(StartScriptActivity)，並把資料傳遞過去
            // Intent intent = new Intent(ScriptActivity.this, StartScriptActivity.class);
            intent.putExtra("characters", characters);
            startActivity(intent);
        }
    }

}
