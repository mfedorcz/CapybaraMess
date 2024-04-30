package com.example.capybaramess;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

//This adapter handles viewing/recycling of chat messages.
public class MessagesAdapter extends RecyclerView.Adapter<MessagesAdapter.ViewHolder> {
    private List<ChatMessage> mMessages;
    private Context mContext;

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView textView;
        public ViewHolder(View v) {
            super(v);
            textView = v.findViewById(R.id.message_text);
        }
    }

    public MessagesAdapter(Context context, List<ChatMessage> messages) {
        mContext = context;
        mMessages = messages;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.message_item, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        ChatMessage message = mMessages.get(position);
        holder.textView.setText(message.getContent());

        ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) holder.textView.getLayoutParams();

        // Set the background and text color based on message type
        if (message.getType() == ChatMessage.MessageType.OUTGOING) {
            holder.textView.setBackgroundResource(R.drawable.outgoing_message_bg);
            holder.textView.setTextColor(ContextCompat.getColor(mContext, R.color.colorPrimary));  // Set text color to white for outgoing messages
            params.horizontalBias = 1.0f; // Align to the right
        } else {
            holder.textView.setBackgroundResource(R.drawable.incoming_message_bg);
            holder.textView.setTextColor(ContextCompat.getColor(mContext, R.color.colorOnSecondary));  // Optionally set a different color for incoming messages
            params.horizontalBias = 0.0f; // Align to the left
        }
        holder.textView.setLayoutParams(params);  // Apply layout parameters
    }

    @Override
    public int getItemCount() {
        return mMessages.size();
    }
}