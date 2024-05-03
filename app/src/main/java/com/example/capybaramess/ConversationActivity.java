package com.example.capybaramess;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Telephony;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
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

import java.util.ArrayList;
import java.util.List;

public class ConversationActivity extends AppCompatActivity {
    private static final int REQUEST_SEND_SMS = 123;
    private EditText messageEditText;
    private Button sendButton;
    private RecyclerView messagesRecyclerView;
    private List<ChatMessage> chatMessages;
    private MessagesAdapter adapter;
    private BroadcastReceiver smsReceiver;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversation);

        // Setting up the custom ActionBar
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        View customView = LayoutInflater.from(this).inflate(R.layout.action_bar_act_conversation, null);
        getSupportActionBar().setCustomView(customView);

        TextView titleText = customView.findViewById(R.id.actionbar_title);
        titleText.setText(getIntent().getStringExtra("contactName")); // Setting the contact's name as title

        ImageView backButton = customView.findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        messagesRecyclerView = findViewById(R.id.messagesRecyclerView);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);  // Start the layout from the bottom
        messagesRecyclerView.setLayoutManager(layoutManager);

        messageEditText = findViewById(R.id.messageEditText);
        sendButton = findViewById(R.id.sendButton);

        String address = getIntent().getStringExtra("address");
        sendButton.setOnClickListener(v -> {
            String messageText = messageEditText.getText().toString().trim();
            if (!messageText.isEmpty()) {
                // Create a new ChatMessage object
                ChatMessage newMessage = new ChatMessage(
                        "unique_id",  // TODO generating/fetching a unique ID
                        address,  //Address of the receiver
                        messageText,
                        System.currentTimeMillis(),
                        ChatMessage.MessageType.OUTGOING
                );

                // Adding message to the list and notify the adapter
                updateUIWithNewMessage(newMessage);

                // Clear the input field
                messageEditText.setText("");

                if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, REQUEST_SEND_SMS);
                } else {
                    sendMessage(newMessage);
                }
            }
        });

        // Retrieving the threadId from the intent
        long threadId = getIntent().getLongExtra("threadId", -1);
        // Fetching and showing messages if a valid threadId is provided
        if (threadId != -1) {
            chatMessages = fetchMessages(threadId); // Fetching messages for the given threadId
            adapter = new MessagesAdapter(this, chatMessages);
            messagesRecyclerView.setAdapter(adapter);
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
                                if (sender.equals(getIntent().getStringExtra("address"))) {
                                    ChatMessage newMessage = new ChatMessage(
                                            "unique_id", sender, messageBody, timestamp, ChatMessage.MessageType.INCOMING
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
            chatMessages.add(message);
            adapter.notifyItemInserted(chatMessages.size() - 1);
            messagesRecyclerView.scrollToPosition(chatMessages.size() - 1);
        });
    }
    private void sendMessage(ChatMessage message) {
        SmsManager smsManager = SmsManager.getDefault();
        try {
            // Send a text message to the given number
            smsManager.sendTextMessage(message.getSender(), null, message.getContent(), null, null);
            Log.d("SMS", "SMS sent successfully.");
        } catch (Exception e) {
            Log.e("SMS", "SMS failed to send.", e);
        }
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

                        ChatMessage.MessageType messageType = (type == Telephony.Sms.MESSAGE_TYPE_SENT) ?
                                ChatMessage.MessageType.OUTGOING : ChatMessage.MessageType.INCOMING;

                        messages.add(new ChatMessage(id, sender, content, timestamp, messageType));
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
}