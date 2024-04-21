package com.example.capybaramess;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;

public class ContactsAdapter extends RecyclerView.Adapter<ContactsAdapter.ViewHolder> {

    private String[] mContacts;
    private Context mContext;  // To hold the context for starting a new activity

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView textView;
        public ViewHolder(View v) {
            super(v);
            textView = v.findViewById(android.R.id.text1);
        }
    }

    public ContactsAdapter(Context context, String[] contacts) {
        mContext = context;
        mContacts = contacts;
    }

    @Override
    public ContactsAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
        ViewHolder vh = new ViewHolder(v);
        v.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int position = vh.getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    String contact = mContacts[position];
                    Intent intent = new Intent(mContext, ConversationActivity.class);
                    intent.putExtra("contactName", contact);
                    mContext.startActivity(intent);
                }
            }
        });
        return vh;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.textView.setText(mContacts[position]);
    }

    @Override
    public int getItemCount() {
        return mContacts.length;
    }
}