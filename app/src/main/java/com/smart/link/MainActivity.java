package com.smart.link;

import android.Manifest;
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
    
    // ✅ تم تعديل الأسماء لتتوافق مع activity_main.xml
    private TextView tvServerIp, tvLogs, tvSuccessCount, tvFailedCount;
    private EditText etServerPort;
    private Button btnToggleServer;
    
    private MySmsServer smsServer;
    private boolean isServerRunning = false;
    
    private int successCounter = 0;
    private int failedCounter = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // ✅ الأسماء مطابقة لـ IDs في activity_main.xml
        tvServerIp = findViewById(R.id.tvServerIp);
        tvLogs = findViewById(R.id.tvLogs);
        tvSuccessCount = findViewById(R.id.tvSuccessCount);
        tvFailedCount = findViewById(R.id.tvFailedCount);
        etServerPort = findViewById(R.id.etServerPort);
        btnToggleServer = findViewById(R.id.btnToggleServer);

        checkSmsPermission();
        updateIpDisplay();

        btnToggleServer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isServerRunning) {
                    stopServer();
                } else {
                    startServer();
                }
            }
        });
    }

    private void updateIpDisplay() {
        String ipAddress = getWifiIpAddress();
        tvServerIp.setText("http://" + ipAddress);
    }

    private void startServer() {
        String portStr = etServerPort.getText().toString().trim();
        if (portStr.isEmpty()) {
            Toast.makeText(this, "الرجاء إدخال رقم المنفذ (Port)", Toast.LENGTH_SHORT).show();
            return;
        }
        
        int port = Integer.parseInt(portStr);
        try {
            smsServer = new MySmsServer(port);
            smsServer.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
            isServerRunning = true;
            
            etServerPort.setEnabled(false);
            btnToggleServer.setText("إيقاف السيرفر");
            btnToggleServer.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
            logMessage("✅ تم تشغيل السيرفر على المنفذ [" + port + "] وهو مستعد للاستقبال...");
        } catch (IOException e) {
            logMessage("❌ فشل تشغيل السيرفر: " + e.getMessage());
        }
    }

    private void stopServer() {
        if (smsServer != null) {
            smsServer.stop();
            isServerRunning = false;
            
            etServerPort.setEnabled(true);
            btnToggleServer.setText("تشغيل السيرفر");
            btnToggleServer.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));
            logMessage("🛑 تم إيقاف السيرفر يدوياً.");
        }
    }

    private String getWifiIpAddress() {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        int ipAddress = wifiManager.getConnectionInfo().getIpAddress();
        if (ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN)) {
            ipAddress = Integer.reverseBytes(ipAddress);
        }
        byte[] ipByteArray = BigInteger.valueOf(ipAddress).toByteArray();
        String ipAddressString;
        try {
            ipAddressString = InetAddress.getByAddress(ipByteArray).getHostAddress();
        } catch (UnknownHostException ex) {
            ipAddressString = "0.0.0.0";
        }
        return ipAddressString;
    }

    private void logMessage(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvLogs.append(message + "\n");
            }
        });
    }

    public void sendSmsMessage(String phoneNumber, String messageText) {
        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(phoneNumber, null, messageText, null, null);
            
            successCounter++;
            tvSuccessCount.setText(String.valueOf(successCounter));
            logMessage("🚀 [تم الإرسال بنجاح] للرقم: " + phoneNumber);
        } catch (Exception e) {
            failedCounter++;
            tvFailedCount.setText(String.valueOf(failedCounter));
            logMessage("❌ [فشل الإرسال] للرقم: " + phoneNumber + " السبب: " + e.getMessage());
        }
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
                        logMessage("📥 طلب وارد من Node.js لإرسال رسالة إلى: " + phone);
                        
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                sendSmsMessage(phone, message);
                            }
                        });
                        
                        return newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, "Success");
                    }
                } catch (Exception e) {
                    logMessage("❌ خطأ في فك ترميز نصوص الرسالة الواردة.");
                }
            }
            return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "Invalid Request");
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
