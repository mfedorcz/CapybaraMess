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
import android.widget.ImageView;
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
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private RecyclerView contactsRecyclerView;
    private FloatingActionButton fabAdd;
    private SmsObserver smsObserver;
    private ScheduledExecutorService executorService;
    private FirebaseFirestore firestore;
    private String phoneNumber;
    private Map<String, Contact> firebaseCache = new HashMap<>(); // Cache for Firebase data
    private Map<String, Integer> contactPositionMap = new HashMap<>(); // Cache for contact positions
    private Map<Long, Contact> cachedConversationMap; // In-memory cache for conversations
    private boolean isFirstResume = true;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "onCreate: Initializing MainActivity");

        initializeFirebase();
        initializeExecutorService();

        if (!hasNecessaryPermissions()) {
            Log.w(TAG, "onCreate: Necessary permissions are missing, requesting them.");
            requestPermissionsAndExit();
            return;
        }

        if (!isUserSignedIn()) {
            Log.d(TAG, "onCreate: User not signed in, redirecting to RegistrationActivity.");
            redirectToRegistration();
            return;
        }

        initializePhoneNumber();
        if (!isPhoneNumberValid()) {
            Log.e(TAG, "onCreate: Phone number is invalid, stopping execution.");
            return;
        }

        setContentView(R.layout.main_activity);
        initializeUIComponents();
        setupActionBar();
        setupRecyclerView();

        Log.d(TAG, "onStart: Initiating conversation list.");
        initiateConversationList(conversationMap -> {
            Log.d(TAG, "Conversation list loaded, updating UI.");
            updateUI(conversationMap);
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: Registering all observers and checking for updates.");
        registerObservers();

        // Skip update check if this is the first resume after onCreate
        if (isFirstResume) {
            isFirstResume = false;  // Reset the flag after the first resume
        } else {
            // Check for updates instead of recreating the list
            checkForConversationUpdates(hasChanges -> {
                if (hasChanges) {
                    updateUI(cachedConversationMap);
                }
            });
        }
    }


    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: Unregistering all observers.");
        unregisterObservers(); // Unregister both SMS and Firebase observers
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: Shutting down executor service.");
        shutdownExecutorService();
    }

    private void initializeFirebase() {
        Log.d(TAG, "initializeFirebase: Initializing Firebase.");
        FirebaseApp.initializeApp(this);
        firestore = FirebaseFirestore.getInstance();
    }

    private void initializeExecutorService() {
        Log.d(TAG, "initializeExecutorService: Initializing executor service.");
        executorService = Executors.newScheduledThreadPool(4); // 4 threads for concurrency
    }

    private void shutdownExecutorService() {
        if (executorService != null && !executorService.isShutdown()) {
            Log.d(TAG, "shutdownExecutorService: Attempting to shut down executor service.");
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                    Log.w(TAG, "shutdownExecutorService: Executor service did not terminate in time, forcing shutdown.");
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                Log.e(TAG, "shutdownExecutorService: Interrupted while shutting down executor service.", e);
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    private void initializePhoneNumber() {
        Log.d(TAG, "initializePhoneNumber: Fetching device phone number.");
        phoneNumber = getDevicePhoneNumber();
        String firebasePhoneNumber = FirebaseAuth.getInstance().getCurrentUser().getPhoneNumber();
        if (firebasePhoneNumber == null || !firebasePhoneNumber.equals(phoneNumber)) {
            Log.w(TAG, "initializePhoneNumber: Phone number mismatch or phone number is missing!");
        } else {
            Log.d(TAG, "initializePhoneNumber: Phone number matches.");
        }
        AppConfig.setPhoneNumber(phoneNumber);
    }

    private void initializeUIComponents() {
        Log.d(TAG, "initializeUIComponents: Initializing UI components.");
        fabAdd = findViewById(R.id.fab_add);
        fabAdd.setOnClickListener(v -> {
            Log.d(TAG, "initializeUIComponents: Add button clicked, launching AddConversationActivity.");
            startActivity(new Intent(MainActivity.this, AddConversationActivity.class));
        });
    }

    private void setupActionBar() {
        Log.d(TAG, "setupActionBar: Setting up custom action bar.");
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        View customView = LayoutInflater.from(this).inflate(R.layout.action_bar_main_activity, null);
        TextView titleText = customView.findViewById(R.id.actionbar_title);
        titleText.setText("Chats");
        getSupportActionBar().setCustomView(customView);

        ImageView settingsButton = findViewById(R.id.actionbar_settings);
        settingsButton.setVisibility(View.VISIBLE);  // Make the settings button visible
        settingsButton.setOnClickListener(v -> {
            Log.d(TAG, "setupActionBar: Settings button clicked, launching SettingsActivity.");
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        });
    }

    private void setupRecyclerView() {
        Log.d(TAG, "setupRecyclerView: Setting up RecyclerView.");
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
            Log.d(TAG, "SmsObserver: Detected change in SMS data, checking for conversation updates.");
            checkForConversationUpdates(hasChanges -> {
                if (hasChanges) {
                    updateUI(cachedConversationMap);
                }
            });
        }
    }

    private void registerFirebaseObservers() {
        Log.d(TAG, "registerFirebaseObservers: Registering Firebase observers.");

        String currentPhoneNumber = AppConfig.checkAndAddCCToNumber(AppConfig.getPhoneNumber());

        // Listening to changes in conversations involving the current user
        firestore.collection("conversations")
                .whereArrayContains("participants", currentPhoneNumber)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.e(TAG, "registerFirebaseObservers: Listen failed.", e);
                        return;
                    }

                    if (snapshots != null && !snapshots.isEmpty()) {
                        List<String> otherParticipants = new ArrayList<>();

                        for (DocumentSnapshot document : snapshots.getDocuments()) {
                            List<String> participants = (List<String>) document.get("participants");
                            if (participants != null) {
                                for (String participant : participants) {
                                    // Add all participants except the current user
                                    if (!participant.equals(currentPhoneNumber)) {
                                        otherParticipants.add(participant);
                                    }
                                }
                            }
                        }

                        // Register listeners for other participants' profile changes
                        registerParticipantProfileListeners(otherParticipants);

                        // Check for conversation updates
                        checkForConversationUpdates(hasChanges -> {
                            if (hasChanges) {
                                updateUI(cachedConversationMap);
                            }
                        }); //TODO change this so it only add/refreshes/deletes for specific conversation.
                    }
                });
    }

    private void registerParticipantProfileListeners(List<String> participantPhoneNumbers) {
        for (String participantPhoneNumber : participantPhoneNumbers) {
            firestore.collection("users")
                    .whereEqualTo("phoneNumber", participantPhoneNumber)
                    .addSnapshotListener((snapshots, e) -> {
                        if (e != null) {
                            Log.e(TAG, "registerFirebaseObservers: Participant profile listen failed.", e);
                            return;
                        }

                        if (snapshots != null && !snapshots.isEmpty()) {
                            Log.d(TAG, "registerFirebaseObservers: Firebase profile changes detected for participant.");

                            for (DocumentSnapshot document : snapshots.getDocuments()) {
                                String phoneNumber = document.getString("phoneNumber");
                                String updatedProfileImageUrl = document.getString("profileImageUrl");
                                String updatedUsername = document.getString("username");

                                // Find the contact in the cachedConversationMap
                                Contact contactToUpdate = findContactByPhoneNumber(phoneNumber);


                                if (contactToUpdate != null) {
                                    boolean dataChanged = false;

                                    // Update the contact's information if necessary
                                    if (updatedProfileImageUrl != null && !updatedProfileImageUrl.equals(contactToUpdate.getProfileImage())) {
                                        contactToUpdate.setProfileImage(updatedProfileImageUrl);
                                        dataChanged = true;
                                    }
                                    if (updatedUsername != null && !updatedUsername.equals(contactToUpdate.getName())) {
                                        contactToUpdate.setName(updatedUsername);
                                        dataChanged = true;
                                    }

                                    if (dataChanged) {
                                        updateCache(contactToUpdate);
                                        // Update the RecyclerView item
                                        runOnUiThread(() -> refreshContactInView(contactToUpdate));
                                    }
                                }
                            }
                        }
                    });
        }
    }

    private void unregisterFirebaseObservers() {
        //For Firestore, the listeners are cleaned up automatically if not stored.
    }
    private void registerObservers() {
        Log.d(TAG, "registerObservers: Registering all observers.");
        registerSmsObserver();
        registerFirebaseObservers();
    }
    private void unregisterObservers() {
        Log.d(TAG, "unregisterObservers: Unregistering all observers.");
        unregisterSmsObserver();
        unregisterFirebaseObservers();
    }
    private void registerSmsObserver() {
        Log.d(TAG, "registerSmsObserver: Registering SMS observer.");
        if (smsObserver == null) {
            smsObserver = new SmsObserver(new Handler(Looper.getMainLooper()));
            getContentResolver().registerContentObserver(Telephony.Sms.CONTENT_URI, true, smsObserver);
        }
    }

    private void unregisterSmsObserver() {
        Log.d(TAG, "unregisterSmsObserver: Unregistering SMS observer.");
        if (smsObserver != null) {
            getContentResolver().unregisterContentObserver(smsObserver);
            smsObserver = null;
        }
    }

    private boolean hasNecessaryPermissions() {
        Log.d(TAG, "hasNecessaryPermissions: Checking if necessary permissions are granted.");
        return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissionsAndExit() {
        Log.d(TAG, "requestPermissionsAndExit: Requesting necessary permissions and exiting.");
        Intent intent = new Intent(this, PermissionRequestActivity.class);
        startActivity(intent);
        finish();
    }

    private boolean isPhoneNumberValid() {
        if (phoneNumber == null) {
            Log.e(TAG, "isPhoneNumberValid: Phone number could not be retrieved, finishing activity.");
            finish();
            return false;
        }
        return true;
    }

    private boolean isUserSignedIn() {
        Log.d(TAG, "isUserSignedIn: Checking if user is signed in.");
        return FirebaseAuth.getInstance().getCurrentUser() != null;
    }

    private void redirectToRegistration() {
        Log.d(TAG, "redirectToRegistration: Redirecting to RegistrationActivity.");
        Intent intent = new Intent(this, RegistrationActivity.class);
        startActivity(intent);
        finish();
    }

    private void initiateConversationList(ConversationCallback callback) {
        executorService.execute(() -> {
            Map<Long, Contact> conversationMap = fetchInboxConversations();

            // Pass the conversationMap to fetchFirebaseConversations
            fetchFirebaseConversations(conversationMap, updatedConversationMap -> {
                cachedConversationMap = updatedConversationMap;  // Cache the updated conversation map

                // Invoke the callback with the updated conversation map once done
                if (callback != null) {
                    runOnUiThread(() -> callback.onComplete(updatedConversationMap));
                }
            });
        });
    }

    public interface ConversationCallback {
        void onComplete(Map<Long, Contact> conversationMap);
    }

    private void checkForConversationUpdates(ConversationUpdateCallback callback) {
        executorService.execute(() -> {
            Map<Long, Contact> newConversationMap = fetchInboxConversations();

            fetchFirebaseConversations(newConversationMap, updatedNewConversationMap -> {
                // This code will run only after fetchFirebaseConversations completes

                // Compare new conversation list with cached list
                boolean hasChanges = !updatedNewConversationMap.equals(cachedConversationMap);

                if (hasChanges) {
                    Log.d(TAG, "checkForConversationUpdates: Changes detected, updating cache.");
                    cachedConversationMap = updatedNewConversationMap; // Update cache
                } else {
                    Log.d(TAG, "checkForConversationUpdates: No changes detected.");
                }

                // Invoke the callback on the main thread
                runOnUiThread(() -> callback.onResult(hasChanges));
            });
        });
    }

    public interface ConversationUpdateCallback {
        void onResult(boolean hasChanges);
    }

    private Map<Long, Contact> fetchInboxConversations() {
        Log.d(TAG, "fetchInboxConversations: Fetching SMS conversations");
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

                    // Avoid duplicate entries by checking if thread ID or address already exists in the map
                    if (conversationMap.containsKey(threadId)) {
                        continue;
                    }

                    String name = getContactName(address);

                    Contact contact = new Contact(name, "???", null, date, type, threadId, address, false);

                    fetchAndAddSmsSnippet(contact);

                    conversationMap.put(threadId, contact);
                    Log.d(TAG, "fetchInboxConversations: Added SMS contact: " + contact.getAddress());
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "fetchInboxConversations: Error fetching SMS conversations: " + e.getMessage(), e);
        }

        return conversationMap;
    }

    private void fetchFirebaseConversations(Map<Long, Contact> conversationMap, FetchConversationsCallback callback) {
        Log.d(TAG, "fetchFirebaseConversations: Fetching Firebase conversations.");
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

                                Log.d(TAG, "fetchFirebaseConversations: Processing participant: " + normalizedParticipant);

                                String name = getContactName(normalizedParticipant);

                                Contact matchingContact = conversationMap.values().stream()
                                        .filter(contact -> contact.getAddress().equals(normalizedParticipant))
                                        .findFirst().orElse(null);

                                int type = lastSenderId.equals(currentPhoneNumber)
                                        ? Telephony.TextBasedSmsColumns.MESSAGE_TYPE_SENT
                                        : Telephony.TextBasedSmsColumns.MESSAGE_TYPE_INBOX;

                                if (matchingContact != null) {
                                    Log.d(TAG, "fetchFirebaseConversations: Found matching SMS contact for " + normalizedParticipant);
                                    if (timestamp != null && timestamp > matchingContact.getTimestamp()) {
                                        String formattedSnippet = formatSnippet(lastMessage, type);

                                        matchingContact.setSnippet(formattedSnippet);
                                        matchingContact.setTimestamp(timestamp);
                                        Log.d(TAG, "fetchFirebaseConversations: Updated timestamp and snippet for " + normalizedParticipant);
                                    }
                                    matchingContact.setRegistered(true);
                                    fetchProfileImageAndUsernameFromFirebase(matchingContact);
                                } else {
                                    Log.d(TAG, "fetchFirebaseConversations: No matching SMS contact found for " + normalizedParticipant + ", creating new contact.");
                                    String formattedSnippet = formatSnippet(lastMessage, type);

                                    Contact contact = new Contact(name, formattedSnippet, null, timestamp != null ? timestamp : 0, -1, -1, normalizedParticipant, true);
                                    fetchProfileImageAndUsernameFromFirebase(contact);
                                    conversationMap.put(contact.getThreadId(), contact);
                                }
                            }
                        }
                        // Notify that fetching is complete
                        callback.onComplete(conversationMap);
                    } else {
                        Log.e(TAG, "fetchFirebaseConversations: Failed to fetch Firebase conversations.");
                        callback.onComplete(conversationMap); // Proceed even if Firebase fetch fails
                    }
                });
    }

    public interface FetchConversationsCallback {
        void onComplete(Map<Long, Contact> conversationMap);
    }

    private void updateUI(Map<Long, Contact> conversationMap) {
        Log.d(TAG, "updateUI: Updating UI with conversation data.");
        List<Contact> sortedContacts = new ArrayList<>(conversationMap.values());
        sortedContacts.sort((c1, c2) -> Long.compare(c2.getTimestamp(), c1.getTimestamp()));

        runOnUiThread(() -> {
            ContactsAdapter adapter = (ContactsAdapter) contactsRecyclerView.getAdapter();
            if (adapter != null) {
                Contact[] contactsArray = sortedContacts.toArray(new Contact[0]);
                adapter.setContacts(contactsArray);  // This will use DiffUtil to update the RecyclerView efficiently
                Log.d(TAG, "updateUI: Contacts updated in RecyclerView.");
            } else {
                Log.w(TAG, "updateUI: ContactsAdapter is null, skipping UI update.");
            }
        });
    }

    private void fetchAndAddSmsSnippet(Contact contact) {
        Log.d(TAG, "fetchAndAddSmsSnippet: Fetching SMS snippet for contact: " + contact.getAddress());
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
                Log.d(TAG, "fetchAndAddSmsSnippet: Snippet fetched and added for contact: " + contact.getAddress());
            } else {
                Log.w(TAG, "fetchAndAddSmsSnippet: No SMS data found for contact: " + contact.getAddress());
            }
        } catch (Exception e) {
            Log.e(TAG, "fetchAndAddSmsSnippet: Error fetching SMS snippet: " + e.getMessage(), e);
        }
    }

    private void fetchProfileImageAndUsernameFromFirebase(Contact contact) {
        String phoneNumber = AppConfig.checkAndAddCCToNumber(contact.getAddress());
        Log.d(TAG, "fetchProfileImageAndUsernameFromFirebase: Fetching profile image and username for " + phoneNumber);

        // Check if data is already cached
        if (firebaseCache.containsKey(phoneNumber)) {
            Log.d(TAG, "fetchProfileImageAndUsernameFromFirebase: Data for " + phoneNumber + " is cached, using cached data.");
            Contact cachedContact = firebaseCache.get(phoneNumber);
            if (cachedContact != null) {
                if (cachedContact.getProfileImage() != null) {
                    contact.setProfileImage(cachedContact.getProfileImage());
                }
                if (cachedContact.getName() != null) {
                    contact.setName(cachedContact.getName());
                }
            }
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
                            Log.d(TAG, "fetchProfileImageAndUsernameFromFirebase: Updated profile image for " + phoneNumber);
                        }
                        if (profileUsername != null && !profileUsername.isEmpty()) {
                            contact.setName(profileUsername);
                            dataChanged = true;
                            Log.d(TAG, "fetchProfileImageAndUsernameFromFirebase: Updated username for " + phoneNumber);
                        }

                        if (dataChanged) {
                            firebaseCache.put(phoneNumber, contact); // Cache the fetched data
                            Log.d(TAG, "fetchProfileImageAndUsernameFromFirebase: Cached data for " + phoneNumber);
                            runOnUiThread(() -> {
                                ContactsAdapter adapter = (ContactsAdapter) contactsRecyclerView.getAdapter();
                                if (adapter != null) {
                                    int position = findContactPosition(contact);
                                    if (position != RecyclerView.NO_POSITION) {
                                        adapter.notifyItemChanged(position);
                                        Log.d(TAG, "fetchProfileImageAndUsernameFromFirebase: Updated contact in RecyclerView for " + phoneNumber);
                                    }
                                }
                            });
                        }
                    } else {
                        Log.w(TAG, "fetchProfileImageAndUsernameFromFirebase: No user data found for " + phoneNumber);
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "fetchProfileImageAndUsernameFromFirebase: Failed to fetch profile info for " + phoneNumber + ": " + e.getMessage(), e));
    }

    private int findContactPosition(Contact contact) {
        Log.d(TAG, "findContactPosition: Finding position for contact: " + contact.getAddress());
        // Use the cached map for quick position lookup
        if (contactPositionMap.containsKey(contact.getAddress())) {
            return contactPositionMap.get(contact.getAddress());
        }

        ContactsAdapter adapter = (ContactsAdapter) contactsRecyclerView.getAdapter();
        if (adapter != null) {
            for (int i = 0; i < adapter.getItemCount(); i++) {
                if (adapter.getContacts()[i].getAddress().equals(contact.getAddress())) {
                    contactPositionMap.put(contact.getAddress(), i); // Cache the position
                    Log.d(TAG, "findContactPosition: Cached position for contact: " + contact.getAddress());
                    return i;
                }
            }
        }
        Log.w(TAG, "findContactPosition: No position found for contact: " + contact.getAddress());
        return RecyclerView.NO_POSITION;
    }
    // Method to find a contact in cachedConversationMap by phone number
    private Contact findContactByPhoneNumber(String phoneNumber) {
        if(cachedConversationMap != null){
            return cachedConversationMap.values().stream()
                    .filter(contact_ -> contact_.getAddress().equals(phoneNumber))
                    .findFirst().orElse(null);
        }   else return null;

    }

    // Method to refresh a specific contact in the RecyclerView
    private void refreshContactInView(Contact contact) {
        int position = findContactPosition(contact);
        if (position != RecyclerView.NO_POSITION) {
            ContactsAdapter adapter = (ContactsAdapter) contactsRecyclerView.getAdapter();
            if (adapter != null) {
                adapter.notifyItemChanged(position);
                Log.d(TAG, "refreshContactInView: Contact updated in RecyclerView at position " + position);
            }
        }
    }
    private String formatSnippet(String snippet, int type) {
        Log.d(TAG, "formatSnippet: Formatting snippet. Type: " + type + ", Snippet: " + snippet);
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
        Log.d(TAG, "getContactName: Fetching contact name for phone number: " + phoneNumber);

        // Check if the phoneNumber is alphanumeric
        if (phoneNumber.matches(".*[^\\d+].*")) {
            Log.d(TAG, "getContactName: The input contains non-digit characters, using it as the contact name.");
            return phoneNumber; // Return the input as the name if it is alphanumeric
        }

        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));
        String[] projection = {ContactsContract.PhoneLookup.DISPLAY_NAME};
        String contactName = null;

        try (Cursor cursor = getContentResolver().query(uri, projection, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                contactName = cursor.getString(0);
                Log.d(TAG, "getContactName: Found contact name: " + contactName + " for phone number: " + phoneNumber);
            } else {
                Log.w(TAG, "getContactName: Contact name for " + phoneNumber + " is not found");
            }
        } catch (Exception e) {
            Log.e(TAG, "getContactName: Error fetching contact name for " + phoneNumber + ": " + e.getMessage(), e);
        }

        return contactName != null ? contactName : phoneNumber;
    }

    private String getDevicePhoneNumber() {
        Log.d(TAG, "getDevicePhoneNumber: Fetching device phone number.");
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_PHONE_STATE}, 1);
            return null;
        }
        return telephonyManager.getLine1Number();
    }

    private void updateCache(Contact updatedContact) {
        if (cachedConversationMap != null && updatedContact != null) {
            for (Map.Entry<Long, Contact> entry : cachedConversationMap.entrySet()) {
                if (entry.getValue().getAddress().equals(updatedContact.getAddress())) {
                    entry.setValue(updatedContact); // Update the contact in the cache
                    break;
                }
            }
        }
        if (firebaseCache != null && updatedContact != null) {
            firebaseCache.put(updatedContact.getAddress(), updatedContact);
        }
    }
}

