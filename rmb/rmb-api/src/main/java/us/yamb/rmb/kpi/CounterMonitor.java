package us.yamb.rmb.kpi;

/**
 * Monontonously increasing counter.
 * 
 * @author Vladimir Katardjiev
 */
public interface CounterMonitor extends BaseMonitor
{
    public void increment();
}
