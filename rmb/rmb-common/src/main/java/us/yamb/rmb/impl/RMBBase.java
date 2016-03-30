package us.yamb.rmb.impl;

import us.yamb.amb.spi.ObservableBase;
import us.yamb.rmb.RMB;
import us.yamb.rmb.RMBStatus;
import us.yamb.rmb.callbacks.OnDelete;
import us.yamb.rmb.callbacks.OnDisconnect;
import us.yamb.rmb.callbacks.OnGet;
import us.yamb.rmb.callbacks.OnMessage;
import us.yamb.rmb.callbacks.OnPost;
import us.yamb.rmb.callbacks.OnPut;
import us.yamb.rmb.callbacks.RMBCallbackInterface;

public abstract class RMBBase extends ObservableBase<RMBCallbackInterface, RMB> implements RMB
{
	protected RMBStatus	   status	= RMBStatus.DISCONNECTED;
	protected OnMessage	   onmessage;
	protected OnGet	       onget;
	protected OnPut	       onput;
	protected OnPost	   onpost;
	protected OnDelete	   ondelete;
	protected OnDisconnect	ondisconnect;

	public RMBStatus status()
	{
		return status;
	}

}
