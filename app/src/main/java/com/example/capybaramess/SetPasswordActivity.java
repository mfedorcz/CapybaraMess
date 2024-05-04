package com.example.capybaramess;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

public class SetPasswordActivity extends AppCompatActivity {

    private EditText editTextPassword;
    private Button buttonSetPassword;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_password);

        mAuth = FirebaseAuth.getInstance();

        editTextPassword = findViewById(R.id.editTextPassword);
        buttonSetPassword = findViewById(R.id.buttonSetPassword);

        buttonSetPassword.setOnClickListener(v -> {
            String password = editTextPassword.getText().toString().trim();
            if (!password.isEmpty()) {
                linkPasswordToAccount(password);
            } else {
                editTextPassword.setError("Password is required");
            }
        });
    }

    private void linkPasswordToAccount(String password) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String email = user.getPhoneNumber() + "@example.com";
            AuthCredential credential = EmailAuthProvider.getCredential(email, password);

            user.linkWithCredential(credential)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(SetPasswordActivity.this, "Password linked successfully", Toast.LENGTH_SHORT).show();
                            updateUserProfile(email);  // Start MainActivity after profile update
                        } else {
                            Toast.makeText(SetPasswordActivity.this, "Failed to link account with password: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        }
    }

    private void updateUserProfile(String email) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                    .setDisplayName(user.getPhoneNumber()) // Example update, add email if needed
                    .build();

            user.updateProfile(profileUpdates)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(SetPasswordActivity.this, "User profile updated", Toast.LENGTH_LONG).show();
                            startMainActivity();  // Call here after profile update
                        } else {
                            Toast.makeText(SetPasswordActivity.this, "Profile update failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
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
}