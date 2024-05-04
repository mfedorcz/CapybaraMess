package com.example.capybaramess;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.concurrent.TimeUnit;

public class RegistrationActivity extends AppCompatActivity {

    private EditText editTextPhoneNumber;
    private Button buttonVerifyPhone;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.registration_activity);

        //Terms of service string formatting
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
                // Handling "Privacy Policy" click
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

        mAuth = FirebaseAuth.getInstance();

        editTextPhoneNumber = findViewById(R.id.phoneNumberInput);
        buttonVerifyPhone = findViewById(R.id.buttonSignIn);

        buttonVerifyPhone.setOnClickListener(v -> {
            String phone = editTextPhoneNumber.getText().toString().trim();
            if (!phone.isEmpty()) {
                sendVerificationCode(phone);
            } else {
                editTextPhoneNumber.setError("Phone number is required");
            }
        });
    }

    private void sendVerificationCode(String phone) {
        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(mAuth)
                .setPhoneNumber(phone)       // Phone number to verify
                .setTimeout(60L, TimeUnit.SECONDS) // Timeout and unit
                .setActivity(this)           // Activity (for callback binding)
                .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onVerificationCompleted(com.google.firebase.auth.PhoneAuthCredential phoneAuthCredential) {
                        // This method is called when the verification is done automatically
                    }

                    @Override
                    public void onVerificationFailed(com.google.firebase.FirebaseException e) {
                        Toast.makeText(RegistrationActivity.this, "Verification Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onCodeSent(String verificationId, PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                        super.onCodeSent(verificationId, forceResendingToken);
                        // Pass verification ID to the next activity
                        Intent intent = new Intent(RegistrationActivity.this, CodeVerificationActivity.class);
                        intent.putExtra("verificationId", verificationId);
                        startActivity(intent);
                    }
                })
                .build();
        PhoneAuthProvider.verifyPhoneNumber(options);
    }
}