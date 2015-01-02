package com.github.liveflow;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

/**
 * LiveMessage is an API for inter-app messaging
 * @author Clint Mourlevat * 
 */
public class LiveMessage
{
    public static String DEFAULT_SERVER = "http://securetalk-sql.sourceforge.net/";
    
    private Timer MainTimer;
    
    public String ServerName;
    public String ApplicationName;
    private OnReceive Receiver;

    /**
     * Initialize a live message class*
     * @param server_name Define a custom server use LiveMessage.DEFAULT_SERVER
     * @param application_name Application name ID use package name
     */
    public LiveMessage(String server_name, String application_name)
    {
        MainTimer = new Timer();
        ServerName = server_name;
        ApplicationName = application_name;
    }

    /**
     * Post your message to the current server
     * @param recipient Recipient of the message use LiveMessage.ALL for sending to all recipient
     * @param message Message content
     * @return true if correctly send
     */
    public boolean PostMessage(String recipient, String message)
    {
        return true;
    }

    /**
     * Define a message receiver
     * @param instance_id Name of the instance
     * @param loop_time Loop time listener in milliseconds
     * @param message_receiver OnReceive listener
     */
    public void setListener(String instance_id, int loop_time, OnReceive message_receiver)
    {
        Receiver = message_receiver;

        MainTimer.scheduleAtFixedRate(new TimerTask()
        {
            @Override
            public void run()
            {
                Update();
            }
        }, 70, loop_time);
    }
    
    public void Update()
    {
        LiveMessage.SimpleMessage msg = new LiveMessage.SimpleMessage();
        msg.Content = "salut";
        msg.Checksum = "none";
        msg.SendDate = new Date();
        Receiver.onReceive(msg);
    }

    /*public void setListener(OnReceive onReceive)
    {
        setListener("default", 5000, onReceive);
    }*/

    /**
     * Close the remote connection
     */
    public void Destroy()
    {
        MainTimer.cancel();
    }

    /**
     * Listener loop task
     */
    private class MainLoop extends TimerTask
    {
        public void run()
        {
            LiveMessage.SimpleMessage msg = new LiveMessage.SimpleMessage();
            msg.Content = "salut";
            msg.Checksum = "none";
            msg.SendDate = new Date();
            Receiver.onReceive(msg);
            //Thread.currentThread().setName("MessageWorker_MainLoop");
        }
    }

    /**
     * Define a message receiver
     */
    public static interface OnReceive
    {
        public void onReceive(LiveMessage.SimpleMessage content);
    }

    /**
     * Simple message
     */
    public static class SimpleMessage
    {
        public Date SendDate;
        public String Content;
        public String Checksum;
    }
}
