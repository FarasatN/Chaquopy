package com.example.chaquopy;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class FilePermissionHelper {

    private static final int STORAGE_PERMISSION_CODE = 100;
    private static final int MANAGE_STORAGE_CODE = 101;
    private final Activity activity;

    public FilePermissionHelper(Activity activity) {
        this.activity = activity;
    }

    // Check and request necessary permissions
    public void requestDownloadFolderAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11 (API 30) and above
            if (!Environment.isExternalStorageManager()) {
                requestManageStoragePermission();
            }
        } else {
            // Android 10 (API 29) and below
            if (!hasReadWritePermissions()) {
                requestReadWritePermissions();
            }
        }
    }

    // Check if we have basic read/write permissions
    private boolean hasReadWritePermissions() {
        return ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    // Request read/write permissions for Android 10 and below
    private void requestReadWritePermissions() {
        ActivityCompat.requestPermissions(activity,
                new String[]{
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                },
                STORAGE_PERMISSION_CODE);
    }

    // Request MANAGE_EXTERNAL_STORAGE permission for Android 11+
    private void requestManageStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + activity.getPackageName()));
                activity.startActivityForResult(intent, MANAGE_STORAGE_CODE);
            } catch (Exception e) {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                activity.startActivityForResult(intent, MANAGE_STORAGE_CODE);
            }
        }
    }

    // Handle permission results
    public void handlePermissionResult(int requestCode) {
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (!hasReadWritePermissions()) {
                // Permission denied, handle accordingly
            }
        } else if (requestCode == MANAGE_STORAGE_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                    !Environment.isExternalStorageManager()) {
                // Permission denied, handle accordingly
            }
        }
    }
}