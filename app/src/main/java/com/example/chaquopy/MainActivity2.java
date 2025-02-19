package com.example.chaquopy;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;


public class MainActivity2 extends AppCompatActivity {
    private FilePermissionHelper permissionHelper;
    private static final String TAG = "MainActivity";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        permissionHelper = new FilePermissionHelper(this);
        permissionHelper.requestDownloadFolderAccess();
        String networkType = NetworkUtil.getNetworkType(this);
        Log.d(TAG, "Current Network Type: " + networkType);

        if ("WIFI".equals(networkType)) {
            Log.v(TAG, "Connected via Wi-Fi");
            System.out.println("Connected via Wi-Fi");
            // Perform Wi-Fi specific actions
            //python code here
            // "context" must be an Activity, Service or Application object from your app.
            if (!Python.isStarted()) {
                Python.start(new AndroidPlatform(getApplicationContext()));
            }
            new Thread(new Runnable() {
                @Override
                public void run() {
                    // Call your Python function here via Chaquopy.
                    Python py = Python.getInstance();
                    PyObject myModule = py.getModule("app");
                    myModule.callAttr("main");
                    // If you need to update the UI with the result, post to the main thread.
                    runOnUiThread(() -> {
                        // Update UI here
                        setContentView(R.layout.activity_main);
                    });
                }
            }).start();

        } else if ("MOBILE".equals(networkType)) {
            Log.v(TAG, "Connected via Mobile Data");
            System.out.println("Connected via Mobile Data");
            // Perform Mobile data specific action
        } else if ("OTHER".equals(networkType)) {
            Log.v(TAG, "Connected via Other Network Type");
            System.out.println("Connected via Other Network Type");
            // Handle other network types if needed
        } else if ("NO_NETWORK".equals(networkType)) {
            Log.v(TAG, "No Network Connection");
            System.out.println("No Network Connection");
            // Handle no network connection scenario (e.g., show error message, disable network-dependent features)
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        permissionHelper.handlePermissionResult(requestCode);
    }



}