package com.smart.link;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.URLDecoder;
import java.net.UnknownHostException;
import java.nio.ByteOrder;
import java.util.List;
import java.util.Map;
import fi.iki.elonen.NanoHTTPD;

public class MainActivity extends AppCompatActivity {

    private static final int SMS_PERMISSION_CODE = 101;
    private static final String PREFS_NAME = "SmartLinkPrefs";
    private static final String KEY_SUCCESS = "success_count";
    private static final String KEY_FAILED = "failed_count";

    private TextView tvServerIp, tvLogs, tvSuccessCount, tvFailedCount;
    private EditText etServerPort;
    private Button btnToggleServer, btnSettings, btnDeveloper;

    private MySmsServer smsServer;
    private boolean isServerRunning = false;

    private int successCounter = 0;
    private int failedCounter = 0;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // ربط عناصر الواجهة
        tvServerIp = findViewById(R.id.tvServerIp);
        tvLogs = findViewById(R.id.tvLogs);
        tvSuccessCount = findViewById(R.id.tvSuccessCount);
        tvFailedCount = findViewById(R.id.tvFailedCount);
        etServerPort = findViewById(R.id.etServerPort);
        btnToggleServer = findViewById(R.id.btnToggleServer);
        btnSettings = findViewById(R.id.btnSettings);
        btnDeveloper = findViewById(R.id.btnDeveloper);

        // زر الإعدادات
        btnSettings.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        });

        // زر المطور
        btnDeveloper.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, DeveloperActivity.class);
            startActivity(intent);
        });

        // زر تشغيل/إيقاف الخادم
        btnToggleServer.setOnClickListener(v -> {
            if (!isServerRunning) {
                int port = Integer.parseInt(etServerPort.getText().toString());
                smsServer = new MySmsServer(port);
                try {
                    smsServer.start();
                    isServerRunning = true;
                    logMessage("🟢 تم تشغيل الخادم على المنفذ: " + port);
                    btnToggleServer.setText(getString(R.string.main_toggle_stop));
                } catch (IOException e) {
                    logMessage("❌ فشل تشغيل الخادم: " + e.getMessage());
                }
            } else {
                stopServer();
                btnToggleServer.setText(getString(R.string.main_toggle_start));
            }
        });

        // فحص صلاحيات SMS
        checkSmsPermission();
    }

    private void checkSmsPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, SMS_PERMISSION_CODE);
        }
    }

    private class MySmsServer extends NanoHTTPD {
        public MySmsServer(int port) {
            super(port);
        }

        @Override
        public Response serve(IHTTPSession session) {
            String uri = session.getUri();

            if (uri.equals("/send")) {
                Map<String, List<String>> decodedParameters = session.getParameters();

                String rawPhone = decodedParameters.get("phone") != null ? decodedParameters.get("phone").get(0) : null;
                String rawMessage = decodedParameters.get("message") != null ? decodedParameters.get("message").get(0) : null;

                try {
                    final String phone = rawPhone != null ? URLDecoder.decode(rawPhone, "UTF-8") : null;
                    final String message = rawMessage != null ? URLDecoder.decode(rawMessage, "UTF-8") : null;

                    if (phone != null && message != null) {
                        logMessage("📥 تم العثور على رسالة إلى: " + phone);

                        runOnUiThread(() -> sendSmsMessage(phone, message));

                        return newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, "Success");
                    }
                } catch (Exception e) {
                    logMessage("❌ خطأ في فك ترميز نصوص الرسالة الواردة.");
                }
            }
            return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "Invalid Request");
        }
    }

    private void logMessage(final String msg) {
        runOnUiThread(() -> {
            if (tvLogs != null) {
                tvLogs.append("\n" + msg);
            }
        });
    }

    private void updateIpDisplay() {
        try {
            WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            int ipAddress = wifiManager.getConnectionInfo().getIpAddress();
            if (ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN)) {
                ipAddress = Integer.reverseBytes(ipAddress);
            }
            byte[] ipByteArray = BigInteger.valueOf(ipAddress).toByteArray();
            String ipAddressString = InetAddress.getByAddress(ipByteArray).getHostAddress();
            tvServerIp.setText("IP: " + ipAddressString);
        } catch (UnknownHostException ex) {
            tvServerIp.setText("IP: لا يمكن جلب العنوان");
        }
    }

    private void stopServer() {
        if (smsServer != null && isServerRunning) {
            smsServer.stop();
            isServerRunning = false;
            logMessage("🔴 تم إيقاف الخادم المحلي.");
        }
    }

    private void sendSmsMessage(String phoneNumber, String messageContent) {
        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(phoneNumber, null, messageContent, null, null);
            successCounter++;
            tvSuccessCount.setText(String.valueOf(successCounter));
            logMessage("✅ تم إرسال الرسالة بنجاح إلى: " + phoneNumber);
        } catch (Exception e) {
            failedCounter++;
            tvFailedCount.setText(String.valueOf(failedCounter));
            logMessage("❌ فشل إرسال الرسالة إلى: " + phoneNumber);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateIpDisplay();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopServer();
    }
}
