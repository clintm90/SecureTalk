package com.github.liveflow;

import java.lang.reflect.Method;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * RemoteBus use Http and JSON*
 * @author Created by clint on 01/01/2015. 
 */
public class RemoteBus
{
    public String InstanceUID;
    public static String UID = UUID.randomUUID().toString();
    
    /**
     * Define a new remote bus*
     * @param namespace 
     * @param instance_uid an unique string by app instance, use UUID.randomUUID().toString() to get unique string
     */
    public RemoteBus(String namespace, String instance_uid)
    {
        InstanceUID = instance_uid;
    }

    /**
     * Register the method to call when bus is called *
     * @param method Method name to call
     * @param time_unit Define time unit
     * @param loop_time
     * @return true or false
     * @throws java.lang.NoSuchMethodException if the called method does not exist
     */
    public Liveflow.Bus Register(String method, TimeUnit time_unit, long loop_time)
    {
        Liveflow.Bus rts = new Liveflow.Bus();
        try
        {
            Method method1 = Method.class.getMethod(method, getClass());
            rts.RunningState = true;
            rts.Callback = method1;
            rts.LoopTime = 5000;
        }
        catch (NoSuchMethodException e)
        {
            e.printStackTrace();
        }
        return rts;
    }

    /**
     * Unregister a bus* 
     * @param bus_to_unregister
     * @return true or false
     */
    public boolean Unregister(Liveflow.Bus bus_to_unregister)
    {
        return bus_to_unregister.Listener.cancel();
    }

    /**
     * Call the specified bus*
     * @param instance_uid call the specified bus registered with the specified instance uid*
     */
    public void Call(String instance_uid)
    {
        Call(instance_uid, null);
    }

    /**
     * Call the specified bus with parameters*
     * @param instance_uid specified instance uid*
     * @param parameters The parameters to call * 
     */
    public void Call(String instance_uid, Object... parameters)
    {
    }
}