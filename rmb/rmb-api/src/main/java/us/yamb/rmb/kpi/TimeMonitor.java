package us.yamb.rmb.kpi;

/**
 * Monitor for time durations (min/max/average).
 * 
 * @author Vladimir Katardjiev
 */
public interface TimeMonitor extends BaseMonitor
{
    
    public interface Stopwatch
    {
        /**
         * Stops the timer and records the time taken.
         */
        public void stop();
    }
    
    /**
     * Creates a stopwatch object, which finishes a single time measurement.
     * 
     * @return
     */
    public Stopwatch start();
    
}
