package com.smart.link;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    private CheckBox cbNotifications, cbAutoStart;
    private Button btnSaveSettings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // ربط العناصر البرمجية بالواجهة XML
        cbNotifications = findViewById(R.id.cbNotifications);
        cbAutoStart = findViewById(R.id.cbAutoStart);
        btnSaveSettings = findViewById(R.id.btnSaveSettings);

        // حدث الضغط على زر حفظ الإعدادات
        btnSaveSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // هنا يمكنك حفظ الإعدادات مستقبلاً باستخدام SharedPreferences
                Toast.makeText(SettingsActivity.this, "تم حفظ الإعدادات بنجاح", Toast.LENGTH_SHORT).show();
                finish(); // العودة للشاشة الرئيسية
            }
        });
    }
}
