package com.trackcell.securetalk;

import android.content.Context;
import android.graphics.drawable.Drawable;

import java.io.Serializable;

public class EnumNavigationDrawer implements Serializable
{
    public String Name;
    public Drawable Photo;

    public EnumNavigationDrawer(Context context, String name, Drawable photo)
    {
        Name = name;
        Photo = photo;
    }
}
