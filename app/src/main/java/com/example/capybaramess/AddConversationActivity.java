package com.example.capybaramess;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.google.firebase.firestore.FirebaseFirestore;

public class AddConversationActivity extends AppCompatActivity {

    private EditText destinationField;
    private EditText messageField;
    private AppCompatButton continueButton;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_conversation_activity);

        setupActionBar();

        firestore = FirebaseFirestore.getInstance();

        destinationField = findViewById(R.id.destinationField);
        messageField = findViewById(R.id.message);
        continueButton = findViewById(R.id.continueButton);

        continueButton.setOnClickListener(v -> sendMessage());
    }

    private void setupActionBar() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowCustomEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            View customView = LayoutInflater.from(this).inflate(R.layout.action_bar_add_conv, null);
            getSupportActionBar().setCustomView(customView);

            // Find the back button and set up the listener
            ImageButton backButton = customView.findViewById(R.id.backButton);
            backButton.setOnClickListener(v -> finish()); // Close the current activity and go back
        }
    }

    private void sendMessage() {
        String recipient = destinationField.getText().toString().trim();
        String message = messageField.getText().toString().trim();

        if (recipient.isEmpty() || message.isEmpty()) {
            Toast.makeText(this, "Please fill in both fields", Toast.LENGTH_SHORT).show();
            return;
        }

        canSendOTTMessage(recipient, message);
    }

    private void canSendOTTMessage(String recipient, String message) {
        firestore.collection("users")
                .whereEqualTo("phoneNumber", recipient)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        openChatWindow(recipient, message, ChatMessage.MessagePlatform.OTT);
                    } else {
                        checkByUsername(recipient, message);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to check recipient. Please try again.", Toast.LENGTH_SHORT).show();
                });
    }

    private void checkByUsername(String username, String message) {
        firestore.collection("users")
                .whereEqualTo("username", username)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        openChatWindow(username, message, ChatMessage.MessagePlatform.OTT);
                    } else {
                        showSMSWarningDialog(username, message);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to check recipient. Please try again.", Toast.LENGTH_SHORT).show();
                });
    }

    private void showSMSWarningDialog(String recipient, String message) {
        new AlertDialog.Builder(this)
                .setTitle("Send SMS")
                .setMessage("OTT messaging is not available for this contact. Would you like to send an SMS instead?")
                .setPositiveButton("Yes", (dialog, which) -> openChatWindow(recipient, message, ChatMessage.MessagePlatform.SMS))
                .setNegativeButton("No", null)
                .show();
    }

    private void openChatWindow(String recipient, String message, ChatMessage.MessagePlatform platform) {
        Intent intent = new Intent(this, ConversationActivity.class);
        intent.putExtra("recipient", recipient);
        intent.putExtra("message", message);
        intent.putExtra("platform", platform);

        startActivity(intent);

        finish(); // Optionally finish the activity
    }
}
