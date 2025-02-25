package com.example.chaquopy;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
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
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements ConnectivityChanger.ConnectivityChangeListener {
    private FilePermissionHelper permissionHelper;
    private static final String TAG = "Auto Upload";
    private static final String channelId = "i.apps.notifications"; // Unique channel ID for notifications
    private final String description = "Auto Upload notification";  // Description for the notification channel
    private static final int notificationId = 1234; // Unique identifier for the notification
    private final int scheduleInterval = 4; // Interval in hours
//    public NetworkUtil networkType;
    private ConnectivityChanger connectivityChanger;

    @Override
    protected void onStart() {
        super.onStart();
    }
    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    public void onConnectivityChanged(boolean isConnected, String networkType) {

    }

    private void updateNetworkStatus() {
        boolean isConnected = connectivityChanger.isConnected();
        String networkType = connectivityChanger.getNetworkType();

        String statusText;
        if (isConnected) {
            statusText = "Connected: Yes\nNetwork Type: " + networkType;
        } else {
            statusText = "Connected: No\nNetwork Type: No Connection";
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Register the connectivity change receiver dynamically.
        // When Wi-Fi is disconnected:
//        String networkType = NetworkUtil.getNetworkType(getApplicationContext());
//        networkType = new NetworkUtil();

        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(networkType, filter);
        if (!"WIFI".equals(networkType.getNetworkType(getApplicationContext()))) {
            sendNotification(getApplicationContext(),"Wifi disconnected, uploading stopped!");
            Log.v(TAG, "Wifi disconnected, uploading stopped!");
            System.out.println("Wifi disconnected, uploading stopped!");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
//        // Unregister the receiver when the activity is not in the foreground
//        if (networkType != null) {
//            unregisterReceiver(networkType);
//        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        setContentView(R.layout.activity_main);
        // Create a notification channel (required for Android 8.0 and higher)
        connectivityChanger = new ConnectivityChanger(this, this); // 'this' Activity is the ConnectivityChangeListener
        Log.d(TAG, "ConnectivityChanger initialized (BroadcastReceiver-based).");

        updateNetworkStatusDisplay(); // Initial network status display

        createNotificationChannel();
        permissionHelper = new FilePermissionHelper(this);
        permissionHelper.requestDownloadFolderAccess();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        101
                );
                return;
            }
        }
        // Check if notifications are enabled in system settings.
        if (!NotificationManagerCompat.from(getApplicationContext()).areNotificationsEnabled()) {
            promptEnableNotifications(getApplicationContext());
        }
        schedulePeriodicWork(); // Schedule WorkManager for periodic Wi-Fi check and Python execution
        // You can remove the immediate network check and Python execution from onCreate
        // because the WorkManager will handle it periodically in the background.
        Log.v(TAG, "Periodic DriveSyncWorker and Python job scheduled.");
    }

    private void schedulePeriodicWork() {
        PeriodicWorkRequest driveSyncWorkRequest =
                new PeriodicWorkRequest.Builder(DriveSyncWorker.class, scheduleInterval, TimeUnit.HOURS)
                        .setConstraints(Constraints.NONE) // You can add constraints if needed (e.g., network type, charging)
                        .build();

        WorkManager.getInstance(getApplicationContext()).enqueueUniquePeriodicWork(
                "DriveSyncWorker", // Unique name for the periodic work
                ExistingPeriodicWorkPolicy.KEEP, // If work with the same name exists, keep the existing one
                driveSyncWorkRequest
        );
        Log.v(TAG, "Periodic DriveSync WorkManager task scheduled to run every "+scheduleInterval+" hours.");
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        permissionHelper.handlePermissionResult(requestCode);
    }


    // Worker class to perform Wi-Fi check and execute Python code in the background
    public static class DriveSyncWorker extends Worker {
        private static final String WORKER_TAG = "DriveSyncWorker";
        public DriveSyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
            super(context, workerParams);
        }

        @NonNull
        @Override
        public Result doWork() {
            try {
                Context context = getApplicationContext();
                String networkType = NetworkUtil.getNetworkType(context);
                Log.v(WORKER_TAG, "WorkManager - Current Network Type: " + networkType);

                if ("WIFI".equals(networkType)) {
                    Log.v(WORKER_TAG, "WorkManager - Connected via Wi-Fi - Executing Python Code");
                    System.out.println("WorkManager - Connected via Wi-Fi - Executing Python Code");
                    executePythonMain();
                    return Result.success(); // Indicate success regardless of network type. Handle failures in Python code and logs.
                } else {
                    Log.v(WORKER_TAG, "WorkManager - Not Wi-Fi - Skipping Python execution");
                    System.out.println("WorkManager - Not Wi-Fi - Skipping Python execution");
                    // When Wi-Fi is disconnected:
                    Log.v(TAG, "Wifi disconnected, uploading stopped!");
                    System.out.println("Wifi disconnected, uploading stopped!");
                    sendNotification(getApplicationContext(),"Wifi disconnected, uploading stopped!");
                    return Result.failure();
                }
            } catch (Exception e) {

                return Result.failure();
            }

        }

        private Result executePythonMain() {
            if (!Python.isStarted()) {
                Python.start(new AndroidPlatform(getApplicationContext()));
            }
            try {
                Python py = Python.getInstance();
                PyObject myModule = py.getModule("app");
                myModule.callAttr("main"); // Call the main function in your Python script
                Log.v(WORKER_TAG, "WorkManager - Python main function executed successfully");
                sendNotification(getApplicationContext(),"Uploading finished, for detail check out log file!");
                return Result.success();
            } catch (Exception e) {
                Log.v(WORKER_TAG, "WorkManager - Error executing Python main function: ", e);
                // In a real app, consider more robust error handling and potentially returning Result.failure()
                // from doWork() if Python execution is critical for the worker's success.
                return Result.failure();
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
                    .setMessage("Notifications are disabled for this app. To ensure you receive important updates, please enable notifications in your settings.")
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
    /**
     * Build and send a notification with a custom layout and action.
     */
    @SuppressLint("MissingPermission")
    private static void sendNotification(Context context, String message) {
        // Intent that triggers when the notification is tapped
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        // Custom layout for the notification content
//        RemoteViews contentView = new RemoteViews(getPackageName(), R.layout.activity_after_notification);
        // Build the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.baseline_notification_important_24) // Notification icon
//                .setContent(contentView) // Custom notification content
                .setContentTitle("Auto Upload") // Title displayed in the notification
                .setContentText(message) // Text displayed in the notification
                .setContentIntent(pendingIntent) // Pending intent triggered when tapped
                .setAutoCancel(true) // Dismiss notification when tapped
                .setPriority(NotificationCompat.PRIORITY_HIGH); // Notification priority for better visibility
        // Display the notification
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(notificationId, builder.build());
    }

}
