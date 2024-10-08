package com.example.capybaramess;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

public class SetPasswordActivity extends AppCompatActivity {

    private EditText editTextPassword1;
    private EditText editTextPassword2;
    private Button buttonSetPassword;
    private FirebaseAuth mAuth;
    private Dialog loadingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.set_password_activity);

        mAuth = FirebaseAuth.getInstance();

        // Initialize the EditText fields
        editTextPassword1 = findViewById(R.id.password1);
        editTextPassword2 = findViewById(R.id.password2);
        buttonSetPassword = findViewById(R.id.buttonSetPassword);

        buttonSetPassword.setOnClickListener(v -> {
            String password1 = editTextPassword1.getText().toString().trim();
            String password2 = editTextPassword2.getText().toString().trim();

            if (password1.isEmpty() || password2.isEmpty()) {
                // Checking if either of the password fields is empty
                if (password1.isEmpty()) {
                    editTextPassword1.setError("Password is required");
                }
                if (password2.isEmpty()) {
                    editTextPassword2.setError("Password is required");
                }
            } else if (!password1.equals(password2)) {
                // Check if the passwords do not match
                editTextPassword1.setError("Passwords do not match");
                editTextPassword2.setError("Passwords do not match");
                editTextPassword1.setText("");
                editTextPassword2.setText("");
                Toast.makeText(SetPasswordActivity.this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            } else {
                // If passwords match, show the loading screen and proceed to link the account
                showLoadingScreen();
                linkPasswordToAccount(password1);
            }
        });
    }

    private void linkPasswordToAccount(String password) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String email = user.getPhoneNumber() + "@capy.bara";
            AuthCredential credential = EmailAuthProvider.getCredential(email, password);

            user.linkWithCredential(credential)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(SetPasswordActivity.this, "Password linked successfully", Toast.LENGTH_SHORT).show();
                            updateUserProfile(email);
                        } else {
                            dismissLoadingScreen();  // Dismiss loading screen if linking fails
                            Toast.makeText(SetPasswordActivity.this, "Failed to link account with password: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        }
    }

    private void updateUserProfile(String email) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                    .setDisplayName(user.getPhoneNumber())  // Add email if needed later
                    .build();

            user.updateProfile(profileUpdates)
                    .addOnCompleteListener(task -> {
                        dismissLoadingScreen();  // Dismiss loading screen after profile update
                        if (task.isSuccessful()) {
                            Toast.makeText(SetPasswordActivity.this, "User profile updated", Toast.LENGTH_LONG).show();
                            startWelcomeActivity();  // Call here after profile update
                        } else {
                            Toast.makeText(SetPasswordActivity.this, "Profile update failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        }
    }

    private void startWelcomeActivity() {
        Intent intent = new Intent(this, WelcomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showLoadingScreen() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);

        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_loading, null);

        // Load GIF using Glide
        ImageView loadingImageView = dialogView.findViewById(R.id.loading_image);
        Glide.with(this)
                .asGif()
                .load(R.drawable.loading_animation)
                .into(loadingImageView);

        builder.setView(dialogView);

        loadingDialog = builder.create();

        // Make dialog background transparent
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
