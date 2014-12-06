package us.yamb.amb.spi;

import us.yamb.amb.AMB;
import us.yamb.amb.AMBStatus;
import us.yamb.amb.callbacks.AMBCallbackInterface;
import us.yamb.amb.callbacks.OnDisconnect;
import us.yamb.amb.callbacks.OnMessage;

public abstract class AMBase extends ObservableBase<AMBCallbackInterface, AMB> implements AMB
{
	protected AMBStatus	   status	= AMBStatus.DISCONNECTED;
	protected OnMessage	   onmessage;
	protected OnDisconnect	ondisconnect;

	public AMBStatus status()
	{
		return status;
	}
}
