package com.example.capybaramess;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.FirebaseApp;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.concurrent.TimeUnit;

public class RegistrationActivity extends AppCompatActivity {
    private FirebaseFirestore firestore;
    private EditText editTextPhoneNumber;
    private FirebaseAuth mAuth;
    private Dialog loadingDialog;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.registration_activity);

        // Initialize Firebase
        FirebaseApp.initializeApp(this);
        firestore = FirebaseFirestore.getInstance();

        // Initialize FirebaseAuth
        mAuth = FirebaseAuth.getInstance();

        editTextPhoneNumber = findViewById(R.id.phoneNumberInput);
        Button buttonVerifyPhone = findViewById(R.id.buttonSignUp);
        Button buttonSignIn = findViewById(R.id.buttonSignIn);

        buttonVerifyPhone.setOnClickListener(v -> {
            String phone = editTextPhoneNumber.getText().toString().trim();
            if (!phone.isEmpty()) {
                // Show the loading screen
                showLoadingScreen();

                // Check if the phone number exists
                checkIfPhoneNumberExists(phone, exists -> {
                    if (exists) {
                        // Phone number exists, dismiss the loading screen
                        dismissLoadingScreen();
                        editTextPhoneNumber.setError("This phone number is already registered with a username.");
                    } else {
                        // Phone number does not exist, send verification code
                        sendVerificationCode(phone);
                    }
                });
            } else {
                editTextPhoneNumber.setError("Phone number is required");
            }
        });

        buttonSignIn.setOnClickListener(v -> {
            Intent intent = new Intent(RegistrationActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });

        // Terms of service string formatting
        TextView textViewTerms = findViewById(R.id.textViewTerms);
        String termsText = "By clicking Sign up, you agree to our Terms of Service and Privacy Policy";
        SpannableString spannableString = new SpannableString(termsText);

        ClickableSpan termsClickableSpan = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                // Handling "Terms of Service" click
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=dQw4w9WgXcQ")));
            }

            @Override
            public void updateDrawState(@NonNull TextPaint ds) {
                super.updateDrawState(ds);
                ds.setUnderlineText(false);
                ds.setColor(Color.BLACK);
            }
        };

        ClickableSpan privacyClickableSpan = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                //handling "Privacy Policy" click
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=dQw4w9WgXcQ")));
            }

            @Override
            public void updateDrawState(@NonNull TextPaint ds) {
                super.updateDrawState(ds);
                ds.setUnderlineText(false);
                ds.setColor(Color.BLACK);
            }
        };

        //Setting the spans for the text
        spannableString.setSpan(termsClickableSpan, termsText.indexOf("Terms of Service"), termsText.indexOf("Terms of Service") + "Terms of Service".length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannableString.setSpan(privacyClickableSpan, termsText.indexOf("Privacy Policy"), termsText.indexOf("Privacy Policy") + "Privacy Policy".length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        //Setting the SpannableString to the TextView
        textViewTerms.setText(spannableString);
        textViewTerms.setMovementMethod(LinkMovementMethod.getInstance());
    }
    private void checkIfPhoneNumberExists(String phoneNumber, PhoneNumberCheckCallback callback) {
        firestore.collection("users")
                .whereEqualTo("phoneNumber", phoneNumber)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    boolean exists = !queryDocumentSnapshots.isEmpty();
                    callback.onResult(exists);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to check phone number. Please try again.", Toast.LENGTH_SHORT).show();
                    callback.onResult(false); // Assume phone doesn't exist on failure
                });
    }

    interface PhoneNumberCheckCallback {
        void onResult(boolean exists);
    }

    private void sendVerificationCode(String phone) {
        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(mAuth)
                .setPhoneNumber(phone)       // number to verify
                .setTimeout(60L, TimeUnit.SECONDS) // Timeout and unit
                .setActivity(this)           // for callback binding
                .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onVerificationCompleted(com.google.firebase.auth.PhoneAuthCredential phoneAuthCredential) {
                        // Dismiss the loading screen
                        dismissLoadingScreen();
                        // method is called when the verification is done automatically
                    }

                    @Override
                    public void onVerificationFailed(com.google.firebase.FirebaseException e) {
                        // Dismiss the loading screen
                        dismissLoadingScreen();

                        String er_msg = e.getMessage();
                        assert er_msg != null;
                        Log.e("RegistrationActivity", er_msg);
                        Toast.makeText(RegistrationActivity.this, "Verification Failed: Try again later or contact administrator.", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onCodeSent(String verificationId, PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                        super.onCodeSent(verificationId, forceResendingToken);

                        // Pass verification ID to the next activity
                        Intent intent = new Intent(RegistrationActivity.this, CodeVerificationActivity.class);
                        intent.putExtra("verificationId", verificationId);
                        intent.putExtra("phoneNumber", phone);
                        intent.putExtra("forceResendingToken", forceResendingToken);
                        dismissLoadingScreen();
                        // Dismiss the loading screen
                        startActivity(intent);

                        finish();
                    }
                })
                .build();

        PhoneAuthProvider.verifyPhoneNumber(options);
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
