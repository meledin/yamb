(function(target) {

	var Trap = {};
	
	Trap._uidCounter = 0;
	Trap._uid = function()
	{
		return Math.random().toString(16).substring(2) + (Trap._uidCounter++).toString(16);
	};
	
	Trap.EventObject = function()
	{
		this._eventlistenersMap = {};
	};

	Trap.EventObject.prototype.addEventListener = function(type, listener) {
	    if (!this._eventlistenersMap[type])
	        this._eventlistenersMap[type] = [];
	    var eventlisteners = this._eventlistenersMap[type];
	    for (var i = 0; i<eventlisteners.length; i++) {
	        if(listener === eventlisteners[i])
	            return;
	    }
	    eventlisteners[i] = listener;
	};

	Trap.EventObject.prototype.removeEventListener = function(type, listener) {
	    if (!this._eventlistenersMap[type])
	        return;
	    var eventlisteners = this._eventlistenersMap[type];
	    for (var i = 0; i < eventlisteners.length; i++) {
	        if (listener === eventlisteners[i]) {
	            eventlisteners.splice(i,1);
	            break;
	        }
	    }
	};

	Trap.EventObject.prototype.on = Trap.EventObject.prototype.addEventListener;
	Trap.EventObject.prototype.off = Trap.EventObject.prototype.removeEventListener;

	Trap.EventObject.prototype._dispatchEvent = function(evt) {
	    var listeners = this._eventlistenersMap[evt.type];
	    
	    if (!evt.target)
	    	evt.target = this;
	    
	    var found = false;
	    
	    if(!!listeners)
	    {
	    	for (var i = 0; i < listeners.length; i++)
	    	{
	    		try
	    		{
	        		listeners[i](evt);
	        		found = true;
	    		}
	    		catch (e)
	    		{
	    			if (this.logger)
	    			{
	    				this.logger.warn("Exception while dispatching event to listener; ", e, " to ", listeners[i], ". Event was ", evt);
	    			}
	    		}
	    		
	    	}
	    }
	    
	    var f;
		try
		{
		    f = this["on"+evt.type];
		    if (f && typeof(f) == "function") {
		    	f.call(this, evt);
		    	found = true;
		    }
		}
		catch (e)
		{
			if (this.logger)
			{
				this.logger.warn("Exception while dispatching event to listener; ", e, " to ", f, ". Event was ", evt);
			}
		}
		
		return found;
	};
	
	var Location = function(src) {
		var qpos = src.search("?");
		
		this.path = "";
		this.query = "";
		this.location = src;
		
		if (qpos > -1)
		{
			this.path = src.substring(0, qpos);
			this.query = src.substring(qpos+1);
		}
		else
		{
			this.path = src;
		}
		
		this.parts = this.path.split("/");
		
	}
	
	var dispatchFun = function(msg) {
		var to = msg.location.parts[msg.idx];
		if (this._id == to)
		{
			
			if (!!msg.method)
			{
				msg.type = msg.method;
				if (this._dispatchEvent(msg))
					return true;
			}
			
			msg.type = "message";
			return this._dispatchEvent(msg);
		}
		
		msg.idx++;
		var child = this.children(msg.location.parts[msg.idx]);
		
		if (!!child)
			return child._dispatchEvent(msg);
		return false;
	};
	
	var RMB = function(amb, options) {
		var rmb = this;
		Trap.EventObject.prototype.constructor(this);
		this._amb = amb;
		amb.onopen = function() { rmb._dispatchEvent({type: open})};
		amb.ondirect = function(msg) { 
			var evt = JSON.parse(msg.string);
			evt.type = "dispatch";
			evt.location = new Location(evt.to);
			evt.idx = 1;
			rmb._dispatchEvent(evt);
		};
		this.ondispatch = dispatchFun;
	};
	
	RMB.prototype = new Trap.EventObject;
	RMB.prototype.constructor = RMB;
	
	RMB.prototype.create = function(id) {
		if (!id)
			id=Trap._uid();
		children[id] = new Child(this, id);
	}
	
	RMB.prototype.request = function(src) {
		if (!src) src = this;
		var msg = {};
		var rmb = this;
		return {
			to: function(str) { msg.to = str.location ? str.location : str; },
			data: function(data) {
				if (typeof(data) == "string")
					msg.data = data;
				if (typeof(data) == "object")
					msg.data = JSON.serialize(data);
			},
			confirmed: function(bool) { msg.confirmed = !!bool; },
			method: function(str) { msg.method = str; },
			status: function(num) { msg.status = num; },
			execute: function(cb) {
				var res = src.create();
				res.onmessage = function(msg) {
					cb(msg);
				};
				var payload = JSON.stringify(msg);
				var loc = new Location(msg.to);
				rmb._amb.message().to("/" + loc.parts[1]).from(rmb.id()).data(payload).send();
				
			}
		};
	};
	
	RMB.prototype.message = function(src) {
		if (!src) src = this;
		var msg = {};
		var rmb = this;
		return {
			to: function(str) { msg.to = str.location ? str.location : str; },
			data: function(data) {
				if (typeof(data) == "string")
					msg.data = data;
				if (typeof(data) == "object")
					msg.data = JSON.serialize(data);
			},
			confirmed: function(bool) { msg.confirmed = !!bool; },
			method: function(str) { msg.method = str; },
			status: function(num) { msg.status = num; },
			send: function() {
				msg.from = src.id();
				var payload = JSON.stringify(msg);
				var loc = new Location(msg.to);
				rmb._amb.message().to("/" + loc.parts[1]).from(rmb.id()).data(payload).send();
				
			}
		};
	};
	
	RMB.prototype.pipe = function() {
		return {
			
		};
	};
	
	RMB.prototype.channel = function() {
		return this._amb.channel();
	};
	
	RMB.prototype.connect = function() {
		// Does not need to be called.
	};
	
	RMB.prototype.disconnect = function() {
		this._amb.disconnect();
	};
	
	RMB.prototype.id = function() {
		return "/"+this._amb.id();
	};
	
	var Child = function(parent, id) {
		Trap.EventObject.prototype.constructor(this);
		this.parent = parent;
		this._id = id;
		this.ondispatch = dispatchFun;
	}

	Child.prototype = new RMB;
	Child.prototype.constructor = Child;
	
	
	var builder = function() {
		var seedArg, idArg;
		return {
			seed: function(src) { seedArg = src; return this; },
			id: function(src) { idArg = src; return this; },
			build: function() {
				var amb = AMB.builder().seed(seedArg).id(idArg).build();
				return new RMB(amb, {seed: seedArg, id: idArg});
			}
		};
	};
	
})(self);
