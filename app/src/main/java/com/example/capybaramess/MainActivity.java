package com.example.capybaramess;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class MainActivity extends AppCompatActivity {

    private String[] contacts = {"Alice", "Bob", "Charlie", "David"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        RecyclerView contactsRecyclerView = findViewById(R.id.contactsRecyclerView);
        contactsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        ContactsAdapter adapter = new ContactsAdapter(this, contacts);
        contactsRecyclerView.setAdapter(adapter);

        //contactsListView.setOnItemClickListener((parent, view, position, id) -> {
        //    Intent intent = new Intent(MainActivity.this, ConversationActivity.class);
        //    intent.putExtra("contactName", contacts[position]);
        //    startActivity(intent);
        //});
    }
}
