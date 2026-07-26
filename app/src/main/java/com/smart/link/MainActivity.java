package com.smart.link;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.telephony.SmsManager;
import android.text.format.Formatter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final String PREFS_NAME = "SmartLinkPrefs";
    private static final String KEY_SUCCESS_COUNT = "success_count";
    private static final String KEY_FAILED_COUNT = "failed_count";

    private ImageView imgLogo;
    private TextView tvServerIp, tvMacAddress, tvSuccessCount, tvFailedCount, tvLogs;
    private EditText etServerPort;
    private Button btnToggleServer, btnDeveloper, btnSettings;

    private boolean serverRunning = false;
    private long backPressedTime;
    private SharedPreferences prefs;

    // BroadcastReceiver لاستقبال التحديثات من الخدمة
    private BroadcastReceiver updateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("com.smart.link.SMS_SENT".equals(action)) {
                boolean success = intent.getBooleanExtra("success", false);
                String phone = intent.getStringExtra("phone");
                updateCounters(success);
                addLog(success ? "✅ تم الإرسال إلى: " + phone : "❌ فشل الإرسال إلى: " + phone);
            } else if ("com.smart.link.SERVER_LOG".equals(action)) {
                String log = intent.getStringExtra("message");
                addLog(log);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // تهيئة SharedPreferences
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // ربط العناصر
        imgLogo = findViewById(R.id.imgLogo);
        tvServerIp = findViewById(R.id.tvServerIp);
        tvMacAddress = findViewById(R.id.tvMacAddress);
        tvSuccessCount = findViewById(R.id.tvSuccessCount);
        tvFailedCount = findViewById(R.id.tvFailedCount);
        tvLogs = findViewById(R.id.tvLogs);
        etServerPort = findViewById(R.id.etServerPort);
        btnToggleServer = findViewById(R.id.btnToggleServer);
        btnDeveloper = findViewById(R.id.btnDeveloper);
        btnSettings = findViewById(R.id.btnSettings);

        // عرض IP و MAC
        displayNetworkInfo();

        // تحميل العدادات المحفوظة
        loadCounters();

        // تسجيل BroadcastReceiver
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.smart.link.SMS_SENT");
        filter.addAction("com.smart.link.SERVER_LOG");
        registerReceiver(updateReceiver, filter);

        // زر تشغيل/إيقاف الخادم
        btnToggleServer.setOnClickListener(v -> {
            if (!serverRunning) {
                if (checkAndRequestPermissions()) {
                    startServer();
                }
            } else {
                stopServer();
            }
        });

        // زر المطور
        btnDeveloper.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, DeveloperActivity.class));
        });

        // زر الإعدادات
        btnSettings.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, SettingsActivity.class));
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(updateReceiver);
    }

    // ==================== عرض معلومات الشبكة ====================

    private void displayNetworkInfo() {
        // عرض IP
        String ip = getDeviceIpAddress();
        tvServerIp.setText(ip != null ? ip : "غير متوفر");

        // عرض MAC Address
        String mac = getMacAddress();
        tvMacAddress.setText(mac != null ? mac : "غير متوفر");
    }

    private String getDeviceIpAddress() {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress() && addr.getHostAddress().indexOf(':') < 0) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        // احتياطي: WiFi Manager
        try {
            WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wifiManager != null) {
                int ipInt = wifiManager.getConnectionInfo().getIpAddress();
                return Formatter.formatIpAddress(ipInt);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private String getMacAddress() {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                if (intf.getName().equalsIgnoreCase("wlan0") || intf.getName().equalsIgnoreCase("eth0")) {
                    byte[] macBytes = intf.getHardwareAddress();
                    if (macBytes == null) return null;
                    StringBuilder sb = new StringBuilder();
                    for (byte b : macBytes) {
                        sb.append(String.format("%02X:", b));
                    }
                    if (sb.length() > 0) sb.deleteCharAt(sb.length() - 1);
                    return sb.toString();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        // احتياطي: WiFi Manager (قد يُرجع 02:00:00:00:00:00 على Android 6+)
        try {
            WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wifiManager != null) {
                WifiInfo info = wifiManager.getConnectionInfo();
                return info.getMacAddress();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // ==================== الصلاحيات ====================

    private boolean checkAndRequestPermissions() {
        String[] permissions = {
                Manifest.permission.SEND_SMS,
                Manifest.permission.INTERNET,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.FOREGROUND_SERVICE
        };

        // إضافة POST_NOTIFICATIONS لـ Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions = new String[]{
                    Manifest.permission.SEND_SMS,
                    Manifest.permission.INTERNET,
                    Manifest.permission.ACCESS_NETWORK_STATE,
                    Manifest.permission.ACCESS_WIFI_STATE,
                    Manifest.permission.FOREGROUND_SERVICE,
                    Manifest.permission.POST_NOTIFICATIONS
            };
        }

        boolean allGranted = true;
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }

        if (!allGranted) {
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
            return false;
        }

        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                startServer();
            } else {
                new AlertDialog.Builder(this)
                        .setTitle("صلاحيات مطلوبة")
                        .setMessage("التطبيق يحتاج صلاحيات SMS والشبكة ليعمل بشكل صحيح. هل تريد الذهاب إلى الإعدادات؟")
                        .setPositiveButton("الإعدادات", (dialog, which) -> {
                            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            intent.setData(android.net.Uri.parse("package:" + getPackageName()));
                            startActivity(intent);
                        })
                        .setNegativeButton("إلغاء", null)
                        .show();
            }
        }
    }

    // ==================== تشغيل/إيقاف السيرفر ====================

    private void startServer() {
        String portStr = etServerPort.getText().toString().trim();
        if (portStr.isEmpty()) {
            Toast.makeText(this, "الرجاء إدخال رقم المنفذ", Toast.LENGTH_SHORT).show();
            return;
        }

        int port;
        try {
            port = Integer.parseInt(portStr);
            if (port < 1024 || port > 65535) {
                Toast.makeText(this, "المنفذ يجب أن يكون بين 1024 و 65535", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "رقم المنفذ غير صالح", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent serviceIntent = new Intent(this, ServerService.class);
        serviceIntent.putExtra("port", port);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }

        btnToggleServer.setText("إيقاف الخادم");
        btnToggleServer.setBackgroundColor(Color.parseColor("#4CAF50"));
        serverRunning = true;
        addLog("🟢 تم تشغيل الخادم على المنفذ: " + port);
        Toast.makeText(this, "تم تشغيل الخادم", Toast.LENGTH_SHORT).show();
    }

    private void stopServer() {
        Intent serviceIntent = new Intent(this, ServerService.class);
        stopService(serviceIntent);

        btnToggleServer.setText("تشغيل الخادم");
        btnToggleServer.setBackgroundColor(Color.parseColor("#D32F2F"));
        serverRunning = false;
        addLog("🔴 تم إيقاف الخادم");
        Toast.makeText(this, "تم إيقاف الخادم", Toast.LENGTH_SHORT).show();
    }

    // ==================== العدادات ====================

    private void loadCounters() {
        int success = prefs.getInt(KEY_SUCCESS_COUNT, 0);
        int failed = prefs.getInt(KEY_FAILED_COUNT, 0);
        tvSuccessCount.setText(String.valueOf(success));
        tvFailedCount.setText(String.valueOf(failed));
    }

    private void updateCounters(boolean success) {
        SharedPreferences.Editor editor = prefs.edit();
        if (success) {
            int count = prefs.getInt(KEY_SUCCESS_COUNT, 0) + 1;
            editor.putInt(KEY_SUCCESS_COUNT, count);
            tvSuccessCount.setText(String.valueOf(count));
        } else {
            int count = prefs.getInt(KEY_FAILED_COUNT, 0) + 1;
            editor.putInt(KEY_FAILED_COUNT, count);
            tvFailedCount.setText(String.valueOf(count));
        }
        editor.apply();
    }

    // ==================== السجل ====================

    public void addLog(String message) {
        String current = tvLogs.getText().toString();
        String timestamp = new java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
                .format(new java.util.Date());
        tvLogs.setText(timestamp + " - " + message + "\n" + current);
    }

    // ==================== تأكيد الخروج ====================

    @Override
    public void onBackPressed() {
        if (backPressedTime + 2000 > System.currentTimeMillis()) {
            super.onBackPressed();
        } else {
            Toast.makeText(this, "اضغط مرة أخرى للخروج", Toast.LENGTH_SHORT).show();
        }
        backPressedTime = System.currentTimeMillis();
    }
}
