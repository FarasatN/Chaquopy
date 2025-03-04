package com.example.chaquopy;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity { // Removed "implements ConnectivityChanger.ConnectivityChangeListener"
    public FilePermissionHelper permissionHelper;
    private static final String TAG = "Auto Upload";
    private static final String channelId = "i.apps.notifications"; // Unique channel ID for notifications
    private static final String description = "Auto Upload notification";  // Description for the notification channel
    private static final int notificationId = 1234; // Unique identifier for the notification
    private static int scheduleIntervalInMinutes = 240;
    private static int setInitialDelayInSeconds = 10; //initial delay when work manager started

    public static boolean isWifiConnected(@NonNull Context context) { // Made static and public
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        NetworkCapabilities capabilities = cm.getNetworkCapabilities(cm.getActiveNetwork());
        return capabilities != null && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
//        } else { // Below Android M
//            NetworkInfo activeNetworkInfo = cm.getActiveNetworkInfo();
//            return activeNetworkInfo != null && activeNetworkInfo.getType() == ConnectivityManager.TYPE_WIFI && activeNetworkInfo.isConnected();
//        }

    }

//    public void restartApp() {
//        Intent intent = new Intent(this, MainActivity.class);
//
////        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
////        finishAffinity(); // Close all existing activities
////        startActivity(intent);
////        // Add this for immediate process kill (optional)
////        System.exit(0);
//        //------------
//        PendingIntent pendingIntent = PendingIntent.getActivity(
//                this,
//                0,
//                intent,
//                PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE
//        );
//        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
//        alarmManager.set(AlarmManager.RTC, System.currentTimeMillis() + 100, pendingIntent);
//        // Terminate current process
//        android.os.Process.killProcess(android.os.Process.myPid());
//        System.exit(0);
//    }

//    @Override
//    protected void onRestart() {
//        super.onRestart();
//        // Network check and scheduling
//        if (isWifiConnected(this)) {
//            sendNotification(this, "WIFI connected, upload processing..");
//            Log.v(TAG, "WIFI connected, upload processing..");
//            //System.out.println("WIFI connected, upload processing..");
//            schedulePeriodicWork();
//            Log.v(TAG, "Periodic task scheduled");
//            //System.out.println("Periodic task scheduled");
//        } else {
//            sendNotification(this, "WIFI disconnected, upload stopped!");
//            Log.v(TAG, "WIFI disconnected, upload stopped!");
//            //System.out.println("WIFI disconnected, upload stopped!");
//        }
//    }

    private boolean loadConfigFromJson() {
        File configFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "/automate/config.json");
        File serviceAcoountFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "/automate/service_account.json");
        StringBuilder jsonContent = new StringBuilder();

        if (!serviceAcoountFile.exists()) {
            Log.e(TAG, "'service_account.json' file not found! ");
            Toast.makeText(getApplicationContext(), "'service_account.json' file not found! ", Toast.LENGTH_LONG).show();
            return false;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(configFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                jsonContent.append(line);
            }
            JSONObject configJson = new JSONObject(jsonContent.toString());
            if (configJson.has("scheduleIntervalInMinutes") && configJson.has("setInitialDelayInSeconds")) {
                scheduleIntervalInMinutes = configJson.getInt("scheduleIntervalInMinutes");
                setInitialDelayInSeconds = configJson.getInt("setInitialDelayInSeconds");
                Log.w(TAG, "scheduleIntervalInMinutes loaded from config.json: " + scheduleIntervalInMinutes + " minutes");
                Log.w(TAG, "setInitialDelayInSeconds loaded from config.json: " + setInitialDelayInSeconds + " seconds");
            } else {
                Log.w(TAG, "scheduleIntervalInMinutes key not found in config.json, using default: " + scheduleIntervalInMinutes + " minutes");
                Log.w(TAG, "setInitialDelayInSeconds key not found in config.json, using default: " + setInitialDelayInSeconds + " seconds");
            }

            return true;
        } catch (IOException e) {
            Log.e(TAG, "Error reading config.json: " + e.getMessage());
            Log.e(TAG, "Using default scheduleIntervalInMinutes: " + scheduleIntervalInMinutes + " minutes");
            Log.e(TAG, "Using default setInitialDelayInSeconds: " + setInitialDelayInSeconds + " seconds");
            Toast.makeText(getApplicationContext(), "Error reading config.json: " + e.getMessage(), Toast.LENGTH_LONG).show();
            return false;
        } catch (org.json.JSONException e) {
            Log.e(TAG, "Error parsing config.json: " + e.getMessage());
            Toast.makeText(getApplicationContext(), "Error parsing config.json: " + e.getMessage(), Toast.LENGTH_LONG).show();
            return false;
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        setContentView(R.layout.activity_main);

        permissionHelper = new FilePermissionHelper(this);

        permissionHelper.requestDownloadFolderAccess();
        // Check permissions immediately in case already granted
        if (permissionHelper.hasAllPermissions()) {

            startAppProcess();
        }
    }

    // Handle permission results
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == FilePermissionHelper.STORAGE_PERMISSION_CODE) {
            permissionHelper.handlePermissionResult(requestCode);
            if (permissionHelper.hasAllPermissions()) {
                startAppProcess();
            }
        }
    }

    private void startAppProcess() {
        // Check notifications
        if (!NotificationManagerCompat.from(this).areNotificationsEnabled()) {
            promptEnableNotifications(this);
        }
        createNotificationChannel();
//        restartApp();
        // Network check and scheduling
//        if (isWifiConnected(this)) {
//            sendNotification(this, "WIFI connected, upload processing..");
//            Log.v(TAG, "WIFI connected, upload processing..");
//            //System.out.println("WIFI connected, upload processing..");
//            Log.v(TAG, "Periodic task scheduled");
//            //System.out.println("Periodic task scheduled");
//        } else {
//            sendNotification(this, "WIFI disconnected, upload stopped!");
//            Log.v(TAG, "WIFI disconnected, upload stopped!");
//            //System.out.println("WIFI disconnected, upload stopped!");
//        }

        boolean isConfigFileExists = loadConfigFromJson(); // Load scheduleInterval from JSON
        Log.v(TAG, "scheduleIntervalInMinutes : " + scheduleIntervalInMinutes);
        Log.v(TAG, "setInitialDelayInSeconds : " + setInitialDelayInSeconds);

        if (isConfigFileExists) {
            schedulePeriodicWork();
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    // Code to be executed after the delay (1 second) goes here
                    // Example:
                    // Toast.makeText(MainActivity.this, "Delay finished!", Toast.LENGTH_SHORT).show();
                    // Or perform some UI update
                    Toast.makeText(getApplicationContext(), "App scheduled in every " + scheduleIntervalInMinutes + " minutes with " + setInitialDelayInSeconds + " seconds initial delay!", Toast.LENGTH_LONG).show();
                    Log.v(TAG, "App scheduled in every " + scheduleIntervalInMinutes + " minutes with " + setInitialDelayInSeconds + " seconds initial delay!");

                }
            }, 5000); // Delay in milliseconds (1 second)

        } else {
//            finish();
            this.finishAffinity();
        }

    }

    private void schedulePeriodicWork() {

        PeriodicWorkRequest driveSyncWorkRequest =
                new PeriodicWorkRequest.Builder(DriveSyncWorker.class, scheduleIntervalInMinutes, TimeUnit.MINUTES)
                        .setConstraints(Constraints.NONE) // You can add constraints if needed (e.g., network type, charging)
                        .setInitialDelay(setInitialDelayInSeconds, TimeUnit.SECONDS)
                        .build();
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "DriveSyncWorker", // Unique name for the periodic work
                ExistingPeriodicWorkPolicy.KEEP, // If work with the same name exists, keep the existing one
                driveSyncWorkRequest
        );
    }

    // Worker class to perform Wi-Fi check and execute Python code in the background
    public static class DriveSyncWorker extends Worker {
        public DriveSyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
            super(context, workerParams);

        }

        @NonNull
        @Override
        public Result doWork() {
            try {
                if (isWifiConnected(this.getApplicationContext())) {

                    // --- Notification and Logging for Wi-Fi connected ---
                    sendNotification(this.getApplicationContext(), "WIFI connected, upload processing..");
                    Log.v(TAG, "WIFI connected, upload processing..");
                    //System.out.println("WIFI connected, upload processing..");

                    executePythonMain();

                    sendNotification(this.getApplicationContext(), "Upload finished! Please, check the log file for details!");
                    Log.v(TAG, "Upload finished! Please, check the log file for details!");
                    //System.out.println("Upload finished! Please, check the log file for details!");

                    return Result.success();

                } else {
                    // --- Notification and Logging for No Wi-Fi ---
                    sendNotification(this.getApplicationContext(), "WIFI disconnected, upload stopped!"); // Optional notification for Wi-Fi disconnect from worker
                    Log.v(TAG, "WIFI disconnected, upload stopped!");
                    //System.out.println("WIFI disconnected, upload stopped!");
                    return Result.failure(); // Indicate failure if Work Manager fails
                }
            } catch (Exception e) {
                Log.v(TAG, "App encountered an error during execution process: ", e);
                sendNotification(this.getApplicationContext(), "App encountered an error during execution process: " + e.getMessage());
                return Result.failure(); // Indicate failure if Work Manager fails
            }

        }

        private void
            //Result
        executePythonMain() {
            if (!Python.isStarted()) {
                Python.start(new AndroidPlatform(this.getApplicationContext()));
            }
            try {
                Python py = Python.getInstance();
                PyObject myModule = py.getModule("app");
                myModule.callAttr("main"); // Call the main function in your Python script
                Log.v(TAG, "WorkManager - Python main function executed successfully");
            } catch (Exception e) {
                Log.v(TAG, "Python execution error: ", e);
            }
        }

    }
//----------------------------------------------------------------------------------------------------------------------

    /**
     * Build and send a notification with a custom layout and action.
     */
    @SuppressLint("MissingPermission")
    public static void sendNotification(Context context, String message) {
        // Intent that triggers when the notification is tapped
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        // Build the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
//                .setSmallIcon(R.drawable.baseline_notification_important_24) // Notification icon
                .setSmallIcon(R.drawable.baseline_notification_important_24) // Notification icon
                .setContentTitle("Auto Upload") // Title displayed in the notification
                .setContentText(message) // Text displayed in the notification
                .setContentIntent(pendingIntent) // Pending intent triggered when tapped
                .setAutoCancel(true) // Dismiss notification when tapped
                .setPriority(NotificationCompat.PRIORITY_HIGH); // Notification priority for better visibility
        // Display the notification
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(notificationId, builder.build());
    }


    //=====================================================================================================
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
//        permissionHelper.handlePermissionResult(requestCode);
        if (requestCode == FilePermissionHelper.MANAGE_STORAGE_CODE) {
            permissionHelper.handlePermissionResult(requestCode);
            if (permissionHelper.hasAllPermissions()) {
                startAppProcess();
            }
        }
    }

    /**
     * Create a notification channel for devices running Android 8.0 or higher.
     * A channel groups notifications with similar behavior.
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(
                    channelId,
                    description,
                    NotificationManager.IMPORTANCE_HIGH
            );
            notificationChannel.enableLights(true); // Turn on notification light
            notificationChannel.setLightColor(Color.GREEN);
            notificationChannel.enableVibration(true); // Allow vibration for notifications
            // Optional: Customize vibration pattern
//            long[] vibrationPattern = {0, 100, 200, 300}; // Example: off 0ms, on 100ms, off 200ms, on 300ms
//            notificationChannel.setVibrationPattern(vibrationPattern);
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(notificationChannel);
            }
        }
    }

    /**
     * Checks whether notifications are enabled for the app.
     *
     * @param context Application or activity context.
     * @return true if notifications are enabled, false otherwise.
     */
    public static boolean areNotificationsEnabled(Context context) {
        return NotificationManagerCompat.from(context).areNotificationsEnabled();
    }

    /**
     * Prompts the user to enable notifications if they are disabled.
     * Opens the notification settings for this app.
     */
    public static void promptEnableNotifications(final Context context) {
        if (!areNotificationsEnabled(context)) {
            // Build a dialog to inform the user.
            new AlertDialog.Builder(context)
                    .setTitle("Enable Notifications")
                    .setMessage("Notifications are disabled for Auto Upload app. To ensure you receive important updates, please enable notifications in your settings.")
                    .setPositiveButton("Open Settings", (dialog, which) -> {
                        // Open the app's notification settings.
                        Intent intent = new Intent();
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            intent.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                            intent.putExtra(Settings.EXTRA_APP_PACKAGE, context.getPackageName());
                        } else {
                            // For devices below Oreo, open the generic app settings.
                            intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            intent.setData(Uri.parse("package:" + context.getPackageName()));
                        }
                        context.startActivity(intent);
                    })
                    .setNegativeButton("Cancel", null)
                    .show();

        }
    }
}


//// Check if battery optimizations are enabled
//Intent intent = new Intent();
//intent.setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
//startActivity(intent);