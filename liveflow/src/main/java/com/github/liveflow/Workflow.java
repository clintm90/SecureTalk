package com.github.liveflow;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Implement workflows
 * @author Clint Mourlevat
 */
public class Workflow
{
    public static final int JAVA = 0x0;
    public static final int PHP = 0x0;

    Map<String, Method> Methods;
    public Object Parent;

    /**
     * Initialize a workflow
     * @param context Set the receiver context
     */
    public Workflow(Object context)
    {
        Methods = new HashMap<String, Method>();
        Parent = context;
    }

    /**
     * Call the specified workflow
     * @param service_name Name of the service
     * @param parameters
     * @return true if correctly called
     */
    public boolean Call(String service_name, Object... parameters)
    {
        try
        {
            Methods.get(service_name).invoke(Parent);
        }
        catch (IllegalAccessException e)
        {
            e.printStackTrace();
        }
        catch (InvocationTargetException e)
        {
            e.printStackTrace();
        }
        return true;
    }

    public boolean Register(String service_name, Object language, String method_to_call)
    {
        try
        {
            Methods.put(service_name, Parent.getClass().getMethod(method_to_call, null));
            return true;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return false;
        }
    }
}
