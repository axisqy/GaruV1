package com.garu.app.model;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface MessageDao {

    @Insert
    long insert(Message message);

    @Query("SELECT * FROM messages ORDER BY timestamp ASC")
    LiveData<List<Message>> getAllMessages();

    @Query("SELECT * FROM messages ORDER BY timestamp ASC")
    List<Message> getAllMessagesSync();

    @Query("DELETE FROM messages")
    void deleteAll();
}
