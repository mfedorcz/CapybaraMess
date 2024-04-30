package com.example.capybaramess;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Telephony;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ConversationActivity extends AppCompatActivity {

    private RecyclerView messagesRecyclerView;

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

        // Retrieving the threadId from the intent
        long threadId = getIntent().getLongExtra("threadId", -1);
        // Fetching and showing messages if a valid threadId is provided
        if (threadId != -1) {
            List<ChatMessage> chatMessages = fetchMessages(threadId); // Fetching messages for the given threadId
            MessagesAdapter adapter = new MessagesAdapter(this, chatMessages);
            messagesRecyclerView.setAdapter(adapter);
        } else {
            Log.e("ConversationActivity", "Invalid threadId passed to ConversationActivity");
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