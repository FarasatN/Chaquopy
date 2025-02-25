// ConnectivityChanger.java
package com.example.chaquopy; // Replace with your actual package name

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;


public class ConnectivityChanger {

    private static final String TAG = "ConnectivityChanger";
    private Context context;
    private ConnectivityChangeListener listener;
    private ConnectivityBroadcastReceiver broadcastReceiver;

    public interface ConnectivityChangeListener {
        void onConnectivityChanged(boolean isConnected, String networkType);
    }

    public ConnectivityChanger(Context context, ConnectivityChangeListener listener) {
        this.context = context.getApplicationContext(); // Use application context to avoid memory leaks
        this.listener = listener;
        this.broadcastReceiver = new ConnectivityBroadcastReceiver();
    }

    public void startMonitoring() {
        IntentFilter intentFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        context.registerReceiver(broadcastReceiver, intentFilter);
        Log.d(TAG, "Connectivity monitoring started (BroadcastReceiver).");
    }

    public void stopMonitoring() {
        try {
            context.unregisterReceiver(broadcastReceiver);
            Log.d(TAG, "Connectivity monitoring stopped (BroadcastReceiver).");
        } catch (IllegalArgumentException e) {
            // Receiver was not registered or already unregistered, ignore exception
            Log.w(TAG, "Receiver already unregistered or not registered: " + e.getMessage());
        }
    }

    public boolean isConnected() {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnected();
    }

    public String getNetworkType() {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return "No Network Service";

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if (activeNetwork != null && activeNetwork.isConnected()) {
            int type = activeNetwork.getType();
            switch (type) {
                case ConnectivityManager.TYPE_WIFI:
                    return "WIFI";
                case ConnectivityManager.TYPE_MOBILE:
                    return "MOBILE";
                case ConnectivityManager.TYPE_ETHERNET:
                    return "ETHERNET";
                case ConnectivityManager.TYPE_BLUETOOTH:
                    return "BLUETOOTH";
                case ConnectivityManager.TYPE_WIMAX:
                    return "WIMAX";
                case ConnectivityManager.TYPE_VPN:
                    return "VPN";
                default:
                    return "UNKNOWN";
            }
        }
        return "No Connection";
    }


    private class ConnectivityBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
                boolean connected = isConnected();
                String networkType = getNetworkType();
                Log.d(TAG, "Connectivity changed (BroadcastReceiver) - Connected: " + connected + ", Type: " + networkType);
                if (listener != null) {
                    listener.onConnectivityChanged(connected, networkType);
                }
            }
        }
    }

    // Static WORKER_TAG for consistency in logs (if needed for WorkManager context) - Not used in this example, but kept for potential future use
    public static final String WORKER_TAG = "ConnectivityWorker";
}