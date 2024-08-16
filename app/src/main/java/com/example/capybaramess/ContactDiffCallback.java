package com.example.capybaramess;

import androidx.recyclerview.widget.DiffUtil;

import java.util.List;

public class ContactDiffCallback extends DiffUtil.Callback {

    private final List<Contact> oldList;
    private final List<Contact> newList;

    public ContactDiffCallback(List<Contact> oldList, List<Contact> newList) {
        this.oldList = oldList;
        this.newList = newList;
    }

    @Override
    public int getOldListSize() {
        return oldList.size();
    }

    @Override
    public int getNewListSize() {
        return newList.size();
    }

    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        // Assuming Contact has a unique identifier, such as threadId
        return oldList.get(oldItemPosition).getThreadId() == newList.get(newItemPosition).getThreadId();
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        // Compare all the fields in the Contact to see if they are the same
        return oldList.get(oldItemPosition).equals(newList.get(newItemPosition));
    }
}
