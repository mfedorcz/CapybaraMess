package com.example.capybaramess;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import androidx.recyclerview.widget.DiffUtil;

import java.util.Arrays;

public class ContactsAdapter extends RecyclerView.Adapter<ContactsAdapter.ViewHolder> {

    private Context mContext;
    private Contact[] mContacts;
    private int[] defaultImages = new int[]{
            R.drawable.default_profile_imgmdpi1,
            R.drawable.default_profile_imgmdpi2,
            R.drawable.default_profile_imgmdpi3
    };

    // Constructor
    public ContactsAdapter(Context context, Contact[] contacts) {
        this.mContext = context;
        this.mContacts = contacts;
    }

    // ViewHolder class to hold item views
    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView nameTextView;
        public TextView snippetTextView;
        public ImageView imageView;

        public ViewHolder(View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.contact_name);
            snippetTextView = itemView.findViewById(R.id.snippet_text);
            imageView = itemView.findViewById(R.id.contact_image);
        }
    }

    // Method to set the contacts list
    public void setContacts(Contact[] newContacts) {
        ContactDiffCallback diffCallback = new ContactDiffCallback(Arrays.asList(this.mContacts), Arrays.asList(newContacts));
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffCallback);

        this.mContacts = newContacts;
        diffResult.dispatchUpdatesTo(this);  // Notifies the adapter of the changes
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
                intent.putExtra("contact", contact);
                intent.putExtra("threadID", contact.getThreadId());
                intent.putExtra("position", position);
                mContext.startActivity(intent);
            }
        });
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Contact contact = mContacts[position];
        holder.nameTextView.setText(contact.getName());
        holder.snippetTextView.setText(contact.getSnippet());

        // Load profile image
        loadProfileImage(contact, holder.imageView, position);
    }

    private void loadProfileImage(Contact contact, ImageView imageView, int position) {
        RequestOptions options = new RequestOptions()
                .circleCrop()
                .placeholder(defaultImages[position % defaultImages.length])
                .error(defaultImages[position % defaultImages.length]);

        // If the contact is registered and has a profile image, use it
        if (contact.isRegistered() && contact.getProfileImage() != null && !contact.getProfileImage().isEmpty()) {
            Glide.with(mContext)
                    .load(contact.getProfileImage())
                    .apply(options)
                    .into(imageView);
        } else {
            // Use default image
            Glide.with(mContext)
                    .load(defaultImages[position % defaultImages.length])
                    .apply(options)
                    .into(imageView);
        }
    }

    @Override
    public int getItemCount() {
        return mContacts.length;
    }
    public Contact[] getContacts() {
        return mContacts;
    }
}
