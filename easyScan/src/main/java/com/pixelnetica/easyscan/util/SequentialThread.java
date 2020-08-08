package com.pixelnetica.easyscan.util;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.pixelnetica.easyscan.AppLog;

import java.util.HashSet;

/**
 * Worker thread to make something in sequence order
 * Created by Denis on 30.04.2015.
 */
public abstract class SequentialThread extends HandlerThread{
    protected static final int EXIT_THREAD = 0;

    /**
     * Add task to end of message queue
     */
    public static final int NORMAL_TASK = 0;

    /**
     * Add task to head of message queue
     */
    public static final int PRIORITY_TASK = 1;

    /**
     * Remove all previous task this type
     */
    public static final int SINGLE_TASK = 2;

    /**
     * Handler for thread to main processing
     */
    private Handler mWorkerHandler = null;

    /**
     * Handler to notify caller (main) thread
     */
    private final Handler mNotifyHandler = new Handler(Looper.getMainLooper());

    /**
     * All task types (to remove all before finish)
     */
    private final HashSet<Integer> mTaskTypes = new HashSet<>();

    // A constructor
    protected SequentialThread(final String name)
    {
        super(name);
    }

    /**
     * Some prepared work
     */
    protected abstract void onThreadStarted();

    /**
     * Something cleanup
     */
    protected abstract void onThreadComplete();

    protected abstract Runnable handleThreadTask(int type, Object params);

    @Override
    protected void onLooperPrepared()
    {
        super.onLooperPrepared();

        onThreadStarted();

        // Create message handler for a loop
	    Log.d(AppLog.TAG, "Create mWorkerHandler");
	    final Handler handler = new Handler(getLooper(), new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                if (msg.what == EXIT_THREAD) {
                    // Try to destroy message loop
                    if (!quit()) {
                        // Hardly interrupt thread if something wrong with looper
                        interrupt();
                    }
                    return true;
                } else if (mTaskTypes.contains(msg.what)) {
                    final Runnable r = handleThreadTask(msg.what, msg.obj);
                    if (r != null) {
                        mNotifyHandler.post(r);
                    }
                    return true;
                } else {
                    // Default processing
                    return false;
                }
            }
        });

	    synchronized (this) {
		    mWorkerHandler = handler;
		    notifyAll();
	    }
    }

    @Override
    public void run() {
        try {
            super.run();
        } finally {
            // No post-looper overrides in HandlerThread :(
            onThreadComplete();
        }
    }

    public synchronized boolean isReady() {
	    return isAlive() && mWorkerHandler != null;
    }

    private void checkWorkingThread() {
        // Safe wait for thread started
        synchronized (this) {
            if (isAlive() && mWorkerHandler == null) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    // dummy
                }
            }
        }

        if (mWorkerHandler == null) {
            throw new IllegalStateException("Thread is not ready yet");
        }
    }

    protected void addThreadTask(int type, Object params, int priority, boolean allowDuplicate) {
        // Check reserved task type
        if (type == EXIT_THREAD) {
            throw new IllegalArgumentException("Task type cannot be 0");
        }

	    // Safe wait for thread started
        checkWorkingThread();

	    //Log.d(AppLog.TAG, "AddThreadTask for handler" + ((mWorkerHandler == null) ? " null!" : ""));
	    synchronized (this)
        {
            // Remove all pending messages if any
            if (priority == SINGLE_TASK) {
                mWorkerHandler.removeMessages(type);
            } else if (allowDuplicate && params != null) {
	            mWorkerHandler.removeMessages(type, params);
            }

            // Create new message
            final Message msg = mWorkerHandler.obtainMessage(type, params);

            // Store task type (to remove all on finish)
            mTaskTypes.add(type);

            if (priority == PRIORITY_TASK) {
	            mWorkerHandler.sendMessageAtFrontOfQueue(msg);
            } else {
                mWorkerHandler.sendMessage(msg);
            }
        }
    }

    protected synchronized void removeThreadTask(int type, Object params) {
	    checkWorkingThread();
	    synchronized (this) {
		    mWorkerHandler.removeMessages(type, params);
	    }
    }

    public void finish()
    {
        // Just in case.
        if (!isAlive()) {
            return;
        }

        synchronized (this) {
            // Remove all pending messages if any
            for (int type : mTaskTypes) {
                if (type != EXIT_THREAD) {
                    mWorkerHandler.removeMessages(type);
                }
            }

            // Send message to quit
            mWorkerHandler.sendEmptyMessage(EXIT_THREAD);
        }

        // Wait for thread termination
        try {
            join();
        } catch (InterruptedException ex)
        {
            // Nothing
        }
    }
}
