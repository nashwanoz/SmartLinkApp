package com.smart.link;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private ImageView imgLogo;
    private TextView tvServerIp, tvSuccessCount, tvFailedCount, tvLogs;
    private EditText etServerPort;
    private Button btnToggleServer, btnDeveloper, btnSettings;

    private boolean serverRunning = false;
    private long backPressedTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imgLogo = findViewById(R.id.imgLogo);
        tvServerIp = findViewById(R.id.tvServerIp);
        tvSuccessCount = findViewById(R.id.tvSuccessCount);
        tvFailedCount = findViewById(R.id.tvFailedCount);
        tvLogs = findViewById(R.id.tvLogs);
        etServerPort = findViewById(R.id.etServerPort);
        btnToggleServer = findViewById(R.id.btnToggleServer);
        btnDeveloper = findViewById(R.id.btnDeveloper);
        btnSettings = findViewById(R.id.btnSettings);

        btnToggleServer.setOnClickListener(v -> {
            if (!serverRunning) {
                startServerService();
                btnToggleServer.setText("إيقاف الخادم");
                btnToggleServer.setBackgroundColor(Color.parseColor("#4CAF50")); // أخضر عند التشغيل
                serverRunning = true;
                Toast.makeText(this, "تم تشغيل الخادم", Toast.LENGTH_SHORT).show();
            } else {
                stopServerService();
                btnToggleServer.setText("تشغيل الخادم");
                btnToggleServer.setBackgroundColor(Color.parseColor("#D32F2F")); // أحمر عند الإيقاف
                serverRunning = false;
                Toast.makeText(this, "تم إيقاف الخادم", Toast.LENGTH_SHORT).show();
            }
        });

        btnDeveloper.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, DeveloperActivity.class));
        });

        btnSettings.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, SettingsActivity.class));
        });
    }

    private void startServerService() {
        Intent serviceIntent = new Intent(this, ServerService.class);
        // استخدام startForegroundService بدل startService
        startForegroundService(serviceIntent);
    }

    private void stopServerService() {
        Intent serviceIntent = new Intent(this, ServerService.class);
        stopService(serviceIntent);
    }
    @Override
    public void onBackPressed() {
        if (backPressedTime + 2000 > System.currentTimeMillis()) {
            super.onBackPressed();
        } else {
            Toast.makeText(this, "اضغط مرة أخرى للخروج من التطبيق", Toast.LENGTH_SHORT).show();
        }
        backPressedTime = System.currentTimeMillis();
    }
}
