package com.example.capybaramess;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class ConversationActivity extends AppCompatActivity {

    private String[] messages = {
            "Hello! How are you?",
            "I'm fine, thanks! And you?",
            "I'm doing well, just been busy lately.",
            "No problem at all, take your time!"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversation);

        // Setting up the custom ActionBar
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        View customView = LayoutInflater.from(this).inflate(R.layout.action_bar_act_conversation, null);
        TextView titleText = customView.findViewById(R.id.actionbar_title);
        titleText.setText(getIntent().getStringExtra("contactName"));  // Setting the contact's name as title
        getSupportActionBar().setCustomView(customView);

        // Setup back button
        ImageView backButton = customView.findViewById(R.id.backButton);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // This will close the current activity and take you back to the previous activity
                finish();
            }
        });

        RecyclerView messagesRecyclerView = findViewById(R.id.messagesRecyclerView);
        messagesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        MessagesAdapter adapter = new MessagesAdapter(this, messages);
        messagesRecyclerView.setAdapter(adapter);
    }
}