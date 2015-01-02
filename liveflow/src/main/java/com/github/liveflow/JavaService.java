package com.github.liveflow;

/**
 * Created by clint on 02/01/2015.
 */
public abstract class JavaService<Type>
{
    protected abstract void Run(Type... types);
}
