package com.example.capybaramess;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputFilter;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.json.JSONException;
import org.json.JSONObject;

public class WelcomeActivity extends AppCompatActivity {

    private EditText usernameField, realNameField, emailField, bioField;
    private FirebaseAuth mAuth;
    private FirebaseStorage storage;
    private StorageReference storageReference;

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
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

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
            saveUserProfileData(user.getUid(), username, realName, email, bio);
        }
    }

    private void saveUserProfileData(String uid, String username, String realName, String email, String bio) {
        JSONObject userProfile = new JSONObject();
        try {
            userProfile.put("username", username);
            userProfile.put("realName", realName);
            userProfile.put("email", email);
            userProfile.put("bio", bio);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        uploadJsonData(uid, userProfile);
    }

    private void uploadJsonData(String uid, JSONObject jsonObject) {
        StorageReference jsonReference = storageReference.child("profiles/" + uid + "/profile.json");
        byte[] jsonData = jsonObject.toString().getBytes();
        jsonReference.putBytes(jsonData)
                .addOnSuccessListener(taskSnapshot -> {
                    Toast.makeText(WelcomeActivity.this, "Profile saved successfully", Toast.LENGTH_SHORT).show();
                    startMainActivity();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(WelcomeActivity.this, "Failed to save profile", Toast.LENGTH_SHORT).show();
                });
    }

    private void startMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
