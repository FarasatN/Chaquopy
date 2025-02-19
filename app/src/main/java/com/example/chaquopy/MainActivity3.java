package com.example.chaquopy;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
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

public class MainActivity3 extends AppCompatActivity {
    private FilePermissionHelper permissionHelper;
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        permissionHelper = new FilePermissionHelper(this);
        permissionHelper.requestDownloadFolderAccess();

        schedulePeriodicWork(); // Schedule WorkManager for periodic Wi-Fi check and Python execution

        // You can remove the immediate network check and Python execution from onCreate
        // because the WorkManager will handle it periodically in the background.
        Log.v(TAG, "Periodic DriveSyncWorker and Python job scheduled.");
    }

    private void schedulePeriodicWork() {
        PeriodicWorkRequest driveSyncWorkRequest =
                new PeriodicWorkRequest.Builder(DriveSyncWorker.class, 15, TimeUnit.MINUTES)
                        .setConstraints(Constraints.NONE) // You can add constraints if needed (e.g., network type, charging)
                        .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "DriveSyncWorker", // Unique name for the periodic work
                ExistingPeriodicWorkPolicy.KEEP, // If work with the same name exists, keep the existing one
                driveSyncWorkRequest
        );
        Log.v(TAG, "Periodic DriveSync WorkManager task scheduled to run every 15 minutes");
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
            Context context = getApplicationContext();
            String networkType = NetworkUtil.getNetworkType(context);
            Log.v(WORKER_TAG, "WorkManager - Current Network Type: " + networkType);

            if ("WIFI".equals(networkType)) {
                Log.v(WORKER_TAG, "WorkManager - Connected via Wi-Fi - Executing Python Code");
                System.out.println("WorkManager - Connected via Wi-Fi - Executing Python Code");
                executePythonMain();
            } else {
                Log.v(WORKER_TAG, "WorkManager - Not Wi-Fi - Skipping Python execution");
                System.out.println("WorkManager - Not Wi-Fi - Skipping Python execution");
            }
            return Result.success(); // Indicate success regardless of network type. Handle failures in Python code and logs.
        }

        private void executePythonMain() {
            if (!Python.isStarted()) {
                Python.start(new AndroidPlatform(getApplicationContext()));
            }
            try {
                Python py = Python.getInstance();
                PyObject myModule = py.getModule("app");
                myModule.callAttr("main"); // Call the main function in your Python script
                Log.d(WORKER_TAG, "WorkManager - Python main function executed successfully");
            } catch (Exception e) {
                Log.e(WORKER_TAG, "WorkManager - Error executing Python main function: ", e);
                // In a real app, consider more robust error handling and potentially returning Result.failure()
                // from doWork() if Python execution is critical for the worker's success.
            }
        }
    }
}