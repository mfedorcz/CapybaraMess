package com.example.capybaramess;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.concurrent.TimeUnit;

public class CodeVerificationActivity extends AppCompatActivity {

    private EditText[] codeInputs = new EditText[6];
    private String verificationId;
    private String phoneNumber;
    private FirebaseAuth mAuth;
    private Dialog loadingDialog;
    private Button buttonSendAgain;
    private CountDownTimer resendCountDownTimer;
    private long resendTimeout = 180000; // 3 minutes in milliseconds
    private long millisUntilFinished; // To store the remaining time
    private boolean canResendCode = false;
    private PhoneAuthProvider.ForceResendingToken forceResendingToken;
    private boolean isActivityActive = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.verification_activity);

        mAuth = FirebaseAuth.getInstance();

        // Initialize each EditText field
        codeInputs[0] = findViewById(R.id.editTextCode1);
        codeInputs[1] = findViewById(R.id.editTextCode2);
        codeInputs[2] = findViewById(R.id.editTextCode3);
        codeInputs[3] = findViewById(R.id.editTextCode4);
        codeInputs[4] = findViewById(R.id.editTextCode5);
        codeInputs[5] = findViewById(R.id.editTextCode6);

        buttonSendAgain = findViewById(R.id.buttonSendAgain);

        verificationId = getIntent().getStringExtra("verificationId");
        phoneNumber = getIntent().getStringExtra("phoneNumber");
        forceResendingToken = getIntent().getParcelableExtra("forceResendingToken");

        // Set listeners to each input to move focus and trigger verification after the last digit
        for (int i = 0; i < codeInputs.length; i++) {
            final int index = i;
            codeInputs[index].addTextChangedListener(new TextWatcher() {
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (s.length() == 1 && index < codeInputs.length - 1) {
                        codeInputs[index + 1].requestFocus();
                    }
                    if (index == codeInputs.length - 1 && s.length() == 1) {
                        verifyVerificationCode();
                    }
                }
                public void afterTextChanged(Editable s) {}
            });
        }

        // Initially, make the button look disabled
        updateButtonAppearance(false);

        // Start the countdown timer for resending the code
        startResendCountDown();

        // Handle the "Send Again" button click
        buttonSendAgain.setOnClickListener(v -> {
            if (canResendCode) {
                // Resend the verification code if allowed
                if (phoneNumber != null && !phoneNumber.isEmpty()) {
                    resendVerificationCode(phoneNumber);
                } else {
                    Toast.makeText(CodeVerificationActivity.this, "Phone number is missing.", Toast.LENGTH_SHORT).show();
                }
            } else {
                // Show the remaining time in a Toast
                long secondsRemaining = millisUntilFinished / 1000;
                long minutes = secondsRemaining / 60;
                long seconds = secondsRemaining % 60;
                String timeRemaining = String.format("%02d:%02d", minutes, seconds);
                if (isActivityActive) {
                    Toast.makeText(CodeVerificationActivity.this, "You can now resend the verification code.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
    @Override
    protected void onStart() {
        super.onStart();
        isActivityActive = true;
    }
    @Override
    protected void onStop() {
        super.onStop();
        isActivityActive = false;
    }
    private void startResendCountDown() {
        resendCountDownTimer = new CountDownTimer(resendTimeout, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                // Update the remaining time
                CodeVerificationActivity.this.millisUntilFinished = millisUntilFinished;
            }

            @Override
            public void onFinish() {
                canResendCode = true; // Allow resending the code
                updateButtonAppearance(true);
                Toast.makeText(CodeVerificationActivity.this, "You can now resend the verification code.", Toast.LENGTH_SHORT).show();
            }
        }.start();
    }

    private void updateButtonAppearance(boolean enabled) {
        if (enabled) {
            buttonSendAgain.setText("Send again!");
        } else {
            buttonSendAgain.setText("Send again?");
        }
    }

    private void verifyVerificationCode() {
        showLoadingScreen();

        StringBuilder code = new StringBuilder();
        for (EditText editText : codeInputs) {
            code.append(editText.getText().toString().trim());
        }
        if (!code.toString().isEmpty()) {
            PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, code.toString());
            signInWithPhoneAuthCredential(credential);
        } else {
            codeInputs[0].setError("Complete code is required");
            dismissLoadingScreen();
        }
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    dismissLoadingScreen();
                    if (task.isSuccessful()) {
                        // Determine the origin of the request
                        Intent intent;
                        String origin = getIntent().getStringExtra("origin");


                        if ("registration".equals(origin)) {
                            // Redirect to SetPasswordActivity if started by RegistrationActivity
                            intent = new Intent(CodeVerificationActivity.this, SetPasswordActivity.class);
                        } else {
                            // Redirect to MainActivity if started by LoginActivity
                            signIn(phoneNumber,getIntent().getStringExtra("password"));
                            intent = new Intent(CodeVerificationActivity.this, MainActivity.class);
                        }

                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(CodeVerificationActivity.this, "Verification failed. Try again.", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(CodeVerificationActivity.this, RegistrationActivity.class);
                        startActivity(intent);
                        finish();
                    }
                });
    }
    private void signIn(String phoneNumber, String password) {
        String email = phoneNumber + "@capy.bara";

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Log.d("CodeVerification", "signIn successful");
                        } else {
                            Toast.makeText(CodeVerificationActivity.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
    private void resendVerificationCode(String phoneNumber) {
        showLoadingScreen();

        PhoneAuthOptions.Builder optionsBuilder = PhoneAuthOptions.newBuilder(mAuth)
                .setPhoneNumber(phoneNumber)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onVerificationCompleted(PhoneAuthCredential credential) {
                        dismissLoadingScreen();
                        signInWithPhoneAuthCredential(credential);
                    }

                    @Override
                    public void onVerificationFailed(com.google.firebase.FirebaseException e) {
                        dismissLoadingScreen();
                        String errorMsg = e.getMessage();
                        Log.e("CodeVerification", errorMsg);
                        Toast.makeText(CodeVerificationActivity.this, "Verification Failed: Try again later.", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onCodeSent(String verificationId, PhoneAuthProvider.ForceResendingToken token) {
                        dismissLoadingScreen();
                        Toast.makeText(CodeVerificationActivity.this, "Verification code sent again.", Toast.LENGTH_SHORT).show();
                        CodeVerificationActivity.this.verificationId = verificationId;
                        CodeVerificationActivity.this.forceResendingToken = token; // Store the token
                        canResendCode = false;
                        startResendCountDown(); // Restart the countdown timer
                    }
                });

        if (forceResendingToken != null) {
            optionsBuilder.setForceResendingToken(forceResendingToken);
        }

        PhoneAuthProvider.verifyPhoneNumber(optionsBuilder.build());
    }

    private void showLoadingScreen() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);

        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_loading, null);

        ImageView loadingImageView = dialogView.findViewById(R.id.loading_image);
        Glide.with(this)
                .asGif()
                .load(R.drawable.loading_animation)
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
