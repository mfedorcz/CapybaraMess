package com.example.capybaramess;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Telephony;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.content.ContextCompat;

import com.google.firebase.firestore.DocumentSnapshot;
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
        String recipient = AppConfig.checkAndAddCCToNumber(destinationField.getText().toString().trim());
        String message = messageField.getText().toString().trim();

        if (recipient.isEmpty()) {
            destinationField.setError("Recipient number or username is required.");
            destinationField.requestFocus();
            return;
        }
        if (message.isEmpty()) {
            messageField.setError("Provide message to be sent.");
            messageField.requestFocus();
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
                        String profileImageUrl = queryDocumentSnapshots.getDocuments().get(0).getString("profileImageUrl");
                        String username = queryDocumentSnapshots.getDocuments().get(0).getString("username");
                        openChatWindow(username != null ? username : recipient, message,profileImageUrl, ChatMessage.MessagePlatform.OTT, recipient);
                    } else {
                        checkByUsername(recipient, message);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to check recipient. Please try again.", Toast.LENGTH_SHORT).show();
                });
    }

    private void checkByUsername(String username, String message) {
        Log.d("Firestore", "Checking the Firestore for user: " + username);
        firestore.collection("users")
                .whereEqualTo("username", username)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        //TODO only one user per given username
                        DocumentSnapshot document = queryDocumentSnapshots.getDocuments().get(0);
                        String recipientNumber = document.getString("phoneNumber");
                        String profileImageUrl = queryDocumentSnapshots.getDocuments().get(0).getString("profileImageUrl");

                        if (recipientNumber != null && !recipientNumber.isEmpty()) {
                            openChatWindow(username, message,profileImageUrl, ChatMessage.MessagePlatform.OTT, recipientNumber);
                        } else {
                            // If no phone number is found, proceed with the only username
                            openChatWindow(username, message,profileImageUrl, ChatMessage.MessagePlatform.OTT,null);
                        }
                        Log.d("Firestore", "User found: " + username);
                    } else {
                        Log.d("Firestore", "User not found: " + username);
                        // Username not found, show SMS warning dialog
                        showSMSWarningDialog(username, message);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to check recipient. Please try again.", Toast.LENGTH_SHORT).show();
                });
    }

    private void showSMSWarningDialog(String recipient, String message) {
        // Inflate the custom view
        View customView = LayoutInflater.from(this).inflate(R.layout.dialog_sms_warning, null);

        // Build the dialog with the custom view
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(customView)
                .setPositiveButton("Yes", (dialog, which) -> openChatWindow(recipient, message, null, ChatMessage.MessagePlatform.SMS, recipient))
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();

        // Customizing buttons programmatically
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(this, R.color.colorOnPrimary));
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(this, R.color.colorOnPrimary));
    }

    private void openChatWindow(String recipient, String message,String profileImage, ChatMessage.MessagePlatform platform, String address) {
        if(recipient == null){
            recipient = address;
        }

        //Check if reciptientNumber has already an threadID in phone database
        Long threadID = getThreadIdForPhoneNumber(address);

        Contact contact = new Contact(recipient,"You: " + message,profileImage, System.currentTimeMillis(), Telephony.TextBasedSmsColumns.MESSAGE_TYPE_SENT,threadID!=null ? threadID : -1, address, platform != ChatMessage.MessagePlatform.SMS);

        Intent intent = new Intent(this, ConversationActivity.class);
        intent.putExtra("contact", contact);
        intent.putExtra("position", 0);
        intent.putExtra("message", message);

        intent.putExtra("newConversation", true);
        startActivity(intent);

        finish();
    }

    private Long getThreadIdForPhoneNumber(String phoneNumber) {
        Uri uri = Uri.parse("content://mms-sms/threadID");
        Uri.Builder uriBuilder = uri.buildUpon();
        uriBuilder.appendQueryParameter("recipient", phoneNumber);

        try (Cursor cursor = getContentResolver().query(uriBuilder.build(), new String[]{"_id"}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getLong(0);  // Return the THREAD_ID
            }
        } catch (Exception e) {
            Log.e("ThreadIDCheck", "Error retrieving thread ID", e);
        }
        return null;  // Return null if no THREAD_ID is found
    }
}