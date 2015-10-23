package us.yamb.rmb.impl;

import us.yamb.rmb.kpi.CounterMonitor;
import us.yamb.rmb.kpi.TimeMonitor;

public class MonitorFactory
{
    
    public static MonitorFactory factory = new MonitorFactory();
    
    protected CounterMonitor createCounter(String name)
    {
        return new CounterMonitor() {
            
            @Override
            public void increment()
            {
                
            }
        };
    }
    
    public static CounterMonitor counter(String name)
    {
        return factory.createCounter(name);
    }
    
    public static TimeMonitor timer(String name)
    {
        return factory.createTimer(name);
    }
    
    protected TimeMonitor createTimer(String name)
    {
        return new TimeMonitor() {
            
            @Override
            public Stopwatch start()
            {
                return new Stopwatch() {
                    
                    @Override
                    public void stop()
                    {
                        
                    }
                };
            }
        };
    }
    
    public static void register(Object obj, String name)
    {
        factory.doRegister(obj, name);
    }

    protected void doRegister(Object obj, String name)
    {
        
    }
}
