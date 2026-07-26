package com.smart.link;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;

public class ServerService extends Service {

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // إشعار دائم عند تشغيل الخادم
        Notification notification = new NotificationCompat.Builder(this, "server_channel")
                .setContentTitle("برنامج الربط الذكي")
                .setContentText("السيرفر قيد العمل")
                .setSmallIcon(R.mipmap.ic_launcher) // استخدام أيقونة التطبيق
                .setOngoing(true)
                .build();

        // تشغيل الخدمة في المقدمة مع إشعار
        startForeground(1, notification);

        // هنا ضع كود تشغيل السيرفر الفعلي
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // هنا ضع كود إيقاف السيرفر إذا لزم
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    "server_channel",
                    "Server Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }
}
