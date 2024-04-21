package com.example.capybaramess;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;

public class MessagesAdapter extends RecyclerView.Adapter<MessagesAdapter.ViewHolder> {

    private String[] mMessages;
    private Context mContext;  // Context to use for interface actions

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView textView;
        public ViewHolder(View v) {
            super(v);
            textView = v.findViewById(android.R.id.text1);
        }
    }

    public MessagesAdapter(Context context, String[] messages) {
        mContext = context;
        mMessages = messages;
    }

    @Override
    public MessagesAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
        ViewHolder vh = new ViewHolder(v);
        v.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int position = vh.getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    // Example action: Display a Toast with the message
                    Toast.makeText(mContext, "Clicked on: " + mMessages[position], Toast.LENGTH_SHORT).show();
                    // You can also start new activities, open dialogs, etc.
                }
            }
        });
        return vh;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.textView.setText(mMessages[position]);
    }

    @Override
    public int getItemCount() {
        return mMessages.length;
    }
}