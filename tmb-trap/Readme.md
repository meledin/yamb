# Tiny Message Broker (tmb)
*tmb* is a tiny, web-socket based message broker supporting publish/subscribe and direct, named messaging. It is a perfect starter component for *amb*, or any other project where you need a quick message bus to get started before you reach a bigger message bus need. *tmb* is only a few hundred lines of code, so it is probably easier to read the code than documentation if something goes wrong.

# Usage (Java)
The following example shows how to use TMB. This is a very simple example. TMB is single threaded, so will not produce significant throughput, but will be easy to debug.

	// Create a new broker
	broker = new Broker(); 
	
	// Wait synchronously for broker to start
	broker.listen("localhost", 21242).get(); 
	
	// Create a new client. We will name this client "alice".
	alice = new Client("alice");
	
	// Add a handler for callbacks
	alice.setHandler(new Handler() 
	{

		// Called when Alice receives a DIRECT message (addressed to her)
		public void onDirect(String from, byte[] data)
		{
			System.out.println("Got data from " + from + ": " + new String(data));
		}

		public void onClose()
		{
		}

		public void onChannel(String channel, String from, byte[] data)
		{
			System.out.println("Got data from " + from + " on channel " + channel + ": " + new String(data));
		}
	});
	
	// Wait for alice to start
	alice.connect("localhost", 21242).get(); 
	
	// Let Alice listen to "irc".
	alice.subscribe("irc"); 
	
	//Create bob.
	bob = new Client("bob");
	
	// Add a handler for callbacks
	bob.setHandler(new Handler() {...});
	
	// Wait for bob to start
	bob.connect("localhost", 21242).get(); 
	
	// Let's send data!
	bob.publish("irc", data);
	bob.send("alice", data);
	
# Usage (JavaScript)
TMB is WebSocket native, so has a JavaScript client. The client can participate on the bus like any other node. Fun on a bun!

	mallory = new Client("mallory", "ws://localhost:21242");
	mallory.onopen = function() {
		mallory.subscribe("irc");
		mallory.send("alice", "hello"); // sends the bytes of "hello" to alice
		mallory.publish("irc", uint8Array); // sends the content of 
	};
	mallory.ondirect = function(evt) {
		var from = evt.from;
		var bytes = evt.data;
		var string = evt.string;
		console.log("Got data from " + from + ": " + evt.string);
	};
	mallory.onchannel = function(evt) {
		var from = evt.from;
		var bytes = evt.data;
		var string = evt.string;
		console.log("Got data from " + from + ": " + evt.string);
	};
	mallory.onclose = function() {
	};
	
# Why tmb?
*tmb* is primarily intended to be used as a basic test harness when running *amb*. It can be embedded into the JVM or even tests, starts up instantly, has few dependencies and is, well, *tiny*. It also supports both native and web clients.