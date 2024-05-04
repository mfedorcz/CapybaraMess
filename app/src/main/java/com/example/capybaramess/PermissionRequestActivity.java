package com.example.capybaramess;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import android.widget.Toast;
import androidx.core.content.ContextCompat;

public class PermissionRequestActivity extends AppCompatActivity {

    private static final int PERMISSIONS_REQUEST_CODE = 1240;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.permission_request_activity);

        findViewById(R.id.btn_request_permissions).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestPermissions();
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (checkPermissions()) {
            startMainActivity();
        }
    }

    private boolean checkPermissions() {
        return ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED;
    }

    private void startMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    private void requestPermissions() {
        String[] permissions = {
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.READ_SMS,
                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.SEND_SMS
        };

        ActivityCompat.requestPermissions(this, permissions, PERMISSIONS_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            boolean allPermissionsGranted = true;
            boolean shouldShowRationale = false;

            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    // Check if we should show a rationale for the denied permission
                    if (ActivityCompat.shouldShowRequestPermissionRationale(this, permissions[i])) {
                        shouldShowRationale = true;
                    }
                }
            }

            if (allPermissionsGranted) {
                // All permissions have been granted
                startMainActivity();  // Or whatever method you use to proceed in your app
            } else if (shouldShowRationale) {
                // At least one permission was denied and we should show a rationale
                Toast.makeText(this, "Permissions needed to run the app properly. Please allow them.", Toast.LENGTH_LONG).show();
            } else {
                // At least one permission was denied and the user selected "Don't ask again"
                Toast.makeText(this, "You have denied permissions. Please enable them in settings to continue.", Toast.LENGTH_LONG).show();
                // Direct the user to the app's settings page
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", getPackageName(), null);
                intent.setData(uri);
                startActivity(intent);
            }
        }
    }
}