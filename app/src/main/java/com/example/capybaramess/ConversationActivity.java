package com.example.capybaramess;

import android.os.Bundle;
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

        RecyclerView messagesRecyclerView = findViewById(R.id.messagesRecyclerView);
        messagesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        MessagesAdapter adapter = new MessagesAdapter(this, messages);
        messagesRecyclerView.setAdapter(adapter);
    }
}