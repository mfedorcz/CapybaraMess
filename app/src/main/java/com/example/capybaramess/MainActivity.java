package com.example.capybaramess;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class MainActivity extends AppCompatActivity {

    private final String[] contacts = {"Alice", "Bob", "Charlie", "David"};

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
