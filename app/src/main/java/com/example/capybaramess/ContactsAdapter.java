package com.example.capybaramess;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class ContactsAdapter extends RecyclerView.Adapter<ContactsAdapter.ViewHolder> {

    private Contact[] mContacts;
    private Context mContext;  // Context to start new activities
    private int[] defaultImages = new int[]{
            R.drawable.default_profile_imgmdpi1,
            R.drawable.default_profile_imgmdpi2,
            R.drawable.default_profile_imgmdpi3
    };

    // ViewHolder class to hold item views
    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView nameTextView;    // To display the contact's name or number
        public TextView snippetTextView; // To display the last message snippet
        public ImageView imageView;      // To display the contact's image

        public ViewHolder(View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.contact_name);
            snippetTextView = itemView.findViewById(R.id.snippet_text);
            imageView = itemView.findViewById(R.id.contact_image);
        }
    }

    public void setContacts(List<Contact> contacts) {
        mContacts = contacts.toArray(new Contact[0]);
    }

    public ContactsAdapter(Context context, Contact[] contacts) {
        mContext = context;
        mContacts = contacts;
    }

    @Override
    public ContactsAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.contact_item, parent, false);
        final ViewHolder viewHolder = new ViewHolder(view);
        view.setOnClickListener(v -> {
            int position = viewHolder.getBindingAdapterPosition();
            if (position != RecyclerView.NO_POSITION) {
                Contact contact = mContacts[position];
                Intent intent = new Intent(mContext, ConversationActivity.class);
                intent.putExtra("contact", contact); // Pass the entire Contact object
                intent.putExtra("threadID", contact.getThreadId()); //Ensuring threadID is passed
                mContext.startActivity(intent);
            }
        });
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Contact contact = mContacts[position];
        holder.nameTextView.setText(contact.getName());

        // Initially set the snippet text to an empty string or a loading indicator
        holder.snippetTextView.setText("Loading...");

        RequestOptions options = new RequestOptions()
                .circleCrop()
                .placeholder(defaultImages[position % defaultImages.length])
                .error(defaultImages[position % defaultImages.length]);

        // Load the profile image if available
        if (contact.getProfileImage() != null && !contact.getProfileImage().isEmpty()) {
            Glide.with(mContext)
                    .load(contact.getProfileImage())
                    .apply(options)
                    .into(holder.imageView);
        } else {
            Glide.with(mContext)
                    .load(defaultImages[position % defaultImages.length])
                    .apply(options)
                    .into(holder.imageView);
        }

        // Fetch last OTT chat message from Firebase and update the snippet if isRegistered == true
        if(contact.isRegistered()) {
            fetchLastMessageFromFirebase(contact, holder.snippetTextView);
        }else {
            // Set the SMS snippet only if the Firebase snippet has not been set (fallback)
            if (contact.getLastMessage() == null && contact.getSnippet() != null) {
                holder.snippetTextView.setText(contact.getSnippet());
            }
        }
    }

    private void fetchLastMessageFromFirebase(Contact contact, TextView snippetTextView) {
        String conversationId = AppConfig.getConversationId(contact.getAddress());  // Generate the conversation ID based on phone numbers

        Log.d("FirebaseQuery", "Fetching last message for conversation ID: " + conversationId);

        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        firestore.collection("conversations")
                .document(conversationId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String lastMessage = documentSnapshot.getString("lastMessage");
                        String lastSenderId = documentSnapshot.getString("lastSenderId");
                        if (lastMessage != null) {
                            contact.setLastMessage(lastMessage);
                            String formattedSnippet = lastSenderId.equals(AppConfig.getPhoneNumber()) ? "You: " + lastMessage : lastMessage;
                            contact.setLastMessage(formattedSnippet);
                            snippetTextView.setText(formattedSnippet);
                        } else {
                            // If no last message found in Firebase, fall back to SMS snippet
                            if (contact.getSnippet() != null) {
                                snippetTextView.setText(contact.getSnippet());
                            }
                        }
                    } else {
                        Log.d("FirebaseQuery", "No document found for conversation ID: " + conversationId);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("FirebaseQuery", "Failed to fetch last message for conversation ID: " + conversationId, e);
                });
    }

    @Override
    public int getItemCount() {
        return mContacts.length;
    }
}
