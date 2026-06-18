package com.garu.app.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "messages")
public class Message {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private String text;
    private boolean isUser;     // true = user, false = Garu
    private long timestamp;

    public Message(String text, boolean isUser) {
        this.text      = text;
        this.isUser    = isUser;
        this.timestamp = System.currentTimeMillis();
    }

    // ── Getters / Setters ────────────────────────────────────────────────────
    public int    getId()        { return id;        }
    public void   setId(int id)  { this.id = id;     }
    public String getText()      { return text;      }
    public boolean isUser()      { return isUser;    }
    public long   getTimestamp() { return timestamp; }
    public void   setTimestamp(long t) { timestamp = t; }
}
