package com.trackcell.securetalk;

import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class DBSecureTalk extends SQLiteOpenHelper
{
    private Context mContext;

    public DBSecureTalk(Context context, String name, SQLiteDatabase.CursorFactory factory, int version, DatabaseErrorHandler errorHandler)
    {
        super(context, name, factory, version, errorHandler);
        mContext = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db)
    {
        try
        {
            db.execSQL("CREATE TABLE IF NOT EXISTS `Elements` (\n" +
                    "\t`ID`\tINTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,\n" +
                    "\t`GravatarID`\tVARCHAR NOT NULL UNIQUE,\n" +
                    "\t`Name`\tVARCHAR NOT NULL,\n" +
                    "\t`Description`\tTEXT NOT NULL,\n" +
                    "\t`PublicKey`\tTEXT NOT NULL\n" +
                    ");");
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
    {
        db.execSQL("DROP TABLE IF EXISTS \"Elements\";");
        onCreate(db);
    }

    public boolean NewElement(String ID, String name, String description, String publicKey)
    {
        try
        {
            SQLiteDatabase mDatabase = getWritableDatabase();
            mDatabase.execSQL("INSERT INTO `Elements` VALUES(NULL, \""+ID+"\", \""+name+"\", \""+description+"\", \""+publicKey+"\");");
            return true;
        }
        catch(Exception e)
        {
            return false;
        }
    }

    public List<EnumContact> GetElements()
    {
        List<EnumContact> mRTS = new ArrayList<EnumContact>();

        SQLiteDatabase mDatabase = getReadableDatabase();
        Cursor result = mDatabase.rawQuery("SELECT * FROM \"Elements\";", null);

        while(result.moveToNext())
        {
            mRTS.add(new EnumContact(mContext, result.getInt(0), result.getString(1), result.getString(2), result.getString(3), result.getString(4), false).singleLine().bwPhoto());
        }

        return mRTS;
    }

    public void RemoveElement(int id)
    {
        SQLiteDatabase mDatabase = getReadableDatabase();
        mDatabase.execSQL("DELETE FROM \"Elements\" WHERE ID=\""+String.valueOf(id)+"\";");
    }

    public void AlterElement(int id, String name, String description, String publicKey)
    {
        SQLiteDatabase mDatabase = getWritableDatabase();
        mDatabase.execSQL("UPDATE \"Elements\" SET \"Name\" = \""+name+"\", \"Description\" = \""+description+"\", \"PublicKey\" = \""+publicKey+"\" WHERE \"ID\" = "+id+";");
    }
}
