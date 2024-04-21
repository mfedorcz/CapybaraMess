package com.example.capybaramess;

import android.os.Bundle;
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

    private List<ChatMessage> chatMessages = new ArrayList<>();
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

        populateMessages();
        messagesRecyclerView = findViewById(R.id.messagesRecyclerView);
        messagesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        MessagesAdapter adapter = new MessagesAdapter(this, chatMessages);
        messagesRecyclerView.setAdapter(adapter);
    }

    private void populateMessages() {
        chatMessages.add(new ChatMessage("1", "Alice", "Hello! How are you?", System.currentTimeMillis(), ChatMessage.MessageType.INCOMING));
        chatMessages.add(new ChatMessage("2", "Bob", "I'm fine, thanks! And you?", System.currentTimeMillis(), ChatMessage.MessageType.OUTGOING));
        chatMessages.add(new ChatMessage("3", "Alice", "I'm doing well, just been busy lately.Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore ", System.currentTimeMillis(), ChatMessage.MessageType.INCOMING));
        chatMessages.add(new ChatMessage("4", "Bob", "No problem at all, take your time!", System.currentTimeMillis(), ChatMessage.MessageType.OUTGOING));
    }
}