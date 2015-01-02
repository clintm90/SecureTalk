package com.github.liveflow;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author Clint Mourlevat* 
 */
public class LocalBus
{
    public Object Parent;
    public String Namespace;
    public Method Callback;
    
    public static LocalBus getNamespace()
    {
        LocalBus rts = new LocalBus("nsp");
        rts.Namespace = "null";
        return rts;
    }
    
    public LocalBus(String namespace)
    {
    }
    
    public boolean Register(Object parent, String method)
    {
        try
        {
            Method method1 = parent.getClass().getMethod(method, null);
            Parent = parent;
            Callback = method1;
            return true;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return false;
        }
    }
    
    public void Call()
    {
        try
        {
            Callback.invoke(Parent);
        }
        catch (IllegalAccessException e)
        {
            e.printStackTrace();
        }
        catch (InvocationTargetException e)
        {
            e.printStackTrace();
        }
    }

    public void Call(Object... parameters)
    {
    }
}
