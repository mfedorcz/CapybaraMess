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
        holder.snippetTextView.setText(contact.getSnippet()); // Set the text for snippet

        RequestOptions options = new RequestOptions()
                .circleCrop()
                .placeholder(defaultImages[position % defaultImages.length])
                .error(defaultImages[position % defaultImages.length]);

        // Check if the contact has a profile image URL
        if (contact.getProfileImage() != null && !contact.getProfileImage().isEmpty()) {
            // Load the profile image from the URL
            Glide.with(mContext)
                    .load(contact.getProfileImage())
                    .apply(options)
                    .into(holder.imageView);
        } else {
            // Load a default image
            Glide.with(mContext)
                    .load(defaultImages[position % defaultImages.length])
                    .apply(options)
                    .into(holder.imageView);
        }
    }

    @Override
    public int getItemCount() {
        return mContacts.length;
    }
}
