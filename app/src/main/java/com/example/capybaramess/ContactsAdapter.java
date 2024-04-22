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

public class ContactsAdapter extends RecyclerView.Adapter<ContactsAdapter.ViewHolder> {

    private Contact[] mContacts;
    private Context mContext;  // To hold the context for starting a new activity
    private int[] defaultImages = new int[]{R.drawable.default_profile_imgmdpi1, R.drawable.default_profile_imgmdpi2, R.drawable.default_profile_imgmdpi3};

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView textView;
        public ImageView imageView;
        public ViewHolder(View v) {
            super(v);
            textView = v.findViewById(R.id.contact_name);
            imageView = v.findViewById(R.id.contact_image);
        }
    }

    public ContactsAdapter(Context context, Contact[] contacts) {
        mContext = context;
        mContacts = contacts;
    }

    @Override
    public ContactsAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // Correct the inflated layout to contact_item.xml
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.contact_item, parent, false);
        ViewHolder vh = new ViewHolder(v);
        v.setOnClickListener(view -> {
            int position = vh.getBindingAdapterPosition();
            if (position != RecyclerView.NO_POSITION) {
                Contact contact = mContacts[position];
                Intent intent = new Intent(mContext, ConversationActivity.class);
                intent.putExtra("contactName", contact.getName());
                mContext.startActivity(intent);
            }
        });
        return vh;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Contact contact = mContacts[position];
        holder.textView.setText(contact.getName());

        // Set up RequestOptions to use with Glide
        RequestOptions options = new RequestOptions()
                .circleCrop() // This applies a circle crop to the image, making it circular
                .placeholder(defaultImages[position % defaultImages.length])  // Use your default images as placeholders
                .error(defaultImages[position % defaultImages.length]);  // Use your default images in case of an error

        // Check if the contact has a profile image; if so, load it, otherwise load the default
        if (contact.getProfileImage() != 0) {
            // If the profile image is specified, load it using Glide
            Glide.with(mContext)
                    .load(contact.getProfileImage())
                    .apply(options)
                    .into(holder.imageView);
        } else {
            // If no profile image is specified, load a default image using Glide
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