//package com.example.chaquopy;
//
//import android.annotation.SuppressLint;
//import android.app.NotificationChannel;
//import android.app.NotificationManager;
//import android.app.PendingIntent;
//import android.content.Context;
//import android.content.Intent;
//import android.graphics.Color;
//import android.net.Uri;
//import android.os.Build;
//import android.os.Bundle;
//import android.provider.Settings;
//import android.util.Log;
//
//import androidx.annotation.NonNull;
//import androidx.annotation.Nullable;
//import androidx.appcompat.app.AlertDialog;
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.core.app.NotificationCompat;
//import androidx.core.app.NotificationManagerCompat;
//import androidx.work.Constraints;
//import androidx.work.ExistingPeriodicWorkPolicy;
//import androidx.work.PeriodicWorkRequest;
//import androidx.work.WorkManager;
//import androidx.work.Worker;
//import androidx.work.WorkerParameters;
//
//import com.chaquo.python.PyObject;
//import com.chaquo.python.Python;
//import com.chaquo.python.android.AndroidPlatform;
//
//import java.util.concurrent.TimeUnit;
//
//public class MainActivity2 extends AppCompatActivity implements ConnectivityChanger.ConnectivityChangeListener {
//    private FilePermissionHelper permissionHelper;
//    private static final String TAG = "Auto Upload";
//    private static final String channelId = "i.apps.notifications"; // Unique channel ID for notifications
//    private final String description = "Auto Upload notification";  // Description for the notification channel
//    private static final int notificationId = 1234; // Unique identifier for the notification
////    private final int scheduleInterval = 240;
//    private final int scheduleInterval = 15; // Interval in minutes
////    public NetworkUtil networkType;
//    private static ConnectivityChanger connectivityChanger;
//    @Override
//    protected void onStart() {
//        super.onStart();
//        connectivityChanger.startMonitoring(); // Start monitoring when Activity starts
//        Log.v(TAG, "Connectivity monitoring started (BroadcastReceiver-based).");
//
//    }
//    @Override
//    protected void onStop() {
//        super.onStop();
////        connectivityChanger.stopMonitoring(); // Stop monitoring when Activity stops
////        Log.v(TAG, "Connectivity monitoring stopped (BroadcastReceiver-based).");
//    }
//    @Override
//    public void onConnectivityChanged(boolean isConnected, String networkType) {
//        Log.v(TAG, "Connectivity changed (BroadcastReceiver) - Connected: " + isConnected + ", Type: " + networkType);
//        updateNetworkStatus(getApplicationContext()); // Update UI when connectivity changes
//        if (isConnected && ("WIFI".equals(networkType))) {
//            sendNotification(getApplicationContext(),networkType+" connected, uploading processing..");
//            Log.v(TAG, networkType+" connected, uploading processing..");
//            System.out.println(networkType+" connected, uploading processing..");
////            Toast.makeText(this,networkType+ " connected, uploading processing.." , Toast.LENGTH_SHORT).show(); // Example Toast notification
//        } else {
//            sendNotification(getApplicationContext(),networkType+" disconnected, uploading stopped!");
//            Log.v(TAG, networkType+" disconnected, uploading stopped!");
//            System.out.println(networkType+" disconnected, uploading stopped!");
////            Toast.makeText(this,networkType+ " disconnected, uploading stopped!" , Toast.LENGTH_SHORT).show();
//        }
//    }
//
//    private static boolean updateNetworkStatus(Context context) {
//        boolean isConnected = connectivityChanger.isConnected();
//        String networkType = connectivityChanger.getNetworkType();
//        if (isConnected && ("WIFI".equals(networkType))) {
//            sendNotification(context,networkType+" connected, uploading processing..");
//            Log.v(TAG, networkType+" connected, uploading processing..");
//            System.out.println(networkType+" connected, uploading processing..");
////            Toast.makeText(context,networkType+ " connected, uploading processing.." , Toast.LENGTH_SHORT).show(); // Example Toast notification
//            return true;
//        }else {
//            sendNotification(context,networkType+" disconnected, uploading stopped!");
//            Log.v(TAG, networkType+" disconnected, uploading stopped!");
//            System.out.println(networkType+" disconnected, uploading stopped!");
////            Toast.makeText(context,networkType+ " disconnected, uploading stopped!" , Toast.LENGTH_SHORT).show();
//            return false;
//        }
//    }
//
//    @Override
//    protected void onResume() {
//        super.onResume();
//        // Register the connectivity change receiver dynamically.
//        // When Wi-Fi is disconnected:
////        updateNetworkStatus(getApplicationContext());
//    }
//
//    @Override
//    protected void onPause() {
//        super.onPause();
////        // Unregister the receiver when the activity is not in the foreground
////        if (networkType != null) {
////            unregisterReceiver(networkType);
////        }
//    }
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        if (getSupportActionBar() != null) {
//            getSupportActionBar().hide();
//        }
//        setContentView(R.layout.activity_main);
//        permissionHelper = new FilePermissionHelper(this);
//        permissionHelper.requestDownloadFolderAccess();
////        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
////            if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.POST_NOTIFICATIONS)
////                    != PackageManager.PERMISSION_GRANTED) {
////                ActivityCompat.requestPermissions(
////                        this,
////                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
////                        101
////                );
////                return;
////            }
////        }else{
////
////        }
//        //         Check if notifications are enabled in system settings.
//        if (!NotificationManagerCompat.from(getApplicationContext()).areNotificationsEnabled()) {
//            promptEnableNotifications(MainActivity2.this);
//        }
//        createNotificationChannel();
//        connectivityChanger = new ConnectivityChanger(this, this); // 'this' Activity is the ConnectivityChangeListener
//        Log.v(TAG, "ConnectivityChanger initialized (BroadcastReceiver-based).");
//        updateNetworkStatus(getApplicationContext()); // Initial network status display
//        schedulePeriodicWork(); // Schedule WorkManager for periodic Wi-Fi check and Python execution
//        // You can remove the immediate network check and Python execution from onCreate
//        // because the WorkManager will handle it periodically in the background.
//    }
//
//    private void schedulePeriodicWork() {
//        PeriodicWorkRequest driveSyncWorkRequest =
////                new PeriodicWorkRequest.Builder(DriveSyncWorker.class, scheduleInterval, TimeUnit.HOURS)
//                new PeriodicWorkRequest.Builder(DriveSyncWorker.class, scheduleInterval, TimeUnit.MINUTES)
//                        .setConstraints(Constraints.NONE) // You can add constraints if needed (e.g., network type, charging)
//                        .build();
//        WorkManager.getInstance(getApplicationContext()).enqueueUniquePeriodicWork(
//                "DriveSyncWorker", // Unique name for the periodic work
//                ExistingPeriodicWorkPolicy.KEEP, // If work with the same name exists, keep the existing one
//                driveSyncWorkRequest
//        );
//        Log.v(TAG, "Periodic DriveSync WorkManager task scheduled to run every "+scheduleInterval+" hours.");
//    }
//
//    // Worker class to perform Wi-Fi check and execute Python code in the background
//    public static class DriveSyncWorker extends Worker {
//        private static final String WORKER_TAG = "DriveSyncWorker";
//        public DriveSyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
//            super(context, workerParams);
//        }
//        @NonNull
//        @Override
//        public Result doWork() {
//            try {
//                if (updateNetworkStatus(getApplicationContext())) {
//                    Log.v(WORKER_TAG, "WorkManager - Connected via Wi-Fi - Executing Python Code");
//                    System.out.println("WorkManager - Connected via Wi-Fi - Executing Python Code");
//                    executePythonMain();
//                    return Result.success(); // Indicate success regardless of network type. Handle failures in Python code and logs.
//                } else {
//                    Log.v(WORKER_TAG, "WorkManager - Not Wi-Fi - Skipping Python execution");
//                    System.out.println("WorkManager - Not Wi-Fi - Skipping Python execution");
//                    // When Wi-Fi is disconnected:
//                    sendNotification(getApplicationContext(),"Wifi disconnected, uploading stopped!");
//                    return Result.failure();
//                }
//            } catch (Exception e) {
//                Log.v(WORKER_TAG, "WorkManager doWork Exception: ", e);
//                sendNotification(getApplicationContext(), "WorkManager encountered an error: " + e.getMessage()); // Notify about WorkManager error
//                return Result.failure();
//            }
//        }
//        private Result executePythonMain() {
//            if (!Python.isStarted()) {
//                Python.start(new AndroidPlatform(getApplicationContext()));
//            }
//            try {
//                Python py = Python.getInstance();
//                PyObject myModule = py.getModule("app");
//                myModule.callAttr("main"); // Call the main function in your Python script
//                Log.v(WORKER_TAG, "WorkManager - Python main function executed successfully");
//                sendNotification(getApplicationContext(),"Uploading finished, for detail check out log file!");
//                return Result.success();
//            } catch (Exception e) {
//                Log.v(WORKER_TAG, "WorkManager - Error executing Python main function: ", e);
//                // In a real app, consider more robust error handling and potentially returning Result.failure()
//                // from doWork() if Python execution is critical for the worker's success.
//                return Result.failure();
//            }
//        }
//    }
////----------------------------------------------------------------------------------------------------------------------
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//    //=====================================================================================================
//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//        permissionHelper.handlePermissionResult(requestCode);
//    }
//    /**
//     * Create a notification channel for devices running Android 8.0 or higher.
//     * A channel groups notifications with similar behavior.
//     */
//    private void createNotificationChannel() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            NotificationChannel notificationChannel = new NotificationChannel(
//                    channelId,
//                    description,
//                    NotificationManager.IMPORTANCE_HIGH
//            );
//            notificationChannel.enableLights(true); // Turn on notification light
//            notificationChannel.setLightColor(Color.YELLOW);
//            notificationChannel.enableVibration(true); // Allow vibration for notifications
//            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
//            if (notificationManager != null) {
//                notificationManager.createNotificationChannel(notificationChannel);
//            }
//        }
//    }
//        /**
//     * Checks whether notifications are enabled for the app.
//     * @param context Application or activity context.
//     * @return true if notifications are enabled, false otherwise.
//     */
//    public static boolean areNotificationsEnabled(Context context) {
//        return NotificationManagerCompat.from(context).areNotificationsEnabled();
//    }
//    /**
//     * Prompts the user to enable notifications if they are disabled.
//     * Opens the notification settings for this app.
//     */
//    public static void promptEnableNotifications(final Context context) {
//        if (!areNotificationsEnabled(context)) {
//            // Build a dialog to inform the user.
//            new AlertDialog.Builder(context)
//                    .setTitle("Enable Notifications")
//                    .setMessage("Notifications are disabled for this app. To ensure you receive important updates, please enable notifications in your settings.")
//                    .setPositiveButton("Open Settings", (dialog, which) -> {
//                        // Open the app's notification settings.
//                        Intent intent = new Intent();
//                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                            intent.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
//                            intent.putExtra(Settings.EXTRA_APP_PACKAGE, context.getPackageName());
//                        } else {
//                            // For devices below Oreo, open the generic app settings.
//                            intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
//                            intent.setData(Uri.parse("package:" + context.getPackageName()));
//                        }
//                        context.startActivity(intent);
//                    })
//                    .setNegativeButton("Cancel", null)
//                    .show();
//        }
//    }
//    /**
//     * Build and send a notification with a custom layout and action.
//     */
//    @SuppressLint("MissingPermission")
//    private static void sendNotification(Context context, String message) {
//        // Intent that triggers when the notification is tapped
//        Intent intent = new Intent(context, MainActivity2.class);
//        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
//        PendingIntent pendingIntent = PendingIntent.getActivity(
//                context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
//        );
//        // Custom layout for the notification content
////        RemoteViews contentView = new RemoteViews(getPackageName(), R.layout.activity_after_notification);
//        // Build the notification
//        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
//                .setSmallIcon(R.drawable.baseline_notification_important_24) // Notification icon
////                .setContent(contentView) // Custom notification content
//                .setContentTitle("Auto Upload") // Title displayed in the notification
//                .setContentText(message) // Text displayed in the notification
//                .setContentIntent(pendingIntent) // Pending intent triggered when tapped
//                .setAutoCancel(true) // Dismiss notification when tapped
//                .setPriority(NotificationCompat.PRIORITY_HIGH); // Notification priority for better visibility
//        // Display the notification
//        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
//        notificationManager.notify(notificationId, builder.build());
//    }
//
//}
