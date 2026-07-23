package com.smart.link;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private TextView tvServerStatus, tvIpAddress, tvPortNumber, tvSentCount;
    private Button btnToggleServer;
    private boolean isServerRunning = false;

    private ServerSocket serverSocket;
    private Thread serverThread;
    private final int SERVER_PORT = 8080; 
    private int messageCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // ربط العناصر البرمجية بالواجهة
        tvServerStatus = findViewById(R.id.tvServerStatus);
        tvIpAddress = findViewById(R.id.tvIpAddress);
        tvPortNumber = findViewById(R.id.tvPortNumber);
        tvSentCount = findViewById(R.id.tvSentCount);
        btnToggleServer = findViewById(R.id.btnToggleServer);

        // فحص وطلب صلاحية إرسال الرسائل فور فتح التطبيق
        checkPermissions();

        // إدخال نصوص الواجهة وتجنب قيم الـ Null
        if (tvIpAddress != null) {
            tvIpAddress.setText("عنوان IP: " + getDeviceIpAddress());
        }
        if (tvPortNumber != null) {
            tvPortNumber.setText("رقم البورت: " + SERVER_PORT);
        }
        
        updateMessageCount();

        if (btnToggleServer != null) {
            btnToggleServer.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (isTrialExpired()) {
                        Toast.makeText(MainActivity.this, "عذراً، انتهت صلاحية الفترة التجريبية للبرنامج!", Toast.LENGTH_LONG).show();
                        return;
                    }

                    if (!isServerRunning) {
                        startServer();
                    } else {
                        stopServer();
                    }
                }
            });
        }
    }

    private void startServer() {
        isServerRunning = true;
        if (tvServerStatus != null) {
            tvServerStatus.setText("حالة السيرفر: يعمل الآن 🟢");
            tvServerStatus.setTextColor(0xFF4CAF50);
        }
        if (btnToggleServer != null) {
            btnToggleServer.setText("إيقاف السيرفر");
        }
        Toast.makeText(MainActivity.this, "تم تشغيل الخادم بنجاح", Toast.LENGTH_SHORT).show();

        serverThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    serverSocket = new ServerSocket(SERVER_PORT);
                    while (isServerRunning) {
                        final Socket socket = serverSocket.accept();
                        handleClientRequest(socket);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        serverThread.start();
    }

    private void stopServer() {
        isServerRunning = false;
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (serverThread != null) {
            serverThread.interrupt();
        }
        if (tvServerStatus != null) {
            tvServerStatus.setText("حالة السيرفر: متوقف 🔴");
            tvServerStatus.setTextColor(0xFFFF5252);
        }
        if (btnToggleServer != null) {
            btnToggleServer.setText("تشغيل السيرفر");
        }
        Toast.makeText(this, "تم إيقاف السيرفر", Toast.LENGTH_SHORT).show();
    }

    private void handleClientRequest(final Socket socket) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    String requestLine = reader.readLine();
                    if (requestLine == null) return;

                    String[] requestParts = requestLine.split(" ");
                    String responseText = "Missing params. Use: /send?phone=X&message=Y";

                    if (requestParts.length > 1) {
                        String urlPath = requestParts[1]; 
                        if (urlPath.startsWith("/send")) {
                            int queryIndex = urlPath.indexOf("?");
                            String queryString = (queryIndex != -1) ? urlPath.substring(queryIndex + 1) : "";
                            Map<String, String> params = parseQuery(queryString);

                            String phone = params.get("phone");
                            String message = params.get("message");

                            if (phone != null && message != null) {
                                try {
                                    SmsManager smsManager = SmsManager.getDefault();
                                    smsManager.sendTextMessage(phone, null, message, null, null);
                                    responseText = "Success: Message sent to " + phone;

                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            messageCount++;
                                            updateMessageCount();
                                        }
                                    });
                                } catch (Exception e) {
                                    responseText = "Error: " + e.getMessage();
                                }
                            }
                        }
                    }

                    OutputStream output = socket.getOutputStream();
                    byte[] responseBytes = responseText.getBytes("UTF-8");
                    
                    output.write("HTTP/1.1 200 OK\r\n".getBytes());
                    output.write("Content-Type: text/plain; charset=utf-8\r\n".getBytes());
                    output.write(("Content-Length: " + responseBytes.length + "\r\n").getBytes());
                    output.write("\r\n".getBytes());
                    output.write(responseBytes);
                    output.flush();
                    
                    socket.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private Map<String, String> parseQuery(String query) {
        Map<String, String> result = new HashMap<>();
        if (query == null || query.isEmpty()) return result;
        try {
            for (String param : query.split("&")) {
                String[] pair = param.split("=");
                if (pair.length > 1) {
                    String key = pair[0]; 
                    String value = URLDecoder.decode(pair[1], "UTF-8"); 
                    result.put(key, value);
                } else if (pair.length > 0) {
                    result.put(pair[0], ""); 
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    private void updateMessageCount() {
        if (tvSentCount != null) {
            tvSentCount.setText("الرسائل المستقبلة والمرسلة: " + messageCount);
        }
    }

    private boolean isTrialExpired() {
        Calendar expiryDate = Calendar.getInstance();
        expiryDate.set(2026, Calendar.DECEMBER, 31);
        Calendar today = Calendar.getInstance();
        return today.after(expiryDate);
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, 100);
        }
    }

    private String getDeviceIpAddress() {
        try {
            WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wifiManager != null && wifiManager.isWifiEnabled() && wifiManager.getConnectionInfo() != null) {
                int ipAddress = wifiManager.getConnectionInfo().getIpAddress();
                if (ipAddress != 0) {
                    return String.format(Locale.getDefault(), "%d.%d.%d.%d",
                            (ipAddress & 0xff),
                            (ipAddress >> 8 & 0xff),
                            (ipAddress >> 16 & 0xff),
                            (ipAddress >> 24 & 0xff));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "127.0.0.1 (يرجى الاتصال بشبكة واي فاي)";
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopServer();
    }
}
