package com.example.capybaramess;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.CursorJoiner;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Telephony;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.Manifest;
import android.widget.Toast;

import androidx.annotation.ContentView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ConversationActivity extends AppCompatActivity {
    private static final int REQUEST_SEND_SMS = 123;
    private EditText messageEditText;
    private Button sendButton;
    private RecyclerView messagesRecyclerView;
    private BroadcastReceiver networkChangeReceiver;
    private List<ChatMessage> chatMessages;
    private MessagesAdapter adapter;
    private BroadcastReceiver smsReceiver;
    private FirebaseFirestore firestore;
    private Contact contact;
    private boolean isChatVisible = false;
    private ExecutorService executorService;
    private String phoneNumber; // Store the device's phone number
    private final int[] defaultImages = new int[]{
            R.drawable.default_profile_imgmdpi1,
            R.drawable.default_profile_imgmdpi2,
            R.drawable.default_profile_imgmdpi3
    };

    private boolean isOTTMode = false; // Default to SMS mode
    private SwitchCompat modeSwitch;
    private TextView modeText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.conversation_activity);

        // Initialize the chatMessages list
        chatMessages = new ArrayList<>();

        // Initialize Firebase Firestore
        firestore = FirebaseFirestore.getInstance();

        // Initialize ExecutorService for background tasks
        executorService = Executors.newSingleThreadExecutor();

        // Retrieve the device's phone number
        phoneNumber = AppConfig.getPhoneNumber();

        if (phoneNumber == null) {
            Log.e("ConversationActivity", "Phone number could not be retrieved, finishing activity.");
            finish();
            return;
        }

        isChatVisible = true;

        // Get contact information
        contact = getIntent().getParcelableExtra("contact");

        if (contact == null) {
            Log.e("ConversationActivity", "Contact is null, finishing activity.");
            finish();
            return;
        }


        // Setting up the custom ActionBar
        setupActionBar();

        // Setting up Chat
        setupChat();
        // Register the network change receiver
        networkChangeReceiver = new NetworkChangeReceiver(this, modeSwitch, modeText, contact);
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(networkChangeReceiver, filter);
        // Initialize BroadcastReceiver
        initializeSmsReceiver();
        registerReceiver(smsReceiver, new IntentFilter("android.provider.Telephony.SMS_RECEIVED"));

    }

    @Override
    protected void onResume() {
        super.onResume();
        isChatVisible = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        isChatVisible = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (networkChangeReceiver != null) {
            unregisterReceiver(networkChangeReceiver);
        }
        unregisterReceiver(smsReceiver);
        executorService.shutdown();
    }
    private void AddConversationMessageInit(String message){
        ChatMessage initMessage = new ChatMessage(
                phoneNumber,
                contact.getAddress(),
                message,
                System.currentTimeMillis(),
                ChatMessage.MessageType.OUTGOING,  //MessageType default set to OUTGOING
                ChatMessage.MessagePlatform.SMS, //MessagePlatform default to SMS
                ChatMessage.DeliveryStatus.SENT
        );
        // Check permission and send the message
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, REQUEST_SEND_SMS);
        } else {
            sendMessage(initMessage);
            updateUIWithNewMessage(initMessage);
        }
    }
    private void setupActionBar() {
        // Enable custom ActionBar
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        // Inflate the custom view
        View customView = LayoutInflater.from(this).inflate(R.layout.action_bar_act_conversation, null);

        // Set the custom view to the ActionBar
        getSupportActionBar().setCustomView(customView);

        // Access views from the custom view and set data
        TextView titleText = customView.findViewById(R.id.actionbar_title);
        titleText.setText(contact.getName());
        titleText.setPadding(40,0,0,0);

        ImageView backButton = customView.findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        ImageView profileImageView = customView.findViewById(R.id.profileImageView);
        int position = getIntent().getIntExtra("position", 0);
        RequestOptions options = new RequestOptions()
                .circleCrop()
                .placeholder(defaultImages[position % defaultImages.length])
                .error(defaultImages[position % defaultImages.length]);

        if (contact.isRegistered() && contact.getProfileImage() != null && !contact.getProfileImage().isEmpty()) {
            Glide.with(this)
                    .load(contact.getProfileImage())
                    .apply(options)
                    .into(profileImageView);
        } else {
            Glide.with(this)
                    .load(defaultImages[position % defaultImages.length])
                    .apply(options)
                    .into(profileImageView);
        }

        //Setup slider
        setupSlider();
    }
    private void setupSlider() {
        modeSwitch = findViewById(R.id.switch_mode);
        modeText = findViewById(R.id.text_mode);
        modeText.setText("SMS");

        // Determine if OTT mode can be used
        boolean canUseOTT = isNetworkAvailable() && contact.isRegistered();

        if (!canUseOTT) {
            // Intercept touch events to prevent the switch from toggling
            modeSwitch.setOnTouchListener((v, event) -> {
                if (event.getAction() == MotionEvent.ACTION_UP) { // Detect the release of the touch
                    // Show the toast message when the switch is touched
                    Toast.makeText(this, !isNetworkAvailable() ? "Network is not available. SMS only" : "Contact is not registered in the server. SMS only.", Toast.LENGTH_SHORT).show();
                }
                return true; // Consume the touch event, preventing the switch from changing state
            });
        } else {
            // Allow the switch to change state and update the UI accordingly
            modeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                isOTTMode = isChecked;
                if (isChecked) {
                    modeText.setText("OTT");
                    modeText.setTypeface(null, Typeface.BOLD);
                } else {
                    modeText.setText("SMS");
                    modeText.setTypeface(null, Typeface.NORMAL);
                }
            });
            modeSwitch.setChecked(true);
        }
    }
    private void initializeSmsReceiver() {
        smsReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equals(intent.getAction())) {
                    Bundle bundle = intent.getExtras();
                    if (bundle != null) {
                        Object[] pdus = (Object[]) bundle.get("pdus");
                        String format = bundle.getString("format"); // Get the SMS message format

                        if (pdus != null) {
                            for (Object pdu : pdus) {
                                SmsMessage smsMessage;
                                smsMessage = SmsMessage.createFromPdu((byte[]) pdu, format);
                                String sender = smsMessage.getDisplayOriginatingAddress();
                                String messageBody = smsMessage.getMessageBody();
                                long timestamp = smsMessage.getTimestampMillis();

                                // Check if the message is from the same thread
                                if (sender.equals(contact.getAddress())) {
                                    ChatMessage newMessage = new ChatMessage(
                                            sender, phoneNumber, messageBody, timestamp, ChatMessage.MessageType.INCOMING, ChatMessage.MessagePlatform.SMS, ChatMessage.DeliveryStatus.READ
                                    );
                                    updateUIWithNewMessage(newMessage);
                                }
                            }
                        }
                    }
                }
            }
        };
    }
    private void setupChat(){
        messagesRecyclerView = findViewById(R.id.messagesRecyclerView);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);  // Start the layout from the bottom
        messagesRecyclerView.setLayoutManager(layoutManager);
        // Initialize the adapter with the chatMessages list
        adapter = new MessagesAdapter(this, chatMessages);
        messagesRecyclerView.setAdapter(adapter);

        setupRealTimeUpdates();

        messageEditText = findViewById(R.id.messageEditText);
        sendButton = findViewById(R.id.sendButton);

        sendButton.setOnClickListener(v -> {
            String messageText = messageEditText.getText().toString().trim();
            if (!messageText.isEmpty()) {
                // Create a new ChatMessage object
                ChatMessage newMessage = new ChatMessage(
                        phoneNumber,  // Sender ID is the device's phone number
                        contact.getAddress(),  // Address of the recipient
                        messageText,
                        System.currentTimeMillis(),
                        ChatMessage.MessageType.OUTGOING,  //MessageType default set to OUTGOING
                        ChatMessage.MessagePlatform.SMS, //MessagePlatform default to SMS
                        ChatMessage.DeliveryStatus.SENT
                );
                // Clear the input field
                messageEditText.setText("");
                // Check permission and send the message
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, REQUEST_SEND_SMS);
                } else {
                    sendMessage(newMessage);
                    // Adding message to the list and notify the adapter
                    updateUIWithNewMessage(newMessage);
                }
            }
        });
        // Retrieving the threadId from the intent
        long threadId = contact.getThreadId();
        // Fetching and showing messages if a valid threadId is provided
        if (threadId != -1) {
            chatMessages.addAll(fetchMessages(threadId)); // Fetching messages for the given threadId
            // Fetch Firebase messages and merge them with SMS messages
            fetchMessagesFromFirebase();
            adapter.notifyDataSetChanged(); // Notify adapter that data has changed
        } else {
            fetchMessagesFromFirebase();
            adapter.notifyDataSetChanged();
            Log.e("ConversationActivity", "\"Only OTT Chat\" threadId passed to ConversationActivity");
        }
        // Checking if chat was from AddConversationActivity and proceeding accordingly
        Intent intent = getIntent();
        if(intent.getBooleanExtra("newConversation", false)){
            AddConversationMessageInit(intent.getStringExtra("message"));
        }
    }
    private void updateUIWithNewMessage(ChatMessage message) {
        runOnUiThread(() -> {
            if (!chatMessages.contains(message)) {  // Check for duplicates
                chatMessages.add(message);
                chatMessages.sort(Comparator.comparing(ChatMessage::getTimestamp));
                adapter.notifyItemInserted(chatMessages.size() - 1);
                messagesRecyclerView.scrollToPosition(chatMessages.size() - 1);
            }
        });
    }

    private void sendMessage(ChatMessage message) {
        if(isOTTMode){
            executorService.execute(() -> sendViaFirebase(message));
        } else {
            sendViaSMS(message);
        }
    }

    private void sendViaFirebase(ChatMessage message) {
        Log.d("Firebase", "Sending Firestore message: " + message.getContent());
        message.setPlatform(ChatMessage.MessagePlatform.OTT);
        String conversationId = AppConfig.getConversationId(
                message.getRecipientId().length() == 9 ? "+48" + message.getRecipientId() : message.getRecipientId()
        );

        // Reference to the conversation document
        DocumentReference conversationRef = firestore.collection("conversations").document(conversationId);

        // Check if the conversation document exists
        conversationRef.get().addOnSuccessListener(documentSnapshot -> {
            if (!documentSnapshot.exists()) {
                // If the document doesn't exist, create a new conversation and add the message
                addConversationToFirestore(message);
                Log.d("Firebase", "Conversation not present in Firestore. Creating entry.");
            } else {
                // If the document exists, just add the message to the messages subcollection
                addMessageToConversation(conversationRef, message);
            }

            // Update the conversation document with the last message and timestamp
            Map<String, Object> conversationData = new HashMap<>();
            conversationData.put("participants", Arrays.asList(message.getSenderId(), message.getRecipientId()));
            conversationData.put("lastMessage", message.getContent());
            conversationData.put("lastSenderId", AppConfig.getPhoneNumber());
            conversationData.put("lastTimestamp", message.getTimestamp());

            conversationRef.set(conversationData, SetOptions.merge())
                    .addOnSuccessListener(aVoid -> Log.d("Firebase", "Conversation updated successfully."))
                    .addOnFailureListener(e -> Log.e("Firebase", "Failed to update conversation.", e));
        }).addOnFailureListener(e -> {
            Log.e("Firebase", "Failed to check if conversation exists.", e);
            // Handle failure to check for document existence
        });
    }

    private void addConversationToFirestore(ChatMessage message) {
        String conversationId = AppConfig.getConversationId(
                message.getRecipientId().length() == 9 ? "+48" + message.getRecipientId() : message.getRecipientId()
        );

        DocumentReference conversationRef = firestore.collection("conversations").document(conversationId);

        // Data for the main conversation document
        Map<String, Object> conversationData = new HashMap<>();
        conversationData.put("conversationId", conversationId);
        conversationData.put("participants", Arrays.asList(message.getSenderId(), message.getRecipientId()));
        conversationData.put("lastMessage", message.getContent());
        conversationData.put("lastSenderId", AppConfig.getPhoneNumber());
        conversationData.put("lastTimestamp", message.getTimestamp());

        // Set the conversation document in Firestore
        conversationRef.set(conversationData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> Log.d("Firebase", "Conversation document created successfully."))
                .addOnFailureListener(e -> Log.e("Firebase", "Failed to create conversation document.", e));

        // Add the first message to the messages subcollection
        addMessageToConversation(conversationRef, message);
    }

    private void addMessageToConversation(DocumentReference conversationRef, ChatMessage message) {
        // Create a map to represent the message data

        Map<String, Object> messageData = new HashMap<>();
        messageData.put("senderId", message.getSenderId());
        messageData.put("recipientId", message.getRecipientId());
        messageData.put("content", message.getContent());
        messageData.put("timestamp", message.getTimestamp());
        messageData.put("deliveryStatus", message.getDeliveryStatus().name());

        // Add the message to the messages subcollection
        conversationRef.collection("messages")
                .add(messageData)
                .addOnSuccessListener(documentReference -> Log.d("Firebase", "Message sent successfully via Firebase."))
                .addOnFailureListener(e -> {
                    Log.e("Firebase", "Failed to send message via Firebase.", e);
                    // Fallback to SMS if Firebase sending fails
                    modeSwitch.toggle();
                    message.setPlatform(ChatMessage.MessagePlatform.SMS);
                    sendViaSMS(message);
                });
    }


    private void sendViaSMS(ChatMessage message) {
        SmsManager smsManager = SmsManager.getDefault();
        try {
            // Send a text message to the given number
            smsManager.sendTextMessage(message.getRecipientId(), null, message.getContent(), null, null);
            Log.d("SMS", "SMS sent successfully.");
        } catch (Exception e) {
            Log.e("SMS", "SMS failed to send.", e);
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private List<ChatMessage> fetchMessages(long threadId) {
        List<ChatMessage> messages = new ArrayList<>();
        Uri uri = Telephony.Sms.CONTENT_URI;
        String[] projection = {
                Telephony.Sms._ID,
                Telephony.Sms.ADDRESS,
                Telephony.Sms.BODY,
                Telephony.Sms.DATE,
                Telephony.Sms.TYPE
        };
        String selection = Telephony.Sms.THREAD_ID + " = ?";
        String[] selectionArgs = {String.valueOf(threadId)};
        String sortOrder = Telephony.Sms.DATE + " ASC";

        try (Cursor cursor = getContentResolver().query(uri, projection, selection, selectionArgs, sortOrder)) {
            if (cursor != null) {
                int idIdx = cursor.getColumnIndexOrThrow(Telephony.Sms._ID);
                int addressIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS);
                int bodyIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY);
                int dateIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE);
                int typeIdx = cursor.getColumnIndexOrThrow(Telephony.Sms.TYPE);

                while (cursor.moveToNext()) {
                    if (idIdx != -1 && addressIdx != -1 && bodyIdx != -1 && dateIdx != -1 && typeIdx != -1) {
                        String id = cursor.getString(idIdx);
                        String sender = cursor.getString(addressIdx);
                        String content = cursor.getString(bodyIdx);
                        long timestamp = cursor.getLong(dateIdx);
                        int type = cursor.getInt(typeIdx);

                        String receiver = type == Telephony.Sms.MESSAGE_TYPE_SENT ? contact.getAddress() : phoneNumber;
                        messages.add(new ChatMessage(sender, receiver, content, timestamp, type == Telephony.Sms.MESSAGE_TYPE_SENT ? ChatMessage.MessageType.OUTGOING : ChatMessage.MessageType.INCOMING, ChatMessage.MessagePlatform.SMS, ChatMessage.DeliveryStatus.SENT));
                    } else {
                        Log.e("SMS Fetch", "One of the required columns is missing in the SMS database.");
                    }
                }
            }
        } catch (Exception e) {
            Log.e("ConversationActivity", "Error fetching SMS messages", e);
        }
        return messages;
    }
    private void fetchMessagesFromFirebase() {
        String conversationId = AppConfig.getConversationId(contact.getAddress().length() == 9 ? "+48" + contact.getAddress() : contact.getAddress());

        firestore.collection("conversations")
                .document(conversationId)
                .collection("messages")
                .orderBy("timestamp")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                        String senderId = document.getString("senderId");
                        String recipientId = document.getString("recipientId");
                        String content = document.getString("content");
                        long timestamp = document.getLong("timestamp");
                        String status = document.getString("deliveryStatus");

                        ChatMessage.DeliveryStatus deliveryStatus = status != null ? ChatMessage.DeliveryStatus.valueOf(status) : ChatMessage.DeliveryStatus.SENT;
                        ChatMessage.MessageType messageType = senderId.equals(phoneNumber) ? ChatMessage.MessageType.OUTGOING : ChatMessage.MessageType.INCOMING;
                        ChatMessage newMessage = new ChatMessage(senderId, recipientId, content, timestamp, messageType, ChatMessage.MessagePlatform.OTT, deliveryStatus);

                        updateUIWithNewMessage(newMessage);
                    }
                })
                .addOnFailureListener(e -> Log.e("Firebase", "Failed to fetch messages from Firebase", e));
    }

    private void setupRealTimeUpdates() {
        String conversationId = AppConfig.getConversationId(contact.getAddress().length() == 9 ? "+48" + contact.getAddress() : contact.getAddress());

        firestore.collection("conversations")
                .document(conversationId)
                .collection("messages")
                .orderBy("timestamp")
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    if (e != null) {
                        Log.e("Firebase", "Listen failed.", e);
                        return;
                    }

                    if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                        for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                            String senderId = document.getString("senderId");
                            String recipientId = document.getString("recipientId");
                            String content = document.getString("content");
                            long timestamp = document.getLong("timestamp");
                            String status = document.getString("deliveryStatus");

                            ChatMessage.DeliveryStatus deliveryStatus = status != null ? ChatMessage.DeliveryStatus.valueOf(status) : ChatMessage.DeliveryStatus.SENT;
                            ChatMessage.MessageType messageType = senderId.equals(phoneNumber) ? ChatMessage.MessageType.OUTGOING : ChatMessage.MessageType.INCOMING;
                            ChatMessage newMessage = new ChatMessage(senderId, recipientId, content, timestamp, messageType, ChatMessage.MessagePlatform.OTT, deliveryStatus);
                            //Zaktualizuj status wiadomości odbierającego
                            if (isChatVisible && messageType == ChatMessage.MessageType.INCOMING && deliveryStatus == ChatMessage.DeliveryStatus.SENT) {
                                markMessageAsRead(newMessage);
                            }
                            //Zaktualizuj status wiadomości dla wysyłającego
                            if (messageType == ChatMessage.MessageType.OUTGOING && deliveryStatus == ChatMessage.DeliveryStatus.READ) {
                                runOnUiThread(() -> {
                                    for (ChatMessage msg : chatMessages) {
                                        if (msg.equals(newMessage) && msg.getDeliveryStatus() != ChatMessage.DeliveryStatus.READ) {
                                            msg.setDeliveryStatus(ChatMessage.DeliveryStatus.READ);
                                            adapter.notifyDataSetChanged();
                                        }
                                    }
                                });
                            }
                            updateUIWithNewMessage(newMessage);
                        }
                    }
                });
    }
    public class NetworkChangeReceiver extends BroadcastReceiver {

        private final SwitchCompat modeSwitch;
        private final TextView modeText;
        private final Context context;
        private final Contact contact;

        public NetworkChangeReceiver(Context context, SwitchCompat modeSwitch, TextView modeText, Contact contact) {
            this.context = context;
            this.modeSwitch = modeSwitch;
            this.modeText = modeText;
            this.contact = contact;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
                boolean canUseOTT = isNetworkAvailable() && contact.isRegistered();

                if (!canUseOTT) {
                    modeSwitch.setChecked(false);
                    modeSwitch.setOnTouchListener((v, event) -> {
                        if (event.getAction() == MotionEvent.ACTION_UP) {
                            Toast.makeText(context, !isNetworkAvailable() ? "Network is not available. SMS only." : "Contact is not registered. SMS only.", Toast.LENGTH_SHORT).show();
                        }
                        return true;
                    });
                    modeText.setText("SMS");
                    modeText.setTypeface(null, Typeface.NORMAL);
                } else {
                    modeSwitch.setOnTouchListener(null);  // Remove the touch listener so the switch can change state
                    modeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                        isOTTMode = isChecked;
                        if (isChecked) {
                            modeText.setText("OTT");
                            modeText.setTypeface(null, Typeface.BOLD);
                        } else {
                            modeText.setText("SMS");
                            modeText.setTypeface(null, Typeface.NORMAL);
                        }
                    });
                    modeSwitch.setChecked(true);
                }
            }
        }

        private boolean isNetworkAvailable() {
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
    }

    private void markMessageAsRead(ChatMessage message) {
        String conversationId = AppConfig.getConversationId(
                contact.getAddress().length() == 9 ? "+48" + contact.getAddress() : contact.getAddress()
        );

        firestore.collection("conversations")
                .document(conversationId)
                .collection("messages")
                .whereEqualTo("timestamp", message.getTimestamp())
                .whereEqualTo("content", message.getContent())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                            document.getReference().update("deliveryStatus", ChatMessage.DeliveryStatus.READ.name())
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d("Firebase", "Message marked as READ.");
                                        message.setDeliveryStatus(ChatMessage.DeliveryStatus.READ);
                                        adapter.notifyDataSetChanged(); // Aktualizacja UI.
                                    })
                                    .addOnFailureListener(e -> Log.e("Firebase", "Failed to update message status.", e));
                        }
                    } else {
                        Log.e("Firebase", "No matching message found to mark as READ.");
                    }
                })
                .addOnFailureListener(e -> Log.e("Firebase", "Failed to fetch messages to update.", e));
    }

}
