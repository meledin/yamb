package us.yamb.amb.tmb;

import us.yamb.tmb.Broker;

public class MainServer
{
	private static Broker broker;

	public static void main(String[] args) throws InterruptedException {
		broker = new Broker();
		broker.listen("localhost", 12345).get();
		
		for(;;)
			Thread.sleep(1000);
	}
}
