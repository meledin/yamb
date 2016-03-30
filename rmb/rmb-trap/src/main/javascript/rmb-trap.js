(function(target) {
	
	var Location = function(src) {
		var qpos = src.search("\\?");
		
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
	
	var Message = function(){
		this.headers = {
			"Confirmed" : "false",
			"Id" : "0",
			"Status" : "0"
		};
		this.data = new Uint8Array();
	};
	
	Message.prototype._string = function() {
		return String.fromUTF8ByteArray(this.data);
	};
	Message.prototype._object = function() {
		return JSON.parse(this.string);
	};
	
	Trap._compat.__defineGetter(Message.prototype, "string", Message.prototype._string);
	Trap._compat.__defineGetter(Message.prototype, "object", Message.prototype._object);

	Trap._compat.__defineGetter(Message.prototype, "to", function() { return this.headers["To"]; });
	Trap._compat.__defineGetter(Message.prototype, "from", function() { return this.headers["From"]; });
	Trap._compat.__defineGetter(Message.prototype, "contentType", function() { return this.headers["ContentType"]; });
	Trap._compat.__defineGetter(Message.prototype, "method", function() { return this.headers["Method"]; });
	Trap._compat.__defineGetter(Message.prototype, "status", function() { return this.headers["Status"]; });
	Trap._compat.__defineGetter(Message.prototype, "confirmed", function() { return this.headers["Confirmed"]; });
	Trap._compat.__defineGetter(Message.prototype, "id", function() { return this.headers["Id"]; });

	Trap._compat.__defineSetter(Message.prototype, "to", function(v) { this.headers["To"] = v; return this; });
	Trap._compat.__defineSetter(Message.prototype, "from", function(v) { this.headers["From"] = v; return this; });
	Trap._compat.__defineSetter(Message.prototype, "contentType", function(v) { this.headers["ContentType"] = v; return this; });
	Trap._compat.__defineSetter(Message.prototype, "method", function(v) { this.headers["Method"] = v; return this; });
	Trap._compat.__defineSetter(Message.prototype, "status", function(v) { this.headers["Status"] = v; return this; });
	Trap._compat.__defineSetter(Message.prototype, "confirmed", function(v) { this.headers["Confirmed"] = v; return this; });
	Trap._compat.__defineSetter(Message.prototype, "id", function(v) { this.headers["Id"] = v; return this; });
	
	Message.Headers = {};
	Message.Headers.addHeader = function(name, val) { Message.Headers[name]=val; Message.Headers[val]=name; };
	Message.Headers.addHeader("To", 1);
	Message.Headers.addHeader("From", 2);
	Message.Headers.addHeader("ContentType", 3);
	Message.Headers.addHeader("Method", 4);
	Message.Headers.addHeader("Status", 5);
	Message.Headers.addHeader("Confirmed", 6);
	Message.Headers.addHeader("Id", 7);
	Message.Headers.addHeader("Custom", 255);
	Message.Headers.fromId = function(id) {
		return Message.Headers[id];
	};
	Message.Headers.toId = function(id) {
		return Message.Headers[id];
	};
	
	Message.prototype._header = function(k, v) {
		this.headers[k] = v;
		return this;
	}
	
	Message.parse = function(data) {
		var in8 = new Uint8Array(data);
		
		// Read the entire message length. 
		var length = Trap.ByteConverter.fromBigEndian(in8, 0);
		
		// Read the number of headers
		var nHeaders = in8[4];
		
		// The preamble length (in bytes) is 8 bytes (the amount we read this far)
		// plus 4 bytes that descripe a header name/value pair.
		var preambleLength = 8+4*nHeaders;
		
		var headerIds = [];
		var headerLengths = [];
		
		// We now read the appropriate number of headers.
		// The offset in bytes thus far is 8, so 8/2=4 is our offset.
		var _id = [0,0,0,0], _len = [0,0,0,0];
		for (var i=0; i < nHeaders; i++)
		{
			_id[2] = in8[8+i*4+0];
			_id[3] = in8[8+i*4+1];
			
			_len[2] = in8[8+i*4+2];
			_len[3] = in8[8+i*4+3];
			
			headerIds[i] = Trap.ByteConverter.fromBigEndian(_id, 0);
			headerLengths[i] = Trap.ByteConverter.fromBigEndian(_len, 0);
		}
		
		var msg = new Message();
		
		// Okay, now what? Time to read the header values.
		// All header values come packed in the buffer with no separators. We have read out all the lengths
		// above though, so now we get the values.
		var headerOffset = 8 + nHeaders * 4;
		
		for (var i=0; i < nHeaders; i++)
		{
			var hnameLength = Math.max(0, headerIds[i]-255);
			var value = String.fromUTF8ByteArray(in8, headerOffset + hnameLength, headerLengths[i]);
			
			if (hnameLength > 0)
			{
				var name = String.fromUTF8ByteArray(in8, headerOffset, hnameLength);
				msg._header(name, value);
			}
			else
			{
				var header = Message.Headers.fromId(headerIds[i]);
				msg._header(header, value);
			}

			headerOffset += hnameLength + headerLengths[i];
			preambleLength += hnameLength + headerLengths[i];
			
		}
		
		var bodyLength = Trap.ByteConverter.fromBigEndian(in8, headerOffset);
		
		if (bodyLength + preambleLength + 4 != length)
			throw "Invalid message; expected message length of " + (bodyLength+preambleLength+4) + " but was " + length + ". This is most likely a corrupt message.";

		var body = new Uint8Array(in8.buffer.slice(headerOffset+4));

		msg.data = body;

		return msg;
		
	}
	
	Message.prototype.pack = function() {
		
		var numHeaders = 0;
		for (var k in this.headers) numHeaders++;

		var headerLength = 0; // Placeholder until we can calculate this.
		var postLength = 4 + this.data.length;

		var out = new Trap.ByteArrayOutputStream();
		out.write(0); // Placeholder until we can calculate the total
		out.write(0);
		out.write(0);
		out.write(0);
		
		out.write(numHeaders);
		out.write(0);
		out.write(0);
		out.write(0);
		
		for (var hName in this.headers)
		{
			var hId = Message.Headers.toId(hName);
			
			var hIdArr = Trap.ByteConverter.toBigEndian(hId);
			out.write(hIdArr[2]);
			out.write(hIdArr[3]);
			
			var val = this.headers[hName];
			var valLength = Trap.ByteConverter.toBigEndian(val.length);
			out.write(valLength[2]);
			out.write(valLength[3]);
		}
		
		for (var hName in this.headers)
		{
			var val = this.headers[hName];
			out.write(val);
		}

		out.write(Trap.ByteConverter.toBigEndian(this.data.length));
		out.write(this.data);
		
		var packed = out.toArray();
		var lengthBytes = Trap.ByteConverter.toBigEndian(packed.length);
		
		for (var i=0; i<4; i++)
			packed[i] = lengthBytes[i];

		return packed;
	}
	
	var dispatchFun = function(msg) {
		var to = msg.location.parts[msg.idx];
		if (this._id == to)
		{
			msg.idx++;
			if (msg.location.parts.length == msg.idx)
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
			
			var child = this.children[msg.location.parts[msg.idx]];
			
			if (!!child)
				return child._dispatchEvent(msg);
		}
		
		
		return false;
	};
	
	var RMB = function(options) {
		
		if (!!options) {
			var rmb = this;
			Trap.EventObject.prototype.constructor.call(this);
			
			this._ep = new Trap.ClientEndpoint(options.seed);
			this._ep.onopen = function() {
				rmb._ep.send({"OP_TYPE" : "OPERATION_REGISTER", "REGISTERED_ID" : options.id}, 1, false);
			};
			
			this._ep.onmessage = function(evt) {
				console.error("Got: ", evt);
				if (evt.channel == 1)
				{
					var ctrl = evt.object;
					if (ctrl["OP_TYPE"] == "OPERATION_REGISTERED")
					{
						rmb._id = ctrl["APPROVED_ID"];
						
						if (typeof(rmb._id) == "undefined")
							rmb._id = options.id;
						
						rmb._dispatchEvent({type: "open"});
					}
				}
				
				var msg = Message.parse(evt.data);
				msg.type = "dispatch";
				msg.location = new Location(msg.to);
				msg.idx = 1;
				rmb._dispatchEvent(msg);
			};
			
			this._ep.onclose = this.onclose;
			
			this.ondispatch = dispatchFun;
			this.children = {};
			
			var id = 0;
			this.__trapSend = function(msg) {
				var channel = (id++%32)+2;
				var data = msg.pack();
				this._ep.send(data, channel, false);
			}
		}
	};
	
	RMB.prototype = new Trap.EventObject;
	RMB.prototype.constructor = RMB;
	
	RMB.prototype.create = function(id) {
		if (!id)
			id=Trap._uuid();
		
		if (!!this.children[id]) 
			return this.children[id];
		
		return this.children[id] = new Child(this, id);
	};
	
	RMB.prototype.get = function(to) {
		var req = this.request().method("GET");
		if (!!to) req.to(to);
		return req;
	};
	
	RMB.prototype.put = function(to) {
		var req = this.request().method("PUT");
		if (!!to) req.to(to);
		return req;
	};
	
	RMB.prototype.post = function(to) {
		var req = this.request().method("POST");
		if (!!to) req.to(to);
		return req;
	};
	
	RMB.prototype.delete = function(to) {
		var req = this.request().method("DELETE");
		if (!!to) req.to(to);
		return req;
	};
	
	RMB.prototype.request = function(src) {
		if (!src) src = this;
		var msg = new Message();
		var rmb = this;
		return {
			to: function(str) { msg.to = str.location ? str.location : str;  return this;},
			data: function(data) {
				if (typeof(data) == "string")
					msg.data = data;
				if (typeof(data) == "object")
					msg.data = JSON.stringify(data);
				 return this;
			},
			confirmed: function(bool) { msg.confirmed = !!bool; return this; },
			method: function(str) { msg.method = str;  return this;},
			status: function(num) { msg.status = num;  return this;},
			execute: function(cb) {
				var res = src.create();
				res.onmessage = function(msg) {
					cb(msg);
				};
				msg.from = res.id();
				self.lastMsg = msg;
				rmb.__trapSend(msg);
			}
		};
	};
	
	RMB.prototype.message = function(src) {
		if (!src) src = this;
		var msg = {};
		var rmb = this;
		return {
			to: function(str) { msg.to = str.location ? str.location : str; return this;},
			data: function(data) {
				if (typeof(data) == "string")
					msg.data = data;
				if (typeof(data) == "object")
					msg.data = JSON.stringify(data);
				return this;
			},
			confirmed: function(bool) { msg.confirmed = !!bool; return this;},
			method: function(str) { msg.method = str; return this;},
			status: function(num) { msg.status = num; return this;},
			send: function() {
				msg.from = src.id();
				rmb.__trapSend(msg);
			}
		};
	};
	
	RMB.prototype.pipe = function() {
		return {
			
		};
	};
	
	RMB.prototype.channel = function() {
		return {};
	};
	
	RMB.prototype.connect = function() {
		// Does not need to be called.
	};
	
	RMB.prototype.disconnect = function() {
		this._ep.disconnect();
	};
	
	RMB.prototype.id = function() {
		return "/"+this._id;
	};
	
	var Child = function(parent, id) {
		Trap.EventObject.prototype.constructor.call(this);
		this.parent = parent;
		this._id = id;
		this.ondispatch = dispatchFun;
		this.children = {};
	}

	Child.prototype = new RMB;
	Child.prototype.constructor = Child;
	
	Child.prototype.id = function() {
		return this.parent.id() + "/" + this._id;
	};
	
	Child.prototype.request = function(src) {
		return this.parent.request(src || this);
	}
	
	Child.prototype.message = function(src) {
		return this.parent.message(src || this);
	}
	
	
	var builder = function() {
		var seedArg, idArg = Trap._uuid();
		return {
			seed: function(src) { seedArg = src; return this; },
			id: function(src) { idArg = src; return this; },
			build: function() {
				return new RMB({seed: seedArg, id: idArg});
			}
		};
	};
	
	target.RMB = {
			builder: builder,
			Message: Message
	};
	
})(self);
