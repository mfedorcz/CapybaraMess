package com.example.capybaramess;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class MessagesAdapterTest {
    @Mock
    private Context mockContext;

    private MessagesAdapter adapter;
    private List<ChatMessage> chatMessages;

    @Before
    public void setup() {
        chatMessages = new ArrayList<>();
        chatMessages.add(new ChatMessage("1", "user1", "Hello", System.currentTimeMillis(), ChatMessage.MessageType.INCOMING));
        chatMessages.add(new ChatMessage("2", "user2", "Hi there", System.currentTimeMillis(), ChatMessage.MessageType.OUTGOING));
        adapter = new MessagesAdapter(mockContext, chatMessages);
    }

    @Test
    public void itemCountTest() {
        assertEquals("Item count should match the list size", 2, adapter.getItemCount());
    }
}