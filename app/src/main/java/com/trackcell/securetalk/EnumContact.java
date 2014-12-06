package com.trackcell.securetalk;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;

import java.io.Serializable;

public class EnumContact implements Serializable
{
    public boolean ArrowSelectable = false;
    public boolean isPaddingRight = true;
    public boolean isPhtoBW = false;
    public int PhotoVisibility = View.VISIBLE;
    public int ArrowVisibility = View.VISIBLE;

    public String ID;
    public String Name;
    public Drawable Photo;
    public String Description;
    public String PublicKey;
    public boolean Bold;

    public EnumContact(Context context, String id, String name, String description, String publicKey, boolean bold)
    {
        ID = id;
        Name = name;
        Description = description;
        PublicKey = publicKey;
        Bold = bold;
        /*if(photo != null)
        {
            Photo = photo;
        }*/
    }

    public EnumContact validArrow()
    {
        ArrowSelectable = true;
        return this;
    }

    public EnumContact hidePhoto()
    {
        PhotoVisibility = View.GONE;
        return this;
    }

    public EnumContact hideArrow()
    {
        ArrowVisibility = View.INVISIBLE;
        isPaddingRight = false;
        return this;
    }

    public EnumContact bwPhoto()
    {
        isPhtoBW = true;
        return this;
    }
}
