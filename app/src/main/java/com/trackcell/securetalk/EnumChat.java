package com.trackcell.securetalk;

import android.content.Context;
import android.graphics.Bitmap;

public class EnumChat
{
    public boolean isMe;
    public boolean Error;
    public boolean isPhoto = false;

    public String Timestamp;
    public String ID;
    public String Title;
    public String Content;
    public Bitmap Photo;

    public EnumChat(Context context, boolean is_me, boolean error, String timestamp, String id, String title)
    {
        isMe = is_me;
        Error = error;
        ID = id;
        Title = title;
        Content = "salut";
        Timestamp = timestamp;
    }

    public EnumChat putPhoto(Bitmap bitmap)
    {
        isPhoto = true;
        Photo = bitmap;
        return this;
    }
}