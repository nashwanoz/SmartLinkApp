package com.smart.link;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.telephony.SmsManager;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import java.io.IOException;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

public class ServerService extends Service {

    private static final String CHANNEL_ID = "smart_link_server";
    private static final int NOTIFICATION_ID = 1;
    private static final String TAG = "ServerService";

    private SmsServer server;
    private int serverPort;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        serverPort = intent.getIntExtra("port", 8080);

        // إشعار دائم
        Notification notification = buildNotification();
        startForeground(NOTIFICATION_ID, notification);

        // تشغيل السيرفر
        try {
            if (server != null) {
                server.stop();
            }
            server = new SmsServer(serverPort);
            server.start();
            broadcastLog("✅ السيرفر يعمل على المنفذ: " + serverPort);
            Log.d(TAG, "Server started on port " + serverPort);
        } catch (IOException e) {
            broadcastLog("❌ فشل تشغيل السيرفر: " + e.getMessage());
            Log.e(TAG, "Failed to start server", e);
            stopSelf();
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (server != null) {
            server.stop();
            server = null;
        }
        broadcastLog("🔴 السيرفر متوقف");
        Log.d(TAG, "Server stopped");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // ==================== الإشعار ====================

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Smart Link Server",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("إشعار دائم عند تشغيل خادم الرسائل");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification buildNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent,
                PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("الربط الذكي - السيرفر يعمل")
                .setContentText("المنفذ: " + serverPort + " | جاري استقبال الطلبات...")
                .setSmallIcon(R.mipmap.ic_launcher_foreground)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    // ==================== بث التحديثات للواجهة ====================

    private void broadcastLog(String message) {
        Intent intent = new Intent("com.smart.link.SERVER_LOG");
        intent.putExtra("message", message);
        sendBroadcast(intent);
    }

    private void broadcastSmsResult(boolean success, String phone) {
        Intent intent = new Intent("com.smart.link.SMS_SENT");
        intent.putExtra("success", success);
        intent.putExtra("phone", phone);
        sendBroadcast(intent);
    }

    // ==================== HTTP Server ====================

    private class SmsServer extends NanoHTTPD {

        public SmsServer(int port) {
            super(port);
        }

        @Override
        public Response serve(IHTTPSession session) {
            String uri = session.getUri();
            Map<String, String> params = session.getParms();

            Log.d(TAG, "Request: " + uri + " | Params: " + params);

            // Endpoint: /send?phone=xxx&message=yyy
            if (uri.equals("/send")) {
                String phone = params.get("phone");
                String message = params.get("message");

                if (phone == null || message == null || phone.isEmpty() || message.isEmpty()) {
                    broadcastLog("⚠️ طلب غير صالح: phone أو message مفقود");
                    return newFixedLengthResponse(
                            Response.Status.BAD_REQUEST,
                            "text/plain",
                            "Error: Missing phone or message parameter"
                    );
                }

                // فك تشفير URL
                try {
                    phone = java.net.URLDecoder.decode(phone, "UTF-8");
                    message = java.net.URLDecoder.decode(message, "UTF-8");
                } catch (Exception e) {
                    Log.e(TAG, "Decode error", e);
                }

                // إرسال SMS
                boolean sent = sendSms(phone, message);

                if (sent) {
                    broadcastLog("📤 تم استقبال وإرسال رسالة إلى: " + phone);
                    return newFixedLengthResponse(
                            Response.Status.OK,
                            "text/plain",
                            "Success: SMS sent to " + phone
                    );
                } else {
                    broadcastLog("❌ فشل إرسال الرسالة إلى: " + phone);
                    return newFixedLengthResponse(
                            Response.Status.INTERNAL_ERROR,
                            "text/plain",
                            "Error: Failed to send SMS"
                    );
                }
            }

            // Endpoint: /status
            if (uri.equals("/status")) {
                return newFixedLengthResponse(
                        Response.Status.OK,
                        "text/plain",
                        "Smart Link Server is running on port " + serverPort
                );
            }

            // أي endpoint آخر
            return newFixedLengthResponse(
                    Response.Status.NOT_FOUND,
                    "text/plain",
                    "Not Found"
            );
        }
    }

    // ==================== إرسال SMS ====================

    private boolean sendSms(String phoneNumber, String message) {
        try {
            SmsManager smsManager = SmsManager.getDefault();

            // تقسيم الرسالة الطويلة
            java.util.ArrayList<String> parts = smsManager.divideMessage(message);

            if (parts.size() > 1) {
                // رسالة طويلة — إرسالها متعددة الأجزاء
                smsManager.sendMultipartTextMessage(
                        phoneNumber,
                        null,
                        parts,
                        null,
                        null
                );
            } else {
                // رسالة قصيرة — إرسال مباشر
                smsManager.sendTextMessage(
                        phoneNumber,
                        null,
                        message,
                        null,
                        null
                );
            }

            broadcastSmsResult(true, phoneNumber);
            return true;

        } catch (Exception e) {
            Log.e(TAG, "SMS send failed", e);
            broadcastSmsResult(false, phoneNumber);
            return false;
        }
    }
}
