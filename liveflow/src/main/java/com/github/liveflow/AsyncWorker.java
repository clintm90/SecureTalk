package com.github.liveflow;

/**
 * Implement an asynchronous task
 * @author Clint Mourlevat
 */
public abstract class AsyncWorker<Input, Output, Cancel>
{
    private Thread CurrentThread;
    private boolean isCancelled = false;

    /**
     * Intialize an asynchronous worker 
     */
    public AsyncWorker()
    {
    }

    /**
     * Cancel the currently task
     * @param cancel_reason Argument when you cancel the taskk
     * @return True if the task was correcly cancelled
     */
    public boolean Cancel(Cancel... cancel_reason)
    {
        try
        {
            isCancelled = true;
            CurrentThread.interrupt();
            onCancel(cancel_reason);
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        return true;
    }

    protected abstract Output onStart(Input... inputs);

    protected abstract void onEnd(Output output);

    protected abstract void onCancel(Cancel... cancel);

    /**
     * Run the current task
     * @param inputs Specify the inputs
     */
    public void Run(final Input... inputs)
    {
        Run(Thread.NORM_PRIORITY, inputs);
    }

    /**
     * Runt the current task with specified priority
     * @param priority Specify the thread priority 0 -> 10
     * @param inputs Specify the inputs
     */
    public void Run(int priority, final Input... inputs)
    {
        CurrentThread = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                Output t = onStart(inputs);
                if(!isCancelled)
                {
                    onEnd(t);
                }
            }
        });
        CurrentThread.setPriority(priority);
        CurrentThread.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler()
        {
            @Override
            public void uncaughtException(Thread t, Throwable e)
            {
                Cancel(null);
                e.printStackTrace();
            }
        });
        CurrentThread.run();
    }

    public void Timeout(long timeout)
    {
        try
        {
            CurrentThread.join(timeout);
        }
        catch(InterruptedException e)
        {
            Cancel(null);
            e.printStackTrace();
        }
        
    }
}
