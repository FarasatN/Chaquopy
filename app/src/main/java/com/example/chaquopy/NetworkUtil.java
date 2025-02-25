package com.example.chaquopy;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.util.Log;

public class NetworkUtil extends BroadcastReceiver {
    private static final String TAG = "WifiStatusReceiver";
    public static String getNetworkType(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) {
            return "NO_NETWORK";
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Use the newer APIs for API 23 and above.
            Network activeNetwork = cm.getActiveNetwork();
            if (activeNetwork == null) {
                return "NO_NETWORK";
            }
            NetworkCapabilities capabilities = cm.getNetworkCapabilities(activeNetwork);
            if (capabilities == null) {
                return "NO_NETWORK";
            }
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                return "WIFI";
            } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                return "MOBILE";
            } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                return "ETHERNET";
            } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH)) {
                return "BLUETOOTH";
            } else {
                return "OTHER";
            }
        } else {
            // Fallback for devices running API levels below 23
            NetworkInfo activeNetworkInfo = cm.getActiveNetworkInfo();
            if (activeNetworkInfo != null && activeNetworkInfo.isConnected()) {
                int type = activeNetworkInfo.getType();
                if (type == ConnectivityManager.TYPE_WIFI) {
                    return "WIFI";
                } else if (type == ConnectivityManager.TYPE_MOBILE) {
                    return "MOBILE";
                } else if (type == ConnectivityManager.TYPE_ETHERNET) {
                    return "ETHERNET";
                } else {
                    return "OTHER";
                }
            }
            return "NO_NETWORK";
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String networkType = getNetworkType(context.getApplicationContext());
        if (!"WIFI".equals(networkType)) {
            Log.v(TAG, "Wi-Fi is disabled or not connected.");
        } else {
            Log.v(TAG, "Wi-Fi is connected.");
        }
    }
}
