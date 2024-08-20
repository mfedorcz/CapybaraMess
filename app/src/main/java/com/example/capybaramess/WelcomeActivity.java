package com.example.capybaramess;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.InputFilter;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class WelcomeActivity extends AppCompatActivity {

    private EditText usernameField, realNameField, emailField, bioField;
    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;
    private Dialog loadingDialog;

    private int usernameCharLimit = 20;
    private int realNameCharLimit = 20;
    private int emailCharLimit = 40;
    private int bioCharLimit = 400;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.welcome_activity);

        usernameField = findViewById(R.id.usernameField);
        realNameField = findViewById(R.id.realNameField);
        emailField = findViewById(R.id.emailField);
        bioField = findViewById(R.id.bio);

        // Set character limits
        usernameField.setFilters(new InputFilter[]{new InputFilter.LengthFilter(usernameCharLimit)});
        realNameField.setFilters(new InputFilter[]{new InputFilter.LengthFilter(realNameCharLimit)});
        emailField.setFilters(new InputFilter[]{new InputFilter.LengthFilter(emailCharLimit)});
        bioField.setFilters(new InputFilter[]{new InputFilter.LengthFilter(bioCharLimit)});

        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        findViewById(R.id.continueButton).setOnClickListener(v -> onContinueClicked());
    }

    private void onContinueClicked() {
        String username = usernameField.getText().toString().trim();
        String realName = realNameField.getText().toString().trim();
        String email = emailField.getText().toString().trim();
        String bio = bioField.getText().toString().trim();

        if (username.isEmpty()) {
            usernameField.setError("Username is required");
            usernameField.requestFocus();
            return;
        }

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            showLoadingScreen(); // Show loading screen when the operation starts
            String phoneNumber = user.getPhoneNumber(); // Get phone number from FirebaseUser
            saveUserProfileData(user.getUid(), username, realName, email, bio, phoneNumber);
        }
    }

    private void saveUserProfileData(String uid, String username, String realName, String email, String bio, String phoneNumber) {
        // Create a Map to store the user profile data
        Map<String, Object> userProfile = new HashMap<>();
        userProfile.put("username", username);
        userProfile.put("realName", realName);
        userProfile.put("email", email);
        userProfile.put("bio", bio);
        userProfile.put("phoneNumber", phoneNumber); // Add phone number to profile data

        // Save the user profile data to Firestore
        firestore.collection("users").document(uid).set(userProfile)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(WelcomeActivity.this, "Profile saved successfully", Toast.LENGTH_SHORT).show();
                    // Ensure Firebase Authentication profile is consistent
                    updateFirebaseUserProfile(realName, username);
                })
                .addOnFailureListener(e -> {
                    dismissLoadingScreen(); // Dismiss loading screen on failure
                    Toast.makeText(WelcomeActivity.this, "Failed to save profile", Toast.LENGTH_SHORT).show();
                });
    }

    private void updateFirebaseUserProfile(String realName, String username) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            // If realName is not provided, use the username as the displayName
            String displayName = realName.isEmpty() ? username : realName;

            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                    .setDisplayName(displayName)
                    .build();

            user.updateProfile(profileUpdates)
                    .addOnCompleteListener(task -> {
                        dismissLoadingScreen();
                        if (task.isSuccessful()) {
                            startMainActivity();
                        } else {
                            Toast.makeText(WelcomeActivity.this, "Failed to update Firebase profile", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void startMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showLoadingScreen() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);

        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_loading, null);

        ImageView loadingImageView = dialogView.findViewById(R.id.loading_image);
        Glide.with(this)
                .asGif()
                .load(R.drawable.loading_animation) // Replace with your GIF drawable
                .into(loadingImageView);

        builder.setView(dialogView);

        loadingDialog = builder.create();

        if (loadingDialog.getWindow() != null) {
            loadingDialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        loadingDialog.show();
    }

    private void dismissLoadingScreen() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
    }
}
