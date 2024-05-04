package com.example.capybaramess;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;

public class CodeVerificationActivity extends AppCompatActivity {

    private EditText codeInput;
    private Button buttonVerifyCode;
    private String verificationId;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.verification_activity);

        mAuth = FirebaseAuth.getInstance();

        codeInput = findViewById(R.id.codeInput);
        buttonVerifyCode = findViewById(R.id.buttonVerifyCode);

        verificationId = getIntent().getStringExtra("verificationId");

        buttonVerifyCode.setOnClickListener(v -> {
            String code = codeInput.getText().toString().trim();
            if (!code.isEmpty()) {
                verifyVerificationCode(code);
            } else {
                codeInput.setError("Code is required");
            }
        });
    }

    private void verifyVerificationCode(String code) {
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, code);
        signInWithPhoneAuthCredential(credential);
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Verification completed successfully
                        startActivity(new Intent(CodeVerificationActivity.this, SetPasswordActivity.class));
                    } else {
                        // Verification failed
                        Toast.makeText(CodeVerificationActivity.this, "Verification Failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}