package com.example.capybaramess;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Telephony;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.content.Intent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

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

        // Use dynamic SMS data
        ContactsAdapter adapter = new ContactsAdapter(this, getSMSConversations().toArray(new Contact[0]));  // Convert list to array
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

    private Map<Long, Contact> fetchInboxConversations() {
        Map<Long, Contact> conversationMap = new HashMap<>();
        Uri inboxUri = Telephony.Sms.Inbox.CONTENT_URI;
        String[] inboxProjection = new String[]{
                Telephony.Sms.Inbox.THREAD_ID,
                Telephony.Sms.Inbox.ADDRESS
        };

        try (Cursor cursor = getContentResolver().query(inboxUri, inboxProjection, null, null, null)) {
            if (cursor != null) {
                int threadIdIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.Inbox.THREAD_ID);
                int addressIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.Inbox.ADDRESS);

                while (cursor.moveToNext()) {
                    long threadId = cursor.getLong(threadIdIdx);
                    String address = cursor.getString(addressIdx);
                    String name = getContactName(address);  // Lookup contact name using the phone number

                    if (!conversationMap.containsKey(threadId)) {
                        conversationMap.put(threadId, new Contact(name, "", 0));
                    }
                }
            }
        }
        return conversationMap;
    }

    private void fetchAndAddSnippetsToConversations(Map<Long, Contact> conversationMap) {
        Uri conversationsUri = Telephony.Sms.Conversations.CONTENT_URI;
        String[] convProjection = new String[]{
                Telephony.Sms.Conversations.THREAD_ID,
                Telephony.Sms.Conversations.SNIPPET
        };

        try (Cursor cursor = getContentResolver().query(conversationsUri, convProjection, null, null, null)) {
            if (cursor != null) {
                int threadIdIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.Conversations.THREAD_ID);
                int snippetIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.Conversations.SNIPPET);

                while (cursor.moveToNext()) {
                    long threadId = cursor.getLong(threadIdIdx);
                    String snippet = cursor.getString(snippetIdx);
                    if (snippet != null && snippet.length() > 40) {
                        snippet = snippet.substring(0, 40) + "...";  // Truncate snippet to 40 characters and add ellipsis
                    }

                    Contact contact = conversationMap.get(threadId);
                    if (contact != null) {
                        contact.setSnippet(snippet);
                    }
                }
            }
        }
    }

    private List<Contact> getSMSConversations() {
        Map<Long, Contact> conversationMap = fetchInboxConversations();
        fetchAndAddSnippetsToConversations(conversationMap);
        return new ArrayList<>(conversationMap.values());
    }

    private String getContactName(String phoneNumber) {
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));
        String projection[] = new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME};
        String contactName = null;
        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                contactName = cursor.getString(0);
            }
            cursor.close();
        }

        return contactName != null ? contactName : phoneNumber;  // Return the contact name if found, otherwise return the original number
    }
}