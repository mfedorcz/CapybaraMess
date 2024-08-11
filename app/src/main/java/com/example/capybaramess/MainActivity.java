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
import android.widget.ImageView;
import android.widget.TextView;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Telephony;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.content.Intent;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class MainActivity extends AppCompatActivity {

    private RecyclerView contactsRecyclerView;
    private SmsObserver smsObserver;
    private ImageView settingsIcon;
    private ExecutorService executorService;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize Firebase
        FirebaseApp.initializeApp(this);
        firestore = FirebaseFirestore.getInstance();

        // Initialize ExecutorService
        executorService = Executors.newSingleThreadExecutor();

        // Check permissions at the start
        if (!hasNecessaryPermissions()) {
            Intent intent = new Intent(this, PermissionRequestActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        // Check if the user is signed in
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Intent intent = new Intent(this, RegistrationActivity.class);
            startActivity(intent);
            finish();
            return;
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
        settingsIcon = findViewById(R.id.actionbar_settings);
        settingsIcon.setVisibility(View.VISIBLE);
        settingsIcon.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, SettingsActivity.class)));
    }

    private void setupRecyclerView() {
        contactsRecyclerView = findViewById(R.id.contactsRecyclerView);
        contactsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        ContactsAdapter adapter = new ContactsAdapter(this, new Contact[0]);
        contactsRecyclerView.setAdapter(adapter);

        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(contactsRecyclerView.getContext(), LinearLayoutManager.VERTICAL);
        dividerItemDecoration.setDrawable(ContextCompat.getDrawable(this, R.drawable.divider));
        contactsRecyclerView.addItemDecoration(dividerItemDecoration);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (smsObserver == null) {
            smsObserver = new SmsObserver(new Handler(Looper.getMainLooper()));
            getContentResolver().registerContentObserver(Telephony.Sms.CONTENT_URI, true, smsObserver);
        }
        updateConversationList();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (smsObserver != null) {
            getContentResolver().unregisterContentObserver(smsObserver);
            smsObserver = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown(); //Shut down the executor service when activity is destroyed
    }

    private class SmsObserver extends ContentObserver {
        public SmsObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            runOnUiThread(MainActivity.this::updateConversationList);
        }
    }

    private void updateConversationList() {
        executorService.execute(() -> {
            List<Contact> newConversations = getSMSConversations();
            runOnUiThread(() -> {
                ContactsAdapter adapter = (ContactsAdapter) contactsRecyclerView.getAdapter();
                if (adapter != null) {
                    adapter.setContacts(newConversations);
                    adapter.notifyDataSetChanged();
                }
            });
        });
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
                Telephony.Sms.TYPE
        };

        try (Cursor cursor = getContentResolver().query(allSmsUri, projection, null, null, Telephony.Sms.DATE + " DESC")) {
            if (cursor != null) {
                int threadIdIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.THREAD_ID);
                int addressIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS);
                int dateIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE);
                int typeIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.TYPE);

                int position = 0;
                while (cursor.moveToNext()) {
                    long threadId = cursor.getLong(threadIdIdx);
                    String address = cursor.getString(addressIdx);
                    long date = cursor.getLong(dateIdx);
                    int type = cursor.getInt(typeIdx);

                    String name = getContactName(address);

                    Contact contact = new Contact(name, "???", null, date, type, threadId, address);

                    conversationMap.put(threadId, contact);

                    // Check Firestore for user details based on phone number
                    checkFirestoreForUserDetails(contact, position);

                    position++;
                }
            }
        } catch (Exception e) {
            Log.e("SMSQuery", "Error fetching SMS conversations", e);
        }
        return conversationMap;
    }


    private void checkFirestoreForUserDetails(Contact contact, int position) {
        executorService.execute(() -> {
            firestore.collection("users")
                    .whereEqualTo("phoneNumber", contact.getAddress())
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                            DocumentSnapshot document = task.getResult().getDocuments().get(0);
                            String username = document.getString("username");
                            String profileImageUrl = document.getString("profileImageUrl");

                            if (username != null) {
                                contact.setName(username); // Update name to username
                            }

                            if (profileImageUrl != null) {
                                contact.setProfileImage(profileImageUrl); // Set profile image URL
                            }

                            // Update the UI with the new contact details on the main thread
                            runOnUiThread(() -> {
                                ContactsAdapter adapter = (ContactsAdapter) contactsRecyclerView.getAdapter();
                                if (adapter != null) {
                                    adapter.notifyItemChanged(position); // Notify adapter to refresh just this item
                                }
                            });
                        }
                    });
        });
    }



    private void fetchAndAddSnippetsToConversations(Map<Long, Contact> conversationMap) {
        Uri conversationsUri = Telephony.Sms.Conversations.CONTENT_URI;
        String[] convProjection = {
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

    private String formatSnippet(String snippet, int type) {
        return type == Telephony.TextBasedSmsColumns.MESSAGE_TYPE_SENT ? "You: " + snippet : snippet;
    }

    private List<Contact> getSMSConversations() {
        Map<Long, Contact> conversationMap = fetchInboxConversations();
        fetchAndAddSnippetsToConversations(conversationMap);

        return conversationMap.values().stream()
                .sorted((c1, c2) -> Long.compare(c2.getDate(), c1.getDate()))
                .collect(Collectors.toList());
    }

    private String getContactName(String phoneNumber) {
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));
        String[] projection = {ContactsContract.PhoneLookup.DISPLAY_NAME};
        String contactName = null;

        try (Cursor cursor = getContentResolver().query(uri, projection, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                contactName = cursor.getString(0);
            }
        }

        return contactName != null ? contactName : phoneNumber;
    }
}
