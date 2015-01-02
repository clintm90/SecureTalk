package com.github.liveflow;

import com.sun.rowset.internal.Row;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * Work with stack
 * @author Clint Mourlevat 
 */
public class Stack
{
    public final static String DEFAULT_STACK = "default.db";
    
    /**
     * Initialize a new stack or open it 
     * @param filename
     */
    public Stack(String filename)
    {
    }

    /**
     * Put the content to the stack
     * @param content The specified content
     * @return Return the row ID
     */
    public String Push(String content)
    {
        return Push(content, "default");
    }

    /**
     * Put the content to the stack
     * @param content The specified content
     * @param owner The specified owner
     * @return Return the row ID
     */
    public String Push(String content, String owner)
    {
        RowStack row = null;
        row.ID = "0";
        row.Owner = owner;
        row.InitDate = new Date();
        row.Content = content;
        return row.ID;
    }

    /**
     * Return content by the specified row
     * @param row
     * @return
     */
    public String GetContentByRow(RowStack row)
    {
        return null;
    }

    /**
     * Retrieve content by id
     * @param id The specified content id to retrieve
     * @return Return the content
     */
    public String RetrieveContentByID(String id)
    {
        return null;
    }
    
    /**
     * Get value from stack
     * @param id The specified content id to retrieve
     * @return The content by id
     */
    public RowStack RetrieveByID(String id)
    {
        return null;
    }

    /**
     * Retrieve a value by date 
     * @param date
     * @return
     */
    public List<Row> RetrieveByDate(Date date)
    {
        return null;
    }

    /**
     * Get value from owner 
     * @param owner The specified owner
     * @return
     */
    public String RetrieveByOwner(String owner)
    {
        return null;
    }

    private static class RowStack implements Serializable
    {
        public String ID;
        public Date InitDate;
        public String Owner;
        public String Content;
    }
}
