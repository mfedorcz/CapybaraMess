package com.example.capybaramess;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;

public class CodeVerificationActivity extends AppCompatActivity {

    private EditText[] codeInputs = new EditText[6];
    private String verificationId;
    private FirebaseAuth mAuth;

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

        verificationId = getIntent().getStringExtra("verificationId");

        // Set listeners to each input to move focus and trigger verification after last digit
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
    }

    private void verifyVerificationCode() {
        StringBuilder code = new StringBuilder();
        for (EditText editText : codeInputs) {
            code.append(editText.getText().toString().trim());
        }
        if (!code.toString().isEmpty()) {
            PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, code.toString());
            signInWithPhoneAuthCredential(credential);
        } else {
            codeInputs[0].setError("Complete code is required");
        }
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Intent intent = new Intent(CodeVerificationActivity.this, SetPasswordActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(CodeVerificationActivity.this, "Verification failed.", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}