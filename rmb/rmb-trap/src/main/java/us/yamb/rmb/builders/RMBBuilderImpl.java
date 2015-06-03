package us.yamb.rmb.builders;

import us.yamb.mb.util.UID;
import us.yamb.rmb.RMB;
import us.yamb.rmb.impl.ClientRMBRoot;
import us.yamb.rmb.impl.RMBRootImpl;
import us.yamb.rmb.impl.ServerRMBRoot;

import com.ericsson.research.trap.utils.Configuration;
import com.ericsson.research.trap.utils.spi.ConfigurationImpl;

public class RMBBuilderImpl extends RMBBuilder
{
    
    RMBRootImpl           rmb;
    private Configuration options = new ConfigurationImpl();
    
    public RMBBuilderImpl() throws InstantiationException, IllegalAccessException, ClassNotFoundException
    {
    }
    
    @Override
    public RMBBuilder seed(String seedPeerInfo)
    {
        if (seedPeerInfo.startsWith("client:"))
        {
            rmb = new ClientRMBRoot(options);
            seedPeerInfo = seedPeerInfo.substring("client:".length());
        }
        else if (seedPeerInfo.startsWith("server:"))
        {
            rmb = new ServerRMBRoot(options);
            seedPeerInfo = seedPeerInfo.substring("server:".length());
        }
        options.initFromString(seedPeerInfo);
        return this;
    }
    
    @Override
    public RMBBuilder id(String id)
    {
        options.setOption(RMBRootImpl.ID, id);
        return this;
    }
    
    @Override
    public RMBBuilder handle(byte[] handle)
    {
        return this;
    }
    
    @Override
    public RMBBuilder configure(String key, Object value)
    {
        options.setOption(key, value.toString());
        return this;
    }
    
    @Override
    public Object getConfig(String key)
    {
        return options.getOption(key);
    }
    
    @Override
    public RMB build()
    {
        rmb.setName(options.getStringOption(RMBRootImpl.ID, UID.randomUID()));
        return rmb;
    }
    
}
