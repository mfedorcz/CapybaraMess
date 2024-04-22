package com.example.capybaramess;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.content.Intent;

public class MainActivity extends AppCompatActivity {

    Contact[] contacts = new Contact[]{
            new Contact("Alice", R.drawable.alice_image),
            new Contact("Bob", 0), // No image for Bob, will use default
            new Contact("Charlie", 0)
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check permissions at the start
        if (!hasNecessaryPermissions()) {
            // If not all permissions are granted, start the PermissionRequestActivity
            Intent intent = new Intent(this, PermissionRequestActivity.class);
            startActivity(intent);
            finish();  // Close MainActivity to prevent the user from using the app without permissions
            return;  // Stop further execution of this method
        }

        setContentView(R.layout.main_activity);

        // Setting up the custom ActionBar
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        View customView = LayoutInflater.from(this).inflate(R.layout.action_bar_main_activity, null);
        TextView titleText = customView.findViewById(R.id.actionbar_title);
        titleText.setText("Chats");
        getSupportActionBar().setCustomView(customView);

        RecyclerView contactsRecyclerView = findViewById(R.id.contactsRecyclerView);
        contactsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        ContactsAdapter adapter = new ContactsAdapter(this, contacts);
        contactsRecyclerView.setAdapter(adapter);

        // Adding an item decoration
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(contactsRecyclerView.getContext(),
                LinearLayoutManager.VERTICAL);
        dividerItemDecoration.setDrawable(ContextCompat.getDrawable(this, R.drawable.divider));
        contactsRecyclerView.addItemDecoration(dividerItemDecoration);
    }

    private boolean hasNecessaryPermissions() {
        return ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED;
    }
}
