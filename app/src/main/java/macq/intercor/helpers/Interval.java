package macq.intercor.helpers;

import android.os.Handler;
import android.os.Looper;

public class Interval {
    // Create a Handler that uses the Main Looper to run in
    private Handler mHandler = new Handler(Looper.getMainLooper());

    private Runnable mStatusChecker;
    private int UPDATE_INTERVAL = 2000;

    /**
     * @param runnable A runnable containing the update routine.
     */
    public Interval(final Runnable runnable) {
        mStatusChecker = new Runnable() {
            @Override
            public void run() {
                // Run the passed runnable
                runnable.run();
                // Re-run it after the update interval
                mHandler.postDelayed(this, UPDATE_INTERVAL);
            }
        };
    }

    /**
     * The same as the default constructor, but specifying the
     * intended update interval.
     *
     * @param runnable A runnable containing the update routine.
     * @param interval  The interval over which the routine
     *                  should run (milliseconds).
     */
    public Interval(Runnable runnable, int interval){
        this(runnable);
        UPDATE_INTERVAL = interval;
    }

    /**
     * Starts the periodical update routine (mStatusChecker
     * adds the callback to the handler).
     */
    public synchronized void start(){
        mStatusChecker.run();
    }

    /**
     * Stops the periodical update routine from running,
     * by removing the callback.
     */
    public synchronized void stop(){
        mHandler.removeCallbacks(mStatusChecker);
    }
}