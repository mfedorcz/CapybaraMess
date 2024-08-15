package com.example.capybaramess;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MessagesAdapter extends RecyclerView.Adapter<MessagesAdapter.ViewHolder> {
    private List<ChatMessage> mMessages;
    private Context mContext;
    private int selectedPosition = RecyclerView.NO_POSITION;  // Track the selected position

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView messageTextView;
        public TextView additionalInfoTextView;

        public ViewHolder(View v) {
            super(v);
            messageTextView = v.findViewById(R.id.message_text);
            additionalInfoTextView = v.findViewById(R.id.additional_info_text);
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
        holder.messageTextView.setText(message.getContent());

        // Format the timestamp to a readable date and time
        String formattedTimestamp = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(new Date(message.getTimestamp()));
        String platformText = message.getPlatform() == ChatMessage.MessagePlatform.SMS ? "SMS" : "OTT";
        String additionalInfoText = formattedTimestamp + " • " + platformText;
        holder.additionalInfoTextView.setText(additionalInfoText);

        ConstraintLayout.LayoutParams messageParams = (ConstraintLayout.LayoutParams) holder.messageTextView.getLayoutParams();
        ConstraintLayout.LayoutParams infoParams = (ConstraintLayout.LayoutParams) holder.additionalInfoTextView.getLayoutParams();

        // Set the background, text color, and alignment based on the message type
        if (message.getType() == ChatMessage.MessageType.OUTGOING) {
            holder.messageTextView.setBackgroundResource(R.drawable.outgoing_message_bg);
            holder.messageTextView.setTextColor(ContextCompat.getColor(mContext, R.color.colorPrimary));
            messageParams.horizontalBias = 1.0f; // Align to the right
            infoParams.horizontalBias = 1.0f; // Align additional info to the right
            infoParams.startToStart = ConstraintLayout.LayoutParams.UNSET;
            infoParams.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID;
        } else {
            holder.messageTextView.setBackgroundResource(R.drawable.incoming_message_bg);
            holder.messageTextView.setTextColor(ContextCompat.getColor(mContext, R.color.colorOnSecondary));
            messageParams.horizontalBias = 0.0f; // Align to the left
            infoParams.horizontalBias = 0.0f; // Align additional info to the left
            infoParams.endToEnd = ConstraintLayout.LayoutParams.UNSET;
            infoParams.startToStart = ConstraintLayout.LayoutParams.PARENT_ID;
        }
        holder.messageTextView.setLayoutParams(messageParams);
        holder.additionalInfoTextView.setLayoutParams(infoParams);

        // Toggle visibility based on the selected position
        holder.additionalInfoTextView.setVisibility(position == selectedPosition ? View.VISIBLE : View.GONE);

        // Set click listener to show the timestamp for the clicked message and hide others
        holder.messageTextView.setOnClickListener(v -> {
            int previousPosition = selectedPosition;
            selectedPosition = holder.getBindingAdapterPosition();

            // Notify changes for the current and previously selected item
            notifyItemChanged(previousPosition);
            notifyItemChanged(selectedPosition);
        });
    }


    @Override
    public int getItemCount() {
        return mMessages.size();
    }

    // Method to get the current selected position
    public int getSelectedPosition() {
        return selectedPosition;
    }

    // Method to clear the selection and hide the timestamp
    public void clearSelection() {
        int previousPosition = selectedPosition;
        selectedPosition = RecyclerView.NO_POSITION;
        notifyItemChanged(previousPosition);  // Notify to hide the previous timestamp
    }
}
