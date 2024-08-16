package com.example.capybaramess;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.ContactsContract;
import android.provider.Telephony;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private RecyclerView contactsRecyclerView;
    private FloatingActionButton fabAdd;
    private SmsObserver smsObserver;
    private ScheduledExecutorService executorService;
    private FirebaseFirestore firestore;
    private String phoneNumber;
    private Map<String, Contact> firebaseCache = new HashMap<>(); // Cache for Firebase data
    private Map<String, Integer> contactPositionMap = new HashMap<>(); // Cache for contact positions

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initializeFirebase();
        initializeExecutorService();

        if (!hasNecessaryPermissions()) {
            requestPermissionsAndExit();
            return;
        }

        initializePhoneNumber();
        if (!isPhoneNumberValid()) {
            return;
        }

        if (!isUserSignedIn()) {
            redirectToRegistration();
            return;
        }

        setContentView(R.layout.main_activity);

        initializeUIComponents();
        setupActionBar();
        setupRecyclerView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerSmsObserver();
        updateConversationList();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterSmsObserver();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        shutdownExecutorService();
    }

    private void initializeFirebase() {
        FirebaseApp.initializeApp(this);
        firestore = FirebaseFirestore.getInstance();
    }

    private void initializeExecutorService() {
        executorService = Executors.newScheduledThreadPool(4); // 4 threads for concurrency
    }

    private void shutdownExecutorService() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    private void initializePhoneNumber() {
        phoneNumber = getDevicePhoneNumber();
        String firebasePhoneNumber = FirebaseAuth.getInstance().getCurrentUser().getPhoneNumber();
        if (firebasePhoneNumber == null || !firebasePhoneNumber.equals(phoneNumber)) {
            Log.w("PhoneNumberCheck", "Phone number mismatch or phone number is missing!");
        } else {
            Log.d("PhoneNumberCheck", "Phone number matches.");
        }
        AppConfig.setPhoneNumber(phoneNumber);
    }

    private void initializeUIComponents() {
        fabAdd = findViewById(R.id.fab_add);
        fabAdd.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, AddConversationActivity.class)));
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
        contactsRecyclerView.setAdapter(new ContactsAdapter(this, new Contact[0]));

        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(contactsRecyclerView.getContext(), LinearLayoutManager.VERTICAL);
        dividerItemDecoration.setDrawable(ContextCompat.getDrawable(this, R.drawable.divider));
        contactsRecyclerView.addItemDecoration(dividerItemDecoration);
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

    private void registerSmsObserver() {
        if (smsObserver == null) {
            smsObserver = new SmsObserver(new Handler(Looper.getMainLooper()));
            getContentResolver().registerContentObserver(Telephony.Sms.CONTENT_URI, true, smsObserver);
        }
    }

    private void unregisterSmsObserver() {
        if (smsObserver != null) {
            getContentResolver().unregisterContentObserver(smsObserver);
            smsObserver = null;
        }
    }

    private boolean hasNecessaryPermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissionsAndExit() {
        Intent intent = new Intent(this, PermissionRequestActivity.class);
        startActivity(intent);
        finish();
    }

    private boolean isPhoneNumberValid() {
        if (phoneNumber == null) {
            Log.e("ConversationActivity", "Phone number could not be retrieved, finishing activity.");
            finish();
            return false;
        }
        return true;
    }

    private boolean isUserSignedIn() {
        return FirebaseAuth.getInstance().getCurrentUser() != null;
    }

    private void redirectToRegistration() {
        Intent intent = new Intent(this, RegistrationActivity.class);
        startActivity(intent);
        finish();
    }

    private void updateConversationList() {
        executorService.execute(() -> {
            Map<Long, Contact> conversationMap = fetchInboxConversations();
            fetchFirebaseConversations(conversationMap);
        });
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

                while (cursor.moveToNext()) {
                    long threadId = cursor.getLong(threadIdIdx);
                    String address = AppConfig.checkAndAddCCToNumber(cursor.getString(addressIdx));
                    long date = cursor.getLong(dateIdx);
                    int type = cursor.getInt(typeIdx);

                    String name = getContactName(address);

                    Contact contact = new Contact(name, "???", null, date, type, threadId, address, false);

                    fetchAndAddSmsSnippet(contact);

                    conversationMap.put(threadId, contact);
                }
            }
        } catch (Exception e) {
            Log.e("SMSQuery", "Error fetching SMS conversations: " + e.getMessage(), e);
        }

        return conversationMap;
    }

    private void fetchFirebaseConversations(Map<Long, Contact> conversationMap) {
        String currentPhoneNumber = AppConfig.checkAndAddCCToNumber(AppConfig.getPhoneNumber());

        firestore.collection("conversations")
                .whereArrayContains("participants", currentPhoneNumber)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        for (DocumentSnapshot document : task.getResult()) {
                            String lastMessage = document.getString("lastMessage");
                            String lastSenderId = document.getString("lastSenderId");
                            String otherParticipant = getOtherParticipant(document.get("participants"), currentPhoneNumber);
                            Long timestamp = document.getLong("lastTimestamp");

                            if (otherParticipant != null) {
                                String normalizedParticipant = AppConfig.checkAndAddCCToNumber(otherParticipant);

                                // Check cache first
                                if (firebaseCache.containsKey(normalizedParticipant)) {
                                    updateUI(conversationMap);
                                    continue;
                                }

                                String name = getContactName(normalizedParticipant);

                                Contact matchingContact = conversationMap.values().stream()
                                        .filter(contact -> contact.getAddress().equals(normalizedParticipant))
                                        .findFirst().orElse(null);

                                int type = lastSenderId.equals(currentPhoneNumber)
                                        ? Telephony.TextBasedSmsColumns.MESSAGE_TYPE_SENT
                                        : Telephony.TextBasedSmsColumns.MESSAGE_TYPE_INBOX;

                                if (matchingContact != null) {
                                    if (timestamp != null && timestamp > matchingContact.getTimestamp()) {
                                        String formattedSnippet = formatSnippet(lastMessage, type);

                                        matchingContact.setSnippet(formattedSnippet);
                                        matchingContact.setTimestamp(timestamp);
                                    }
                                    matchingContact.setRegistered(true);
                                    fetchProfileImageAndUsernameFromFirebase(matchingContact);
                                } else {
                                    // Create a new Contact object and format the snippet
                                    String formattedSnippet = formatSnippet(lastMessage, type);

                                    Contact contact = new Contact(name, formattedSnippet, null, timestamp != null ? timestamp : 0, -1, -1, normalizedParticipant, true);
                                    fetchProfileImageAndUsernameFromFirebase(contact);
                                    conversationMap.put(contact.getThreadId(), contact);
                                }
                                // Cache the fetched contact
                                firebaseCache.put(normalizedParticipant, matchingContact);
                            }
                        }
                        updateUI(conversationMap);

                    } else {
                        Log.e("FirebaseQuery", "Failed to fetch Firebase conversations: " + (task.getException() != null ? task.getException().getMessage() : "Unknown error"));
                    }
                });
    }

    private void updateUI(Map<Long, Contact> conversationMap) {
        List<Contact> sortedContacts = new ArrayList<>(conversationMap.values());
        sortedContacts.sort((c1, c2) -> Long.compare(c2.getTimestamp(), c1.getTimestamp()));

        runOnUiThread(() -> {
            ContactsAdapter adapter = (ContactsAdapter) contactsRecyclerView.getAdapter();
            if (adapter != null) {
                Contact[] contactsArray = sortedContacts.toArray(new Contact[0]);
                adapter.setContacts(contactsArray);  // This will use DiffUtil to update the RecyclerView efficiently
            }
        });
    }

    private void fetchAndAddSmsSnippet(Contact contact) {
        Uri smsUri = Telephony.Sms.CONTENT_URI;
        String[] smsProjection = {
                Telephony.Sms.THREAD_ID,
                Telephony.Sms.BODY,
                Telephony.Sms.DATE,
                Telephony.Sms.TYPE
        };

        String selection = Telephony.Sms.THREAD_ID + " = ?";
        String[] selectionArgs = {String.valueOf(contact.getThreadId())};

        try (Cursor cursor = getContentResolver().query(smsUri, smsProjection, selection, selectionArgs, Telephony.Sms.DATE + " DESC")) {
            if (cursor != null && cursor.moveToFirst()) {
                String snippet = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.BODY));
                long smsTimestamp = cursor.getLong(cursor.getColumnIndexOrThrow(Telephony.Sms.DATE));
                int smsType = cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Sms.TYPE));

                contact.setSnippet(formatSnippet(snippet, smsType));
                contact.setTimestamp(smsTimestamp);
            }
        } catch (Exception e) {
            Log.e("SMSQuery", "Error fetching SMS snippet: " + e.getMessage(), e);
        }
    }

    private void fetchProfileImageAndUsernameFromFirebase(Contact contact) {
        String phoneNumber = AppConfig.checkAndAddCCToNumber(contact.getAddress());

        // Check if data is already cached
        if (firebaseCache.containsKey(phoneNumber)) {
            Contact cachedContact = firebaseCache.get(phoneNumber);
            contact.setProfileImage(cachedContact.getProfileImage());
            contact.setName(cachedContact.getName());
            runOnUiThread(() -> {
                ContactsAdapter adapter = (ContactsAdapter) contactsRecyclerView.getAdapter();
                if (adapter != null) {
                    int position = findContactPosition(contact);
                    if (position != RecyclerView.NO_POSITION) {
                        adapter.notifyItemChanged(position);
                    }
                }
            });
            return;
        }

        firestore.collection("users")
                .whereEqualTo("phoneNumber", phoneNumber)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        String profileImageUrl = querySnapshot.getDocuments().get(0).getString("profileImageUrl");
                        String profileUsername = querySnapshot.getDocuments().get(0).getString("username");

                        boolean dataChanged = false;

                        if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                            contact.setProfileImage(profileImageUrl);
                            dataChanged = true;
                        }
                        if (profileUsername != null && !profileUsername.isEmpty()) {
                            contact.setName(profileUsername);
                            dataChanged = true;
                        }

                        if (dataChanged) {
                            firebaseCache.put(phoneNumber, contact); // Cache the fetched data
                            runOnUiThread(() -> {
                                ContactsAdapter adapter = (ContactsAdapter) contactsRecyclerView.getAdapter();
                                if (adapter != null) {
                                    int position = findContactPosition(contact);
                                    if (position != RecyclerView.NO_POSITION) {
                                        adapter.notifyItemChanged(position);
                                    }
                                }
                            });
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e("FirebaseQuery", "Failed to fetch profile info for " + phoneNumber + ": " + e.getMessage(), e));
    }

    private int findContactPosition(Contact contact) {
        // Use the cached map for quick position lookup
        if (contactPositionMap.containsKey(contact.getAddress())) {
            return contactPositionMap.get(contact.getAddress());
        }

        ContactsAdapter adapter = (ContactsAdapter) contactsRecyclerView.getAdapter();
        if (adapter != null) {
            for (int i = 0; i < adapter.getItemCount(); i++) {
                if (adapter.getContacts()[i].getAddress().equals(contact.getAddress())) {
                    contactPositionMap.put(contact.getAddress(), i); // Cache the position
                    return i;
                }
            }
        }
        return RecyclerView.NO_POSITION;
    }

    private String formatSnippet(String snippet, int type) {
        return type == Telephony.TextBasedSmsColumns.MESSAGE_TYPE_SENT ? "You: " + snippet : snippet;
    }

    private String getOtherParticipant(Object participants, String currentPhoneNumber) {
        if (participants instanceof List<?>) {
            for (Object participant : (List<?>) participants) {
                if (!participant.equals(currentPhoneNumber)) {
                    return participant.toString();
                }
            }
        }
        return null;
    }

    private String getContactName(String phoneNumber) {
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));
        String[] projection = {ContactsContract.PhoneLookup.DISPLAY_NAME};
        String contactName = null;

        try (Cursor cursor = getContentResolver().query(uri, projection, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                contactName = cursor.getString(0);
            }
        } catch (Exception e) {
            Log.e("ContactQuery", "Error fetching contact name for " + phoneNumber + ": " + e.getMessage(), e);
        }

        return contactName != null ? contactName : phoneNumber;
    }

    private String getDevicePhoneNumber() {
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_PHONE_STATE}, 1);
            return null;
        }
        return telephonyManager.getLine1Number();
    }
}
