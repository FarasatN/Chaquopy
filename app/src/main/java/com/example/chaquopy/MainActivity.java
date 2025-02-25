package com.example.chaquopy;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;

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

import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity { // Removed "implements ConnectivityChanger.ConnectivityChangeListener"
    public FilePermissionHelper permissionHelper;
    private static final String TAG = "Auto Upload";
    private static final String WORKER_TAG = "DriveSyncWorker"; // Consistent WORKER_TAG
    private static final String channelId = "i.apps.notifications"; // Unique channel ID for notifications
    private final String description = "Auto Upload notification";  // Description for the notification channel
    private static final int notificationId = 1234; // Unique identifier for the notification
    private final int scheduleInterval = 15; // Interval in minutes

    public static boolean isWifiConnected(@NonNull Context context) { // Made static and public
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            NetworkCapabilities capabilities = cm.getNetworkCapabilities(cm.getActiveNetwork());
            return capabilities != null && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
        } else { // Below Android M
            NetworkInfo activeNetworkInfo = cm.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.getType() == ConnectivityManager.TYPE_WIFI && activeNetworkInfo.isConnected();
        }
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
//            Log.v(WORKER_TAG, "WIFI connected, upload processing..");
//            System.out.println("WIFI connected, upload processing..");
//            schedulePeriodicWork();
//            Log.v(TAG, "Periodic task scheduled");
//            System.out.println("Periodic task scheduled");
//        } else {
//            sendNotification(this, "WIFI disconnected, upload stopped!");
//            Log.v(WORKER_TAG, "WIFI disconnected, upload stopped!");
//            System.out.println("WIFI disconnected, upload stopped!");
//        }
//    }

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
        if (isWifiConnected(this)) {
            sendNotification(this, "WIFI connected, upload processing..");
            Log.v(WORKER_TAG, "WIFI connected, upload processing..");
            System.out.println("WIFI connected, upload processing..");
            schedulePeriodicWork();
            Log.v(TAG, "Periodic task scheduled");
            System.out.println("Periodic task scheduled");
        } else {
            sendNotification(this, "WIFI disconnected, upload stopped!");
            Log.v(WORKER_TAG, "WIFI disconnected, upload stopped!");
            System.out.println("WIFI disconnected, upload stopped!");
        }
    }

    private void schedulePeriodicWork() {
        PeriodicWorkRequest driveSyncWorkRequest =
                new PeriodicWorkRequest.Builder(DriveSyncWorker.class, scheduleInterval, TimeUnit.MINUTES)
                        .setConstraints(Constraints.NONE) // You can add constraints if needed (e.g., network type, charging)
                        .setInitialDelay(5, TimeUnit.SECONDS)
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
            if (isWifiConnected(this.getApplicationContext())) {
                // --- Notification and Logging for Wi-Fi connected ---
                sendNotification(this.getApplicationContext(),"WIFI connected, upload processing..");
                Log.v(WORKER_TAG, "WIFI connected, upload processing..");
                System.out.println("WIFI connected, upload processing..");

                Log.v(WORKER_TAG, "WorkManager - Connected via Wi-Fi - Executing Python Code");
                System.out.println("WorkManager - Connected via Wi-Fi - Executing Python Code");
                executePythonMain();
                return Result.success();
            } else {
                // --- Notification and Logging for No Wi-Fi ---
                sendNotification(this.getApplicationContext(),"WIFI disconnected, upload stopped!"); // Optional notification for Wi-Fi disconnect from worker
                Log.v(WORKER_TAG, "WIFI disconnected, upload stopped!");
                System.out.println("WIFI disconnected, upload stopped!");

                Log.v(WORKER_TAG, "WorkManager - Not Wi-Fi - Skipping Python execution");
                System.out.println("WorkManager - Not Wi-Fi - Skipping Python execution");
                return Result.success(); // Indicate success - worker ran, network checked, Python skipped (not a failure)
            }
        }
        private Result executePythonMain() {
            if (!Python.isStarted()) {
                Python.start(new AndroidPlatform(this.getApplicationContext()));
            }
            try {
                Python py = Python.getInstance();
                PyObject myModule = py.getModule("app");
                myModule.callAttr("main"); // Call the main function in your Python script
                Log.v(WORKER_TAG, "WorkManager - Python main function executed successfully");
                sendNotification(this.getApplicationContext(),"upload finished, for detail check out log file!");
                return Result.success();
            } catch (Exception e) {
                Log.v(WORKER_TAG, "WorkManager - Error executing Python main function: ", e);
                sendNotification(this.getApplicationContext(), "WorkManager encountered an error during Python execution: " + e.getMessage());
                return Result.failure(); // Indicate failure if Python execution fails
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
            notificationChannel.setLightColor(Color.YELLOW);
            notificationChannel.enableVibration(true); // Allow vibration for notifications
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(notificationChannel);
            }
        }
    }
    /**
     * Checks whether notifications are enabled for the app.
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