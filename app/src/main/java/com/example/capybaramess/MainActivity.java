package com.example.capybaramess;

import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.ContactsContract;
import android.util.Log;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private RecyclerView contactsRecyclerView;
    private SmsObserver smsObserver;

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
        setupActionBar();
        setupRecyclerView();
    }

    private void setupActionBar() {
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        View customView = LayoutInflater.from(this).inflate(R.layout.action_bar_main_activity, null);
        TextView titleText = customView.findViewById(R.id.actionbar_title);
        titleText.setText("Chats");
        getSupportActionBar().setCustomView(customView);
    }

    private void setupRecyclerView() {
        contactsRecyclerView = findViewById(R.id.contactsRecyclerView);
        contactsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        ContactsAdapter adapter = new ContactsAdapter(this, getSMSConversations().toArray(new Contact[0]));  // Data now comes sorted
        contactsRecyclerView.setAdapter(adapter);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(contactsRecyclerView.getContext(),
                LinearLayoutManager.VERTICAL);
        dividerItemDecoration.setDrawable(ContextCompat.getDrawable(this, R.drawable.divider));
        contactsRecyclerView.addItemDecoration(dividerItemDecoration);
    }

    @Override
    protected void onResume() {
        super.onResume();
        smsObserver = new SmsObserver(new Handler(Looper.getMainLooper()));
        getContentResolver().registerContentObserver(Telephony.Sms.CONTENT_URI, true, smsObserver);
        // Refresh the conversation list every time the activity resumes
        updateConversationList();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (smsObserver != null) {
            getContentResolver().unregisterContentObserver(smsObserver);
        }
    }

    private class SmsObserver extends ContentObserver {
        public SmsObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            // Update the list on the UI thread
            runOnUiThread(() -> updateConversationList());
        }
    }

    private void updateConversationList() {
        ContactsAdapter adapter = (ContactsAdapter) contactsRecyclerView.getAdapter();
        if (adapter != null) {
            List<Contact> newConversations = getSMSConversations();
            adapter.setContacts(newConversations);  // Make sure setContacts accepts List<Contact>
            adapter.notifyDataSetChanged();
        }
    }

    private boolean hasNecessaryPermissions() {
        return ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED;
    }

    private Map<Long, Contact> fetchInboxConversations() {
        Map<Long, Contact> conversationMap = new HashMap<>();
        Uri allSmsUri = Telephony.Sms.CONTENT_URI;
        String[] projection = {
                Telephony.Sms.THREAD_ID,
                Telephony.Sms.ADDRESS,
                Telephony.Sms.DATE,
                Telephony.Sms.TYPE  // This column distinguishes between sent and received messages.
        };

        // Ensure that the messages are sorted in descending order by date so that the most recent ones come first
        try (Cursor cursor = getContentResolver().query(allSmsUri, projection, null, null, Telephony.Sms.DATE + " DESC")) {
            if (cursor != null) {
                int threadIdIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.THREAD_ID);
                int addressIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS);
                int dateIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE);
                int typeIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.TYPE);

                while (cursor.moveToNext()) {
                    long threadId = cursor.getLong(threadIdIdx);
                    String address = cursor.getString(addressIdx);
                    long date = cursor.getLong(dateIdx);
                    int type = cursor.getInt(typeIdx);

                    String name = getContactName(address);
                    // Update the Contact entry only if it's either not yet added or the current message is newer
                    if (!conversationMap.containsKey(threadId) || conversationMap.get(threadId).getDate() < date) {
                        conversationMap.put(threadId, new Contact(name, address, 0, date, type));
                    }
                }
            } else {
                Log.d("SMSQuery", "No SMS data found.");
            }
        } catch (Exception e) {
            Log.e("SMSQuery", "Error fetching SMS conversations", e);
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
                    if (snippet != null) {
                        Log.d("FormatSnippet", "Before formatSnippet - Thread ID: " + threadId + ", Snippet: " + snippet + ", Type: " + conversationMap.get(threadId).getType());
                        snippet = formatSnippet(snippet, conversationMap.get(threadId).getType());
                    }

                    Contact contact = conversationMap.get(threadId);
                    if (contact != null) {
                        contact.setSnippet(snippet);
                    }
                }
            }
        }
    }

    //adding You: TODO doesn't work as for now. Type is always 1 "Received" :c
    private String formatSnippet(String snippet, int type) {
        if (type == Telephony.TextBasedSmsColumns.MESSAGE_TYPE_SENT) {
            return "You: " + snippet;
        } else{
            return (snippet);
        }
    }

    private List<Contact> getSMSConversations() {
        Map<Long, Contact> conversationMap = fetchInboxConversations();
        fetchAndAddSnippetsToConversations(conversationMap);

        // Convert map values to a list and sort it
        ArrayList<Contact> sortedConversations = new ArrayList<>(conversationMap.values());
        Collections.sort(sortedConversations, (c1, c2) -> Long.compare(c2.getDate(), c1.getDate())); // Assuming Contact has a getDate() method returning the timestamp

        return sortedConversations;
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

