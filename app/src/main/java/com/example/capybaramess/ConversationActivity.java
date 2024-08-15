package com.example.capybaramess;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Telephony;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.Manifest;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ConversationActivity extends AppCompatActivity {
    private static final int REQUEST_SEND_SMS = 123;
    private EditText messageEditText;
    private Button sendButton;
    private RecyclerView messagesRecyclerView;
    private List<ChatMessage> chatMessages;
    private MessagesAdapter adapter;
    private BroadcastReceiver smsReceiver;
    private FirebaseFirestore firestore;
    private Contact contact;
    private ExecutorService executorService;
    private String phoneNumber; // Store the device's phone number

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.conversation_activity);

        // Initialize the chatMessages list
        chatMessages = new ArrayList<>();

        // Initialize Firebase Firestore
        firestore = FirebaseFirestore.getInstance();

        // Initialize ExecutorService for background tasks
        executorService = Executors.newSingleThreadExecutor();

        // Retrieve the device's phone number
        phoneNumber = getDevicePhoneNumber();

        if (phoneNumber == null) {
            Log.e("ConversationActivity", "Phone number could not be retrieved, finishing activity.");
            finish();
            return;
        }

        // Get contact information (assumes this is passed via the Intent)
        contact = getIntent().getParcelableExtra("contact");

        if (contact == null) {
            Log.e("ConversationActivity", "Contact is null, finishing activity.");
            finish();
            return;
        }

        // Setting up the custom ActionBar
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        View customView = LayoutInflater.from(this).inflate(R.layout.action_bar_act_conversation, null);
        getSupportActionBar().setCustomView(customView);

        TextView titleText = customView.findViewById(R.id.actionbar_title);
        titleText.setText(contact.getName()); // Setting the contact's name as title

        ImageView backButton = customView.findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        messagesRecyclerView = findViewById(R.id.messagesRecyclerView);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);  // Start the layout from the bottom
        messagesRecyclerView.setLayoutManager(layoutManager);

        // Initialize the adapter with the chatMessages list
        adapter = new MessagesAdapter(this, chatMessages);
        messagesRecyclerView.setAdapter(adapter);

        setupRealTimeUpdates();

        messageEditText = findViewById(R.id.messageEditText);
        sendButton = findViewById(R.id.sendButton);

        sendButton.setOnClickListener(v -> {
            String messageText = messageEditText.getText().toString().trim();
            if (!messageText.isEmpty()) {
                // Create a new ChatMessage object
                ChatMessage newMessage = new ChatMessage(
                        phoneNumber,  // Sender ID is the device's phone number
                        contact.getAddress(),  // Address of the recipient
                        messageText,
                        System.currentTimeMillis(),
                        ChatMessage.MessageType.OUTGOING  // MessageType set to OUTGOING
                );

                // Adding message to the list and notify the adapter
                updateUIWithNewMessage(newMessage);

                // Clear the input field
                messageEditText.setText("");

                // Check permission and send the message
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, REQUEST_SEND_SMS);
                } else {
                    sendMessage(newMessage);
                }
            }
        });

        // Retrieving the threadId from the intent
        long threadId = contact.getThreadId();
        // Fetching and showing messages if a valid threadId is provided
        if (threadId != -1) {
            chatMessages.addAll(fetchMessages(threadId)); // Fetching messages for the given threadId
            // Fetch Firebase messages and merge them with SMS messages
            fetchMessagesFromFirebase();
            adapter.notifyDataSetChanged(); // Notify adapter that data has changed
        } else {
            Log.e("ConversationActivity", "Invalid threadId passed to ConversationActivity");
        }

        // Initialize BroadcastReceiver
        initializeSmsReceiver();
        registerReceiver(smsReceiver, new IntentFilter("android.provider.Telephony.SMS_RECEIVED"));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(smsReceiver);
        executorService.shutdown();
    }

    private String getDevicePhoneNumber() {
        // Get the device's phone number
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_PHONE_STATE}, 1);
            return null;
        }
        return telephonyManager.getLine1Number();
    }

    private void initializeSmsReceiver() {
        smsReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equals(intent.getAction())) {
                    Bundle bundle = intent.getExtras();
                    if (bundle != null) {
                        Object[] pdus = (Object[]) bundle.get("pdus");
                        String format = bundle.getString("format"); // Get the SMS message format

                        if (pdus != null) {
                            for (Object pdu : pdus) {
                                SmsMessage smsMessage;
                                smsMessage = SmsMessage.createFromPdu((byte[]) pdu, format);
                                String sender = smsMessage.getDisplayOriginatingAddress();
                                String messageBody = smsMessage.getMessageBody();
                                long timestamp = smsMessage.getTimestampMillis();

                                // Check if the message is from the same thread
                                if (sender.equals(contact.getAddress())) {
                                    ChatMessage newMessage = new ChatMessage(
                                            sender, phoneNumber, messageBody, timestamp, ChatMessage.MessageType.INCOMING
                                    );
                                    updateUIWithNewMessage(newMessage);
                                }
                            }
                        }
                    }
                }
            }
        };
    }

    private void updateUIWithNewMessage(ChatMessage message) {
        runOnUiThread(() -> {
            if (!chatMessages.contains(message)) {  // Check for duplicates
                chatMessages.add(message);
                chatMessages.sort(Comparator.comparing(ChatMessage::getTimestamp));
                adapter.notifyItemInserted(chatMessages.size() - 1);
                messagesRecyclerView.scrollToPosition(chatMessages.size() - 1);
            }
        });
    }

    private void sendMessage(ChatMessage message) {
        if (contact.isRegistered() && isNetworkAvailable()) {
            executorService.execute(() -> sendViaFirebase(message));
        } else {
            sendViaSMS(message);
        }
    }

    private void sendViaFirebase(ChatMessage message) {
        String conversationId = getConversationId(message.getSenderId(), message.getRecipientId());

        // Reference to the conversation document
        DocumentReference conversationRef = firestore.collection("conversations")
                .document(conversationId);

        // Create a map to represent the message data
        Map<String, Object> messageData = new HashMap<>();
        messageData.put("senderId", message.getSenderId());
        messageData.put("recipientId", message.getRecipientId());
        messageData.put("content", message.getContent());
        messageData.put("timestamp", message.getTimestamp());

        // Add the message to the messages sub-collection
        conversationRef.collection("messages")
                .add(messageData)
                .addOnSuccessListener(documentReference -> Log.d("Firebase", "Message sent successfully via Firebase."))
                .addOnFailureListener(e -> {
                    Log.e("Firebase", "Failed to send message via Firebase.", e);
                    // Fallback to SMS if Firebase sending fails
                    sendViaSMS(message);
                });

        // Update the conversation document with the last message and timestamp
        Map<String, Object> conversationData = new HashMap<>();
        conversationData.put("participants", Arrays.asList(message.getSenderId(), message.getRecipientId()));
        conversationData.put("lastMessage", message.getContent());
        conversationData.put("lastTimestamp", message.getTimestamp());

        conversationRef.set(conversationData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> Log.d("Firebase", "Conversation updated successfully."))
                .addOnFailureListener(e -> Log.e("Firebase", "Failed to update conversation.", e));
    }

    private String getConversationId(String senderId, String recipientId) {
        // This should generate a unique conversation ID based on the participants' phone numbers
        return senderId.compareTo(recipientId) < 0 ? senderId + "_" + recipientId : recipientId + "_" + senderId;
    }

    private void sendViaSMS(ChatMessage message) {
        SmsManager smsManager = SmsManager.getDefault();
        try {
            // Send a text message to the given number
            smsManager.sendTextMessage(message.getRecipientId(), null, message.getContent(), null, null);
            Log.d("SMS", "SMS sent successfully.");
        } catch (Exception e) {
            Log.e("SMS", "SMS failed to send.", e);
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private List<ChatMessage> fetchMessages(long threadId) {
        List<ChatMessage> messages = new ArrayList<>();
        Uri uri = Telephony.Sms.CONTENT_URI;
        String[] projection = {
                Telephony.Sms._ID,
                Telephony.Sms.ADDRESS,
                Telephony.Sms.BODY,
                Telephony.Sms.DATE,
                Telephony.Sms.TYPE
        };
        String selection = Telephony.Sms.THREAD_ID + " = ?";
        String[] selectionArgs = {String.valueOf(threadId)};
        String sortOrder = Telephony.Sms.DATE + " ASC";

        try (Cursor cursor = getContentResolver().query(uri, projection, selection, selectionArgs, sortOrder)) {
            if (cursor != null) {
                int idIdx = cursor.getColumnIndexOrThrow(Telephony.Sms._ID);
                int addressIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS);
                int bodyIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY);
                int dateIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE);
                int typeIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.TYPE);

                while (cursor.moveToNext()) {
                    if (idIdx != -1 && addressIdx != -1 && bodyIdx != -1 && dateIdx != -1 && typeIdx != -1) {
                        String id = cursor.getString(idIdx);
                        String sender = cursor.getString(addressIdx);
                        String content = cursor.getString(bodyIdx);
                        long timestamp = cursor.getLong(dateIdx);
                        int type = cursor.getInt(typeIdx);

                        String receiver = type == Telephony.Sms.MESSAGE_TYPE_SENT ? contact.getAddress() : phoneNumber;
                        messages.add(new ChatMessage(sender, receiver, content, timestamp, type == Telephony.Sms.MESSAGE_TYPE_SENT ? ChatMessage.MessageType.OUTGOING : ChatMessage.MessageType.INCOMING));
                    } else {
                        Log.e("SMS Fetch", "One of the required columns is missing in the SMS database.");
                    }
                }
            }
        } catch (Exception e) {
            Log.e("ConversationActivity", "Error fetching SMS messages", e);
        }
        return messages;
    }
    private void fetchMessagesFromFirebase() {
        String conversationId = getConversationId(phoneNumber, contact.getAddress());

        firestore.collection("conversations")
                .document(conversationId)
                .collection("messages")
                .orderBy("timestamp")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                        String senderId = document.getString("senderId");
                        String recipientId = document.getString("recipientId");
                        String content = document.getString("content");
                        long timestamp = document.getLong("timestamp");

                        ChatMessage.MessageType messageType = senderId.equals(phoneNumber) ? ChatMessage.MessageType.OUTGOING : ChatMessage.MessageType.INCOMING;
                        ChatMessage newMessage = new ChatMessage(senderId, recipientId, content, timestamp, messageType);

                        updateUIWithNewMessage(newMessage);  // This method now checks for duplicates
                    }
                })
                .addOnFailureListener(e -> Log.e("Firebase", "Failed to fetch messages from Firebase", e));
    }

    private void setupRealTimeUpdates() {
        String conversationId = getConversationId(phoneNumber, contact.getAddress());

        firestore.collection("conversations")
                .document(conversationId)
                .collection("messages")
                .orderBy("timestamp")
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    if (e != null) {
                        Log.e("Firebase", "Listen failed.", e);
                        return;
                    }

                    if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                        for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                            String senderId = document.getString("senderId");
                            String recipientId = document.getString("recipientId");
                            String content = document.getString("content");
                            long timestamp = document.getLong("timestamp");

                            ChatMessage.MessageType messageType = senderId.equals(phoneNumber) ? ChatMessage.MessageType.OUTGOING : ChatMessage.MessageType.INCOMING;
                            ChatMessage newMessage = new ChatMessage(senderId, recipientId, content, timestamp, messageType);

                            updateUIWithNewMessage(newMessage);  // This method now checks for duplicates
                        }
                    }
                });
    }
}
