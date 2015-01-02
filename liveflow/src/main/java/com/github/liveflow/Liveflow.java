package com.github.liveflow;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.TimerTask;

/*
 * @author Clint Mourlevat
 */
public class Liveflow
{
    public static int REGISTER = 0xA;
    
    public static String WebRequest(String url)
    {
        try
        {
            String rts = "", c;
            BufferedReader reader = new BufferedReader(new InputStreamReader(new URL(url).openStream()));

            while ((c = reader.readLine()) != null)
                rts += c;
            return rts;
        }
        catch(Exception e)
        {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Created by clint on 01/01/2015.
     * @author Clint Mourlevat*
     */
    public static class Bus
    {
        public static final String ASYNCHRONOUS = "async";
        public static final String ALL = "all";
        public boolean RunningState = false;
        public TimerTask Listener;
        public Method Callback;
        public long LoopTime;
    }
}
