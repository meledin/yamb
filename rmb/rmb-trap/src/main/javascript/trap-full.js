(function(t){var Trap = {};

// Array of classes for Trap Transports

/**
 * Array of classes for Trap transports.
 * @namespace
 */
Trap.Transports = {};

/**
 * Enables/disables binary support
 * @property
 */
Trap.supportsBinary = typeof(Uint8Array) != "undefined";

/**
 * @namespace
 */
Trap.Constants = {};
Trap.Constants.OPTION_MAX_CHUNK_SIZE = "trap.maxchunksize";
Trap.Constants.OPTION_ENABLE_COMPRESSION = "trap.enablecompression";
Trap.Constants.OPTION_AUTO_HOSTNAME = "trap.auto_hostname";
Trap.Constants.CONNECTION_TOKEN = "trap.connection-token";
Trap.Constants.TRANSPORT_ENABLED_DEFAULT = true;
Trap.Constants.ENDPOINT_ID = "trap.endpoint-id";
Trap.Constants.ENDPOINT_ID_UNDEFINED = "UNDEFINED";
Trap.Constants.ENDPOINT_ID_CLIENT = "NEW";
//< needs(trap)

Trap.ByteArrayOutputStream = function(initialLength) {
	// Set an initial length of the array, unless otherwise specified
	if (typeof (initialLength) != "number" || initialLength < 0)
		initialLength = 512;

	this.buf = new Uint8Array(initialLength);
	this.off = 0;
	this.size = initialLength;
	this.growthSize = 512;
};

Trap.ByteArrayOutputStream.prototype._remaining = function() {
	return this.size - this.off;
};

Trap.ByteArrayOutputStream.prototype._resize = function(newSize) {
	var copySize = Math.min(newSize, this.off);
	var newBuf = new Uint8Array(newSize);

	var src = (copySize < this.buf.length ? this.buf.subarray(0, copySize)
			: this.buf);
	newBuf.set(src, 0);
	this.buf = newBuf;
	this.size = this.buf.length;
};

Trap.ByteArrayOutputStream.prototype._checkAndResize = function(neededBytes) {
	if (this._remaining() < neededBytes)
		this._resize(this.size + Math.max(neededBytes, this.growthSize));
};

Trap.ByteArrayOutputStream.prototype.write = function(src, off, len) {
	if (typeof (src) == "number") {
		this._checkAndResize(1);
		this.buf[this.off++] = src;
		return;
	}

	if (typeof (off) != "number")
		off = 0;

	if (typeof (len) != "number")
		len = (src.byteLength ? src.byteLength : src.length);

	if (typeof (src) == "string") {

		var result = src.toUTF8ByteArray();
		this._checkAndResize(result.length);
		for ( var i = 0; i < result.length; i++)
			this.buf[this.off++] = result[i];
		return;
	}

	this._checkAndResize(len - off);

	if (typeof (src.length) == "number" && src.slice) {
		for ( var i = off; i < off + len; i++)
			this.buf[this.off++] = src[i];

		return;
	}

	if (typeof (src.byteLength) == "number") {

		if (src.byteLength == 0)
			return;
		
		if (src.byteOffset > 0)
			off += src.byteOffset;

		var buf = (src.buffer ? src.buffer : src);
		var view = new Uint8Array(buf, off, len);
		this.buf.set(view, this.off);
		this.off += len;
		return;
	}

	throw "Cannot serialise: " + typeof (src);
};

Trap.ByteArrayOutputStream.prototype.toString = function() {
	var str = "";
	for ( var i = 0; i < this.buf.length; i++)
		str += this.buf[i];
	return String.utf8Decode(str);
};

Trap.ByteArrayOutputStream.prototype.toArray = function() {
	return new Uint8Array(this.buf.buffer.slice(0, this.off));
};

Trap.ByteArrayOutputStream.prototype.clear = function() {
	this.buf = new Uint8Array(512);
	this.off = 0;
	this.size = 512;
};

Trap.ByteStringOutputStream = function() {
	this.buf = "";
};

// Append the contents of the write operation in compact mode.
// Assume the input is byte-equivalent
Trap.ByteStringOutputStream.prototype.write = function(str, off, len) {

	if (typeof (str) == "number") {
		this.buf += String.fromCharCode(str);
		return;
	}

	if (typeof (off) != "number")
		off = 0;

	if (typeof (len) != "number")
		len = str.length;

	if (typeof (str) == "string")
		this.buf += str.substr(0, len);
	else if (typeof (str.length) == "number" && str.slice)
		for ( var i = off; i < off + len; i++)
			this.buf += String.fromCharCode(str[i]);
	else
		throw "Cannot serialise: " + typeof (str);
};

Trap.ByteStringOutputStream.prototype.toString = function() {
	return this.buf;
};

Trap.ByteStringOutputStream.prototype.toArray = function() {
	var arr = [];
	for ( var i = 0; i < this.buf.length; i++)
		arr[i] = this.buf[i].charCodeAt(0);
	return arr;
};

Trap.ByteStringOutputStream.prototype.clear = function() {
	this.buf = "";
};


Trap._compat = {};

Trap._compat.capitalise = function(str)
{
	return str.substr(0,1).toUpperCase() + str.substr(1);
};

Trap._compat.__defineSetter = function(object, setterName, cb)
{
	
	var newName = "set" + Trap._compat.capitalise(setterName);
	
	if (!cb)
	{
		var privateName = "_" + setterName;
		cb = function(val) {
			this[privateName] = val;
			return this;
		};
	}
	
	if (object.__defineSetter__)
	{
		try
		{
			object.__defineSetter__(setterName, cb);
		} catch(e){}
	}

	// Also create the getter function as a property of the object
	object[newName] = cb;
};

Trap._compat.__defineGetter = function(object, getterName, cb)
{
	
	var newName = "get" + Trap._compat.capitalise(getterName);
	
	if (!cb)
	{
		var privateName = "_" + getterName;
		
		cb = function() {
			return this[privateName];
		};
	}
	
	if (object.__defineGetter__)
	{
		try
		{
			object.__defineGetter__(getterName, cb);
		} catch(e){}
	}
	
	// Also create the getter function as a property of the object
	object[newName] = cb;
};

Trap._compat.__defineGetterSetter = function(object, publicName, privateName, getter, setter)
{
	if (!privateName)
		privateName = "_" + publicName;
	
	if (!getter)
	{
		getter = function() {
			return this[privateName];
		};
	}
	
	if (!setter)
	{
		setter = function(val) {
			this[privateName] = val;
			return this;
		};
	}

	Trap._compat.__defineSetter(object, publicName, setter);
	Trap._compat.__defineGetter(object, publicName, getter);
};

Trap._compat.__addEventListener = function(object, event, listener)
{
	
	function ie() { object.attachEvent("on"+event, listener); }
	
	if (object.addEventListener)
		try
		{
			object.addEventListener(event, listener, false);
		} catch(e) { ie(); } // Yes, Internet Explorer supports AddEventListener... YET STILL THROWS. What's the logic? Really?
	else if (object.attachEvent)
		ie();
	else
		throw "Could not add listener for " + event + " to object " + object;
};

Trap._uuidCounter = 0;
Trap._uuid = function()
{
	return Math.random().toString(16).substring(2) + (Trap._uuidCounter++).toString(16);
};

// Choosing not to define a common function, in case someone wants to feature detect object type
Trap.subarray = function(src, start, end)
{
	if (src.subarray)
		return src.subarray(start,end);
	else
		return src.slice(start,end);
};

// Some ArrayBuffers don't have slice
if (typeof(ArrayBuffer) != "undefined" && typeof(ArrayBuffer.prototype.slice) == "undefined") {
	ArrayBuffer.prototype.slice = function(begin,end) {
		if (typeof(begin) == "undefined") return new ArrayBuffer(0);
		if (begin < 0) begin = this.byteLength + begin;
		if (begin < 0) begin = 0;
		if (begin > this.byteLength-1) begin = this.byteLength-1;
		if (typeof(end) != "undefined") {
			if (end < 0) end = this.byteLength + end;
			if (end < 0) end = 0;
			if (end > this.byteLength) end = this.byteLength;
		} else {
			end = this.byteLength;
		}
		if (end-begin <= 0) return new ArrayBuffer(0);
		var src = new Uint8Array(this,begin,end-begin);
		var dst = new Uint8Array(end-begin);
		dst.set(src);
		return dst.buffer;		
	};
}

// Flag detects if the browser supports getters (optimises access)
Trap._useGetters = false;
try { eval('var f = {get test() { return true; }}; Trap._useGetters = f.test;'); } catch(e){}

Trap.ByteConverter = {};

Trap.ByteConverter.toBigEndian = function(i, arr, j)
{
	
	if (!arr)
		arr = [];

	if (!j)
		j = 0;
	
	arr[j + 0] = (i >> 24);
	arr[j + 1] = ((i >> 16) & 0xFF);
	arr[j + 2] = ((i >> 8) & 0xFF);
	arr[j + 3] = ((i >> 0) & 0xFF);
	return arr;
};

Trap.ByteConverter.fromBigEndian = function(arr, offset)
{
	var rv = 0;

	rv |= (arr[offset + 0] & 0xFF) << 24;
	rv |= ((arr[offset + 1] & 0xFF) << 16);
	rv |= ((arr[offset + 2] & 0xFF) << 8);
	rv |= ((arr[offset + 3] & 0xFF) << 0);

	return rv;
};

/**
 * Converts an integer to a 7-bit representation of an integer by only
 * taking the 28 lowest-order values.
 *
 * @param src
 * @return
 */
Trap.ByteConverter.toBigEndian7 = function(src)
{

	var rv = [];

	rv[0] = this.getBits(src, 4, 11);
	rv[1] = this.getBits(src, 11, 18);
	rv[2] = this.getBits(src, 18, 25);
	rv[3] = this.getBits(src, 25, 32);

	return rv;
};

Trap.ByteConverter.fromBigEndian7 = function(arr, offset)
{
	
	if (!offset)
		offset = 0;
	
	var rv = 0;

	rv |= (arr[offset + 0] & 0xFF) << 21;
	rv |= ((arr[offset + 1] & 0xFF) << 14);
	rv |= ((arr[offset + 2] & 0xFF) << 7);
	rv |= ((arr[offset + 3] & 0xFF) << 0);

	if (this.getBits(rv, 4, 5) == 1)
		rv |= 0xF0000000;

	return rv;
};

/**
 * Fetch a number of bits from an integer.
 *
 * @param src
 *            The source to take from
 * @param startBit
 *            The first bit, with starting index 0
 * @param endBit
 *            The end index. This index will NOT be included in the return
 *            value.
 * @return The endBit-startBit number of bits from index startBit will be in
 *         the lowest order bits of the returned value.
 */
Trap.ByteConverter.getBits = function(src, startBit, endBit)
{
	var mask = (Math.pow(2, endBit - startBit) - 1);
	mask = mask << (32 - endBit);
	var rv = (src & mask) >> (32 - endBit);
	return rv;
};
//< needs(trap)

//> public void fn()
Trap.EventObject = function()
{
	this._eventlistenersMap = {};
};

// (void) addEventListener(String, Function)
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

//(void) removeEventListener(String, Function)
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
    
    if(!!listeners)
    {
    	for (var i = 0; i < listeners.length; i++)
    	{
    		try
    		{
        		listeners[i](evt);
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
	    if (f && typeof(f) == "function") f.call(this, evt);
	}
	catch (e)
	{
		if (this.logger)
		{
			this.logger.warn("Exception while dispatching event to listener; ", e, " to ", f, ". Event was ", evt);
		}
	}
};
//<needs(trap)

//> public void fn()
Trap.List = function()
{
	this.list = [];
};

Trap.List.prototype.className = "list";

Trap.List.prototype.add = function(a, b)
{
	
	// add(index, object)
	if (!!b && typeof(a) == "number")
	{
		this.list.splice(a, 0, b);
	}
	else
	{
		// add(object)
		this.list.push(a);
	}
};

Trap.List.prototype.addLast = function(o)
{
	this.list.push(o);
};

Trap.List.prototype.pop = function()
{
	return this.remove(0);
};

Trap.List.prototype.remove = function(o)
{
	
	if (!o)
		o = 0;
	
	if (typeof o == "number")
	{
		var orig = this.list[o];
		this.list.splice(o, 1);
		return orig;
	}
	
	for (var i=0; i<this.list.length; i++)
		if (this.list[i] == o)
			this.list.splice(i, 1);
	
	return o;
};

Trap.List.prototype.peek = function()
{
	return (this.list.length > 0 ? this.list[0] : null);
};

Trap.List.prototype.size = function()
{
	return this.list.length;
};

Trap.List.prototype.isEmpty = function()
{
	return this.size() == 0;
};

Trap.List.prototype.get = function(idx)
{
	return this.list[idx];
};

Trap.List.prototype.getLast = function()
{
	return this.get(this.size()-1);
};

Trap.List.prototype.contains = function(needle)
{
	for (var i=0; i<this.list.length; i++)
		if (this.list[i] == needle)
			return true;
	return false;
};

Trap.List.prototype.sort = function()
{
	this.list.sort.apply(this.list, arguments);
};

Trap.List.prototype.clear = function()
{
	this.list = [];
};

Trap.List.prototype.addAll = function()
{
	var args = arguments;
	
	if (args.length < 1)
		return;
	
	if (args.length == 1)
	{
		var o = args[0];
		
		if (o.className == "list")
			args = o.list;
		else if (typeof(o) == "array" || (o.length && o.map))
			args = o;
	}

	// Add all elements
	for (var i=0; i<args.length; i++)
		this.addLast(args[i]);
	
};

Trap.List.prototype.iterator = function()
{
	var list = this;
	var idx = -1;
	return {
		hasNext: function() { return !!list.get(idx+1); },
		next: function() { return list.get(++idx); },
		remove: function() { list.remove(idx); idx; }
	};
};
//<needs(trap)

Trap.Logger = function(name)
{
	this.name = name;
};

Trap.Logger._loggers = {};

Trap.Logger.getLogger = function(name)
{
	var logger = Trap.Logger._loggers[name];
	
	if (!logger)
	{
		logger = new Trap.Logger(name);
		Trap.Logger._loggers[name] = logger;
	}
	
	return logger;
};

// TODO: Proper formatter plx
Trap.Logger.formatter = {};

{
	var _pad = function (val, len) {
		val = String(val);
		len = len || 2;
		while (val.length < len)
			val = "0" + val;
		return val;
	};

	var _logtime = function () {
		var d = new Date();
	    return [_pad(d.getHours(), 2)+":"+_pad(d.getMinutes(), 2)+":"+_pad(d.getSeconds(), 2)+"."+_pad(d.getMilliseconds(), 3)+" - "];
	};
	
	Trap.Logger.formatter._format = function(logMessage)
	{

		var params = _logtime();
		params.push(logMessage.label);
		
		if (logMessage.objects.length > 1 && typeof(logMessage.objects[0]) == "string")
		{
			// Slam the objects as needed.
			var msg = logMessage.objects[0];
			var idx = msg.indexOf("{}");
			var i=1;

			while (idx != -1)
			{
				if (i >= logMessage.objects.length)
					break;
				
				// Replaces first instance.
				msg = msg.replace("{}", logMessage.objects[i]);
				i++;
				
				// Technically, we can do it differently, but this way we'll prevent searching the parts of the string we processed
				idx = msg.indexOf("{}", idx);
			}
			
			params.push(msg);
			
			while (i < logMessage.objects.length)
			{
				var o = logMessage.objects[i++];
				
				params.push(o);
				
				if (o.stack)
					params.push(o.stack);
			}
			
		}
		else
			params.push.apply(params, logMessage.objects);
		
		if (logMessage.objects[0].stack)
			params.push(logMessage.objects[0].stack);
		
		return params;
	};
}

// TODO: Proper appender plx
Trap.Logger.appender = {};
Trap.Logger.appender._info = Trap.Logger.appender._warn = Trap.Logger.appender._error = function(){};

if (self.console && self.console.log) {
    if (self.console.log.apply)
    	Trap.Logger.appender._info = function(params) { self.console.log.apply(self.console, params); };
    else
    	Trap.Logger.appender._info = function(params) { self.console.log(params.join("")); };
    	
    if (self.console.warn) {
	    if (self.console.warn.apply)
	    	Trap.Logger.appender._warn = function(params) { self.console.warn.apply(self.console, params); };
	    else
	    	Trap.Logger.appender._warn = function(params) { self.console.warn(params.join("")); };
    } 
    else
    	Trap.Logger.appender._warn = Trap.Logger.appender._info;
    
    if (self.console.error) {
	    if (self.console.error.apply)
	    	Trap.Logger.appender._error = function(params) { self.console.error.apply(self.console, params); };
	    else
	    	Trap.Logger.appender._error = function(params) { self.console.error(params.join("")); };
    } 
    else
    	Trap.Logger.appender._error = Trap.Logger.appender._info;
}

Trap.Logger.prototype.isTraceEnabled = function() {
	return true;
};

Trap.Logger.prototype.trace = function()
{
	Trap.Logger.appender._info(Trap.Logger.formatter._format({objects: arguments, label: "", logger: this.name}));
};

Trap.Logger.prototype.debug = function()
{
	Trap.Logger.appender._info(Trap.Logger.formatter._format({objects: arguments, label: "", logger: this.name}));
};

Trap.Logger.prototype.info = function()
{
	Trap.Logger.appender._info(Trap.Logger.formatter._format({objects: arguments, label: "", logger: this.name}));
};

Trap.Logger.prototype.warn = function()
{
	Trap.Logger.appender._warn(Trap.Logger.formatter._format({objects: arguments, label: "WARN: ", logger: this.name}));
};

Trap.Logger.prototype.error = function()
{
	Trap.Logger.appender._error(Trap.Logger.formatter._format({objects: arguments, label: "ERROR: ", logger: this.name}));
};
//<needs(trap)
/*
 * The Map class provides an implementation-agnostic way to have a map with any
 * keys/values but without the hassle of being affected during iterations by
 * third party libraries, since you can ask for all keys.
 */

Trap.Map = function(src)
{
	this._map = {};
	this._keys = [];
	
	if (typeof(src) != "undefined")
	{
		// Clone
		for (var key in src.allKeys())
			this.put(key, src.get(key));
	}
};

Trap.Map.prototype.put = function(key, value)
{
	if (!(key in this._map))
		this._keys.push(key);
	
	this._map[key] = value;
};

Trap.Map.prototype.get = function(key)
{
	return this._map[key];
};

Trap.Map.prototype.allKeys = function()
{
	return this._keys;
};

Trap.Map.prototype.containsKey = function(key)
{
	return typeof(this._map[key]) != "undefined";
};

Trap.Map.prototype.remove = function(key)
{
	for (var i=0; i<this._keys.length; i++)
		if (this._keys[i] == key)
			this._keys.splice(i, 1);
	
	delete this._map[key];
};

Trap.Map.prototype.size = function()
{
	return this._keys.length;
};

Trap.Map.prototype.putAll = function(src)
{
	var keys = src.allKeys();
	
	for (var i=0; i<keys.length; i++)
		this.put(keys[i], src.get(keys[i]));
};
//<needs(trap)
Trap.MD5 = function (string) {
 
	function RotateLeft(lValue, iShiftBits) {
		return (lValue<<iShiftBits) | (lValue>>>(32-iShiftBits));
	}
 
	function AddUnsigned(lX,lY) {
		var lX4,lY4,lX8,lY8,lResult;
		lX8 = (lX & 0x80000000);
		lY8 = (lY & 0x80000000);
		lX4 = (lX & 0x40000000);
		lY4 = (lY & 0x40000000);
		lResult = (lX & 0x3FFFFFFF)+(lY & 0x3FFFFFFF);
		if (lX4 & lY4) {
			return (lResult ^ 0x80000000 ^ lX8 ^ lY8);
		}
		if (lX4 | lY4) {
			if (lResult & 0x40000000) {
				return (lResult ^ 0xC0000000 ^ lX8 ^ lY8);
			} else {
				return (lResult ^ 0x40000000 ^ lX8 ^ lY8);
			}
		} else {
			return (lResult ^ lX8 ^ lY8);
		}
 	}
 
 	function F(x,y,z) { return (x & y) | ((~x) & z); }
 	function G(x,y,z) { return (x & z) | (y & (~z)); }
 	function H(x,y,z) { return (x ^ y ^ z); }
	function I(x,y,z) { return (y ^ (x | (~z))); }
 
	function FF(a,b,c,d,x,s,ac) {
		a = AddUnsigned(a, AddUnsigned(AddUnsigned(F(b, c, d), x), ac));
		return AddUnsigned(RotateLeft(a, s), b);
	};
 
	function GG(a,b,c,d,x,s,ac) {
		a = AddUnsigned(a, AddUnsigned(AddUnsigned(G(b, c, d), x), ac));
		return AddUnsigned(RotateLeft(a, s), b);
	};
 
	function HH(a,b,c,d,x,s,ac) {
		a = AddUnsigned(a, AddUnsigned(AddUnsigned(H(b, c, d), x), ac));
		return AddUnsigned(RotateLeft(a, s), b);
	};
 
	function II(a,b,c,d,x,s,ac) {
		a = AddUnsigned(a, AddUnsigned(AddUnsigned(I(b, c, d), x), ac));
		return AddUnsigned(RotateLeft(a, s), b);
	};
 
	function ConvertToWordArray(string) {
		var lWordCount;
		var lMessageLength = string.length;
		var lNumberOfWords_temp1=lMessageLength + 8;
		var lNumberOfWords_temp2=(lNumberOfWords_temp1-(lNumberOfWords_temp1 % 64))/64;
		var lNumberOfWords = (lNumberOfWords_temp2+1)*16;
		var lWordArray=Array(lNumberOfWords-1);
		var lBytePosition = 0;
		var lByteCount = 0;
		while ( lByteCount < lMessageLength ) {
			lWordCount = (lByteCount-(lByteCount % 4))/4;
			lBytePosition = (lByteCount % 4)*8;
			lWordArray[lWordCount] = (lWordArray[lWordCount] | (string.charCodeAt(lByteCount)<<lBytePosition));
			lByteCount++;
		}
		lWordCount = (lByteCount-(lByteCount % 4))/4;
		lBytePosition = (lByteCount % 4)*8;
		lWordArray[lWordCount] = lWordArray[lWordCount] | (0x80<<lBytePosition);
		lWordArray[lNumberOfWords-2] = lMessageLength<<3;
		lWordArray[lNumberOfWords-1] = lMessageLength>>>29;
		return lWordArray;
	};
 
	function WordToHex(lValue) {
		var WordToHexValue="",WordToHexValue_temp="",lByte,lCount;
		for (lCount = 0;lCount<=3;lCount++) {
			lByte = (lValue>>>(lCount*8)) & 255;
			WordToHexValue_temp = "0" + lByte.toString(16);
			WordToHexValue = WordToHexValue + WordToHexValue_temp.substr(WordToHexValue_temp.length-2,2);
		}
		return WordToHexValue;
	};
 
	function Utf8Encode(string) {
		string = string.replace(/\r\n/g,"\n");
		var utftext = "";
 
		for (var n = 0; n < string.length; n++) {
 
			var c = string.charCodeAt(n);
 
			if (c < 128) {
				utftext += String.fromCharCode(c);
			}
			else if((c > 127) && (c < 2048)) {
				utftext += String.fromCharCode((c >> 6) | 192);
				utftext += String.fromCharCode((c & 63) | 128);
			}
			else {
				utftext += String.fromCharCode((c >> 12) | 224);
				utftext += String.fromCharCode(((c >> 6) & 63) | 128);
				utftext += String.fromCharCode((c & 63) | 128);
			}
 
		}
 
		return utftext;
	};
 
	var x=Array();
	var k,AA,BB,CC,DD,a,b,c,d;
	var S11=7, S12=12, S13=17, S14=22;
	var S21=5, S22=9 , S23=14, S24=20;
	var S31=4, S32=11, S33=16, S34=23;
	var S41=6, S42=10, S43=15, S44=21;
 
	string = Utf8Encode(string);
 
	x = ConvertToWordArray(string);
 
	a = 0x67452301; b = 0xEFCDAB89; c = 0x98BADCFE; d = 0x10325476;
 
	for (k=0;k<x.length;k+=16) {
		AA=a; BB=b; CC=c; DD=d;
		a=FF(a,b,c,d,x[k+0], S11,0xD76AA478);
		d=FF(d,a,b,c,x[k+1], S12,0xE8C7B756);
		c=FF(c,d,a,b,x[k+2], S13,0x242070DB);
		b=FF(b,c,d,a,x[k+3], S14,0xC1BDCEEE);
		a=FF(a,b,c,d,x[k+4], S11,0xF57C0FAF);
		d=FF(d,a,b,c,x[k+5], S12,0x4787C62A);
		c=FF(c,d,a,b,x[k+6], S13,0xA8304613);
		b=FF(b,c,d,a,x[k+7], S14,0xFD469501);
		a=FF(a,b,c,d,x[k+8], S11,0x698098D8);
		d=FF(d,a,b,c,x[k+9], S12,0x8B44F7AF);
		c=FF(c,d,a,b,x[k+10],S13,0xFFFF5BB1);
		b=FF(b,c,d,a,x[k+11],S14,0x895CD7BE);
		a=FF(a,b,c,d,x[k+12],S11,0x6B901122);
		d=FF(d,a,b,c,x[k+13],S12,0xFD987193);
		c=FF(c,d,a,b,x[k+14],S13,0xA679438E);
		b=FF(b,c,d,a,x[k+15],S14,0x49B40821);
		a=GG(a,b,c,d,x[k+1], S21,0xF61E2562);
		d=GG(d,a,b,c,x[k+6], S22,0xC040B340);
		c=GG(c,d,a,b,x[k+11],S23,0x265E5A51);
		b=GG(b,c,d,a,x[k+0], S24,0xE9B6C7AA);
		a=GG(a,b,c,d,x[k+5], S21,0xD62F105D);
		d=GG(d,a,b,c,x[k+10],S22,0x2441453);
		c=GG(c,d,a,b,x[k+15],S23,0xD8A1E681);
		b=GG(b,c,d,a,x[k+4], S24,0xE7D3FBC8);
		a=GG(a,b,c,d,x[k+9], S21,0x21E1CDE6);
		d=GG(d,a,b,c,x[k+14],S22,0xC33707D6);
		c=GG(c,d,a,b,x[k+3], S23,0xF4D50D87);
		b=GG(b,c,d,a,x[k+8], S24,0x455A14ED);
		a=GG(a,b,c,d,x[k+13],S21,0xA9E3E905);
		d=GG(d,a,b,c,x[k+2], S22,0xFCEFA3F8);
		c=GG(c,d,a,b,x[k+7], S23,0x676F02D9);
		b=GG(b,c,d,a,x[k+12],S24,0x8D2A4C8A);
		a=HH(a,b,c,d,x[k+5], S31,0xFFFA3942);
		d=HH(d,a,b,c,x[k+8], S32,0x8771F681);
		c=HH(c,d,a,b,x[k+11],S33,0x6D9D6122);
		b=HH(b,c,d,a,x[k+14],S34,0xFDE5380C);
		a=HH(a,b,c,d,x[k+1], S31,0xA4BEEA44);
		d=HH(d,a,b,c,x[k+4], S32,0x4BDECFA9);
		c=HH(c,d,a,b,x[k+7], S33,0xF6BB4B60);
		b=HH(b,c,d,a,x[k+10],S34,0xBEBFBC70);
		a=HH(a,b,c,d,x[k+13],S31,0x289B7EC6);
		d=HH(d,a,b,c,x[k+0], S32,0xEAA127FA);
		c=HH(c,d,a,b,x[k+3], S33,0xD4EF3085);
		b=HH(b,c,d,a,x[k+6], S34,0x4881D05);
		a=HH(a,b,c,d,x[k+9], S31,0xD9D4D039);
		d=HH(d,a,b,c,x[k+12],S32,0xE6DB99E5);
		c=HH(c,d,a,b,x[k+15],S33,0x1FA27CF8);
		b=HH(b,c,d,a,x[k+2], S34,0xC4AC5665);
		a=II(a,b,c,d,x[k+0], S41,0xF4292244);
		d=II(d,a,b,c,x[k+7], S42,0x432AFF97);
		c=II(c,d,a,b,x[k+14],S43,0xAB9423A7);
		b=II(b,c,d,a,x[k+5], S44,0xFC93A039);
		a=II(a,b,c,d,x[k+12],S41,0x655B59C3);
		d=II(d,a,b,c,x[k+3], S42,0x8F0CCC92);
		c=II(c,d,a,b,x[k+10],S43,0xFFEFF47D);
		b=II(b,c,d,a,x[k+1], S44,0x85845DD1);
		a=II(a,b,c,d,x[k+8], S41,0x6FA87E4F);
		d=II(d,a,b,c,x[k+15],S42,0xFE2CE6E0);
		c=II(c,d,a,b,x[k+6], S43,0xA3014314);
		b=II(b,c,d,a,x[k+13],S44,0x4E0811A1);
		a=II(a,b,c,d,x[k+4], S41,0xF7537E82);
		d=II(d,a,b,c,x[k+11],S42,0xBD3AF235);
		c=II(c,d,a,b,x[k+2], S43,0x2AD7D2BB);
		b=II(b,c,d,a,x[k+9], S44,0xEB86D391);
		a=AddUnsigned(a,AA);
		b=AddUnsigned(b,BB);
		c=AddUnsigned(c,CC);
		d=AddUnsigned(d,DD);
	}
 
	var temp = WordToHex(a)+WordToHex(b)+WordToHex(c)+WordToHex(d);
 
	return temp.toLowerCase();
};


//<needs(trap)
//<needs(list)

Trap.Set = function(){
	Trap.List.prototype.constructor.call(this);
};
Trap.Set.prototype = new Trap.List;

Trap.Set.prototype.className = "set";

Trap.Set.prototype.add = function(a, b)
{
	
	var key = a;
	
	if (!!b && typeof(a) == "number")
		key = b;
	
	if (!this.contains(key))
		Trap.List.prototype.add.call(this, a, b);
};
/**
Workaround for iOS 6 setTimeout bug using requestAnimationFrame to simulate timers during Touch/Gesture-based events
Author: Jack Pattishall (jpattishall@gmail.com)
This code is free to use anywhere (MIT, etc.)
 
Usage: Pass TRUE as the final argument for setTimeout or setInterval.
 
Ex:
setTimeout(func, 1000) // uses native code
setTimeout(func, 1000, true) // uses workaround
 
Demos:
http://jsfiddle.net/xKh5m/ - uses native setTimeout
http://jsfiddle.net/ujxE3/ - uses workaround timers
*/
 
(function(){
  // Only apply settimeout workaround for iOS 6 - for all others, we map to native Timers
  if (!navigator || !navigator.userAgent.match(/OS 6(_\d)+/i)) return;
  
  // Abort if we're running in a worker. Let's hope workers aren't paused during scrolling!!!
  if (typeof(window) == "undefined")
	  return;
 
  (function (window) {
	  
      // This library re-implements setTimeout, setInterval, clearTimeout, clearInterval for iOS6.
      // iOS6 suffers from a bug that kills timers that are created while a page is scrolling.
      // This library fixes that problem by recreating timers after scrolling finishes (with interval correction).
	// This code is free to use by anyone (MIT, blabla).
	// Author: rkorving@wizcorp.jp

      var timeouts = {};
      var intervals = {};
      var orgSetTimeout = window.setTimeout;
      var orgSetInterval = window.setInterval;
      var orgClearTimeout = window.clearTimeout;
      var orgClearInterval = window.clearInterval;


      function createTimer(set, map, args) {
              var id, cb = args[0], repeat = (set === orgSetInterval);

              function callback() {
                      if (cb) {
                              cb.apply(window, arguments);

                              if (!repeat) {
                                      delete map[id];
                                      cb = null;
                              }
                      }
              }

              args[0] = callback;

              id = set.apply(window, args);

              map[id] = { args: args, created: Date.now(), cb: cb, id: id };

              return id;
      }


      function resetTimer(set, clear, map, virtualId, correctInterval) {
              var timer = map[virtualId];

              if (!timer) {
                      return;
              }

              var repeat = (set === orgSetInterval);

              // cleanup

              clear(timer.id);

              // reduce the interval (arg 1 in the args array)

              if (!repeat) {
                      var interval = timer.args[1];

                      var reduction = Date.now() - timer.created;
                      if (reduction < 0) {
                              reduction = 0;
                      }

                      interval -= reduction;
                      if (interval < 0) {
                              interval = 0;
                      }

                      timer.args[1] = interval;
              }

              // recreate

              function callback() {
                      if (timer.cb) {
                              timer.cb.apply(window, arguments);
                              if (!repeat) {
                                      delete map[virtualId];
                                      timer.cb = null;
                              }
                      }
              }

              timer.args[0] = callback;
              timer.created = Date.now();
              timer.id = set.apply(window, timer.args);
      }


      window.setTimeout = function () {
              return createTimer(orgSetTimeout, timeouts, arguments);
      };


      window.setInterval = function () {
              return createTimer(orgSetInterval, intervals, arguments);
      };

      window.clearTimeout = function (id) {
              var timer = timeouts[id];

              if (timer) {
                      delete timeouts[id];
                      orgClearTimeout(timer.id);
              }
      };

      window.clearInterval = function (id) {
              var timer = intervals[id];

              if (timer) {
                      delete intervals[id];
                      orgClearInterval(timer.id);
              }
      };

      window.addEventListener('scroll', function () {
              // recreate the timers using adjusted intervals
              // we cannot know how long the scroll-freeze lasted, so we cannot take that into account

              var virtualId;

              for (virtualId in timeouts) {
                      resetTimer(orgSetTimeout, orgClearTimeout, timeouts, virtualId);
              }

              for (virtualId in intervals) {
                      resetTimer(orgSetInterval, orgClearInterval, intervals, virtualId);
              }
      });

}(window));
})();
/*
 * Adds more string functions
 */

if (typeof String.prototype.startsWith != 'function') {
  String.prototype.startsWith = function (str){
    return this.indexOf(str) == 0;
  };
}

if (typeof String.prototype.trim != 'function')
{
	String.prototype.trim = function(str)
	{
		var	s = str.replace(/^\s\s*/, ''),
			ws = /\s/,
			i = s.length;
		while (ws.test(s.charAt(--i)));
		return s.slice(0, i + 1);
		
	};
}

if (typeof String.prototype.endsWith != 'function') {
	String.prototype.endsWith = function(suffix) {
	    return this.indexOf(suffix, this.length - suffix.length) !== -1;
	};
}

if (typeof String.prototype.contains != 'function')
{
	String.prototype.contains = function(target)
	{
		return this.indexOf(target) != -1;
	};
};

// Calculates the length of the string in utf-8
if (typeof String.prototype.utf8ByteLength != 'function')
{
	String.prototype.utf8ByteLength = function()
	{
		// Matches only the 10.. bytes that are non-initial characters in a multi-byte sequence.
		var m = encodeURIComponent(this).match(/%[89ABab]/g);
		return this.length + (m ? m.length : 0);
	};
};

if (typeof String.prototype.toUTF8ByteArray != 'function')
String.prototype.toUTF8ByteArray = function() {
	var bytes = [];

	var s = unescape(encodeURIComponent(this));

	for (var i = 0; i < s.length; i++) {
		var c = s.charCodeAt(i);
		bytes.push(c);
	}

	return bytes;
};

String.utf8Encode = function(src)
{
	return unescape( encodeURIComponent( src ) );
};

String.utf8Decode = function(src)
{
	return decodeURIComponent( escape( src ) );
};

if (typeof String.prototype.fromUTF8ByteArray != 'function')
String.fromUTF8ByteArray = function(arr, offset, length)
{
	var str = "";
	if (typeof(offset) == "undefined")
	{
		offset = 0; length = arr.length;
	}
	
	for (var i=offset; i<length+offset; i++)
		str += String.fromCharCode(arr[i]);
	
	return String.utf8Decode(str);
};
//<needs(trap)

//> void fn()
Trap.StringBuffer = function()
{
	this.buf = "";
};

//> void fn(String)
Trap.StringBuffer.prototype.append = function(arg)
{
	this.buf += arg;
};

//> String fn()
Trap.StringBuffer.prototype.toString = function()
{
	return this.buf;
};

Trap.StringBuilder = Trap.StringBuffer;
/** @license zlib.js 2012 - imaya [ https://github.com/imaya/zlib.js ] The MIT License */(function() {'use strict';function m(b){throw b;}var n=void 0,r=this;function s(b,d){var a=b.split("."),c=r;!(a[0]in c)&&c.execScript&&c.execScript("var "+a[0]);for(var f;a.length&&(f=a.shift());)!a.length&&d!==n?c[f]=d:c=c[f]?c[f]:c[f]={}};var u="undefined"!==typeof Uint8Array&&"undefined"!==typeof Uint16Array&&"undefined"!==typeof Uint32Array;function v(b){var d=b.length,a=0,c=Number.POSITIVE_INFINITY,f,e,g,h,k,l,q,p,t;for(p=0;p<d;++p)b[p]>a&&(a=b[p]),b[p]<c&&(c=b[p]);f=1<<a;e=new (u?Uint32Array:Array)(f);g=1;h=0;for(k=2;g<=a;){for(p=0;p<d;++p)if(b[p]===g){l=0;q=h;for(t=0;t<g;++t)l=l<<1|q&1,q>>=1;for(t=l;t<f;t+=k)e[t]=g<<16|p;++h}++g;h<<=1;k<<=1}return[e,a,c]};function w(b,d){this.g=[];this.h=32768;this.d=this.f=this.a=this.l=0;this.input=u?new Uint8Array(b):b;this.m=!1;this.i=x;this.r=!1;if(d||!(d={}))d.index&&(this.a=d.index),d.bufferSize&&(this.h=d.bufferSize),d.bufferType&&(this.i=d.bufferType),d.resize&&(this.r=d.resize);switch(this.i){case y:this.b=32768;this.c=new (u?Uint8Array:Array)(32768+this.h+258);break;case x:this.b=0;this.c=new (u?Uint8Array:Array)(this.h);this.e=this.z;this.n=this.v;this.j=this.w;break;default:m(Error("invalid inflate mode"))}}
var y=0,x=1,z={t:y,s:x};
w.prototype.k=function(){for(;!this.m;){var b=A(this,3);b&1&&(this.m=!0);b>>>=1;switch(b){case 0:var d=this.input,a=this.a,c=this.c,f=this.b,e=n,g=n,h=n,k=c.length,l=n;this.d=this.f=0;e=d[a++];e===n&&m(Error("invalid uncompressed block header: LEN (first byte)"));g=e;e=d[a++];e===n&&m(Error("invalid uncompressed block header: LEN (second byte)"));g|=e<<8;e=d[a++];e===n&&m(Error("invalid uncompressed block header: NLEN (first byte)"));h=e;e=d[a++];e===n&&m(Error("invalid uncompressed block header: NLEN (second byte)"));h|=
e<<8;g===~h&&m(Error("invalid uncompressed block header: length verify"));a+g>d.length&&m(Error("input buffer is broken"));switch(this.i){case y:for(;f+g>c.length;){l=k-f;g-=l;if(u)c.set(d.subarray(a,a+l),f),f+=l,a+=l;else for(;l--;)c[f++]=d[a++];this.b=f;c=this.e();f=this.b}break;case x:for(;f+g>c.length;)c=this.e({p:2});break;default:m(Error("invalid inflate mode"))}if(u)c.set(d.subarray(a,a+g),f),f+=g,a+=g;else for(;g--;)c[f++]=d[a++];this.a=a;this.b=f;this.c=c;break;case 1:this.j(B,C);break;case 2:aa(this);
break;default:m(Error("unknown BTYPE: "+b))}}return this.n()};
var D=[16,17,18,0,8,7,9,6,10,5,11,4,12,3,13,2,14,1,15],E=u?new Uint16Array(D):D,F=[3,4,5,6,7,8,9,10,11,13,15,17,19,23,27,31,35,43,51,59,67,83,99,115,131,163,195,227,258,258,258],G=u?new Uint16Array(F):F,H=[0,0,0,0,0,0,0,0,1,1,1,1,2,2,2,2,3,3,3,3,4,4,4,4,5,5,5,5,0,0,0],I=u?new Uint8Array(H):H,J=[1,2,3,4,5,7,9,13,17,25,33,49,65,97,129,193,257,385,513,769,1025,1537,2049,3073,4097,6145,8193,12289,16385,24577],K=u?new Uint16Array(J):J,L=[0,0,0,0,1,1,2,2,3,3,4,4,5,5,6,6,7,7,8,8,9,9,10,10,11,11,12,12,13,
13],M=u?new Uint8Array(L):L,N=new (u?Uint8Array:Array)(288),O,P;O=0;for(P=N.length;O<P;++O)N[O]=143>=O?8:255>=O?9:279>=O?7:8;var B=v(N),Q=new (u?Uint8Array:Array)(30),R,S;R=0;for(S=Q.length;R<S;++R)Q[R]=5;var C=v(Q);function A(b,d){for(var a=b.f,c=b.d,f=b.input,e=b.a,g;c<d;)g=f[e++],g===n&&m(Error("input buffer is broken")),a|=g<<c,c+=8;g=a&(1<<d)-1;b.f=a>>>d;b.d=c-d;b.a=e;return g}
function T(b,d){for(var a=b.f,c=b.d,f=b.input,e=b.a,g=d[0],h=d[1],k,l,q;c<h;){k=f[e++];if(k===n)break;a|=k<<c;c+=8}l=g[a&(1<<h)-1];q=l>>>16;b.f=a>>q;b.d=c-q;b.a=e;return l&65535}
function aa(b){function d(a,b,c){var d,e,f,g;for(g=0;g<a;)switch(d=T(this,b),d){case 16:for(f=3+A(this,2);f--;)c[g++]=e;break;case 17:for(f=3+A(this,3);f--;)c[g++]=0;e=0;break;case 18:for(f=11+A(this,7);f--;)c[g++]=0;e=0;break;default:e=c[g++]=d}return c}var a=A(b,5)+257,c=A(b,5)+1,f=A(b,4)+4,e=new (u?Uint8Array:Array)(E.length),g,h,k,l;for(l=0;l<f;++l)e[E[l]]=A(b,3);g=v(e);h=new (u?Uint8Array:Array)(a);k=new (u?Uint8Array:Array)(c);b.j(v(d.call(b,a,g,h)),v(d.call(b,c,g,k)))}
w.prototype.j=function(b,d){var a=this.c,c=this.b;this.o=b;for(var f=a.length-258,e,g,h,k;256!==(e=T(this,b));)if(256>e)c>=f&&(this.b=c,a=this.e(),c=this.b),a[c++]=e;else{g=e-257;k=G[g];0<I[g]&&(k+=A(this,I[g]));e=T(this,d);h=K[e];0<M[e]&&(h+=A(this,M[e]));c>=f&&(this.b=c,a=this.e(),c=this.b);for(;k--;)a[c]=a[c++-h]}for(;8<=this.d;)this.d-=8,this.a--;this.b=c};
w.prototype.w=function(b,d){var a=this.c,c=this.b;this.o=b;for(var f=a.length,e,g,h,k;256!==(e=T(this,b));)if(256>e)c>=f&&(a=this.e(),f=a.length),a[c++]=e;else{g=e-257;k=G[g];0<I[g]&&(k+=A(this,I[g]));e=T(this,d);h=K[e];0<M[e]&&(h+=A(this,M[e]));c+k>f&&(a=this.e(),f=a.length);for(;k--;)a[c]=a[c++-h]}for(;8<=this.d;)this.d-=8,this.a--;this.b=c};
w.prototype.e=function(){var b=new (u?Uint8Array:Array)(this.b-32768),d=this.b-32768,a,c,f=this.c;if(u)b.set(f.subarray(32768,b.length));else{a=0;for(c=b.length;a<c;++a)b[a]=f[a+32768]}this.g.push(b);this.l+=b.length;if(u)f.set(f.subarray(d,d+32768));else for(a=0;32768>a;++a)f[a]=f[d+a];this.b=32768;return f};
w.prototype.z=function(b){var d,a=this.input.length/this.a+1|0,c,f,e,g=this.input,h=this.c;b&&("number"===typeof b.p&&(a=b.p),"number"===typeof b.u&&(a+=b.u));2>a?(c=(g.length-this.a)/this.o[2],e=258*(c/2)|0,f=e<h.length?h.length+e:h.length<<1):f=h.length*a;u?(d=new Uint8Array(f),d.set(h)):d=h;return this.c=d};
w.prototype.n=function(){var b=0,d=this.c,a=this.g,c,f=new (u?Uint8Array:Array)(this.l+(this.b-32768)),e,g,h,k;if(0===a.length)return u?this.c.subarray(32768,this.b):this.c.slice(32768,this.b);e=0;for(g=a.length;e<g;++e){c=a[e];h=0;for(k=c.length;h<k;++h)f[b++]=c[h]}e=32768;for(g=this.b;e<g;++e)f[b++]=d[e];this.g=[];return this.buffer=f};
w.prototype.v=function(){var b,d=this.b;u?this.r?(b=new Uint8Array(d),b.set(this.c.subarray(0,d))):b=this.c.subarray(0,d):(this.c.length>d&&(this.c.length=d),b=this.c);return this.buffer=b};function U(b,d){var a,c;this.input=b;this.a=0;if(d||!(d={}))d.index&&(this.a=d.index),d.verify&&(this.A=d.verify);a=b[this.a++];c=b[this.a++];switch(a&15){case V:this.method=V;break;default:m(Error("unsupported compression method"))}0!==((a<<8)+c)%31&&m(Error("invalid fcheck flag:"+((a<<8)+c)%31));c&32&&m(Error("fdict flag is not supported"));this.q=new w(b,{index:this.a,bufferSize:d.bufferSize,bufferType:d.bufferType,resize:d.resize})}
U.prototype.k=function(){var b=this.input,d,a;d=this.q.k();this.a=this.q.a;if(this.A){a=(b[this.a++]<<24|b[this.a++]<<16|b[this.a++]<<8|b[this.a++])>>>0;var c=d;if("string"===typeof c){var f=c.split(""),e,g;e=0;for(g=f.length;e<g;e++)f[e]=(f[e].charCodeAt(0)&255)>>>0;c=f}for(var h=1,k=0,l=c.length,q,p=0;0<l;){q=1024<l?1024:l;l-=q;do h+=c[p++],k+=h;while(--q);h%=65521;k%=65521}a!==(k<<16|h)>>>0&&m(Error("invalid adler-32 checksum"))}return d};var V=8;s("Zlib.Inflate",U);s("Zlib.Inflate.prototype.decompress",U.prototype.k);var W={ADAPTIVE:z.s,BLOCK:z.t},X,Y,Z,$;if(Object.keys)X=Object.keys(W);else for(Y in X=[],Z=0,W)X[Z++]=Y;Z=0;for($=X.length;Z<$;++Z)Y=X[Z],s("Zlib.Inflate.BufferType."+Y,W[Y]);}).call(this); //@ sourceMappingURL=inflate.min.js.map
/** @license zlib.js 2012 - imaya [ https://github.com/imaya/zlib.js ] The MIT License */(function() {'use strict';var n=void 0,w=!0,aa=this;function ba(f,d){var c=f.split("."),e=aa;!(c[0]in e)&&e.execScript&&e.execScript("var "+c[0]);for(var b;c.length&&(b=c.shift());)!c.length&&d!==n?e[b]=d:e=e[b]?e[b]:e[b]={}};var C="undefined"!==typeof Uint8Array&&"undefined"!==typeof Uint16Array&&"undefined"!==typeof Uint32Array;function K(f,d){this.index="number"===typeof d?d:0;this.e=0;this.buffer=f instanceof(C?Uint8Array:Array)?f:new (C?Uint8Array:Array)(32768);if(2*this.buffer.length<=this.index)throw Error("invalid index");this.buffer.length<=this.index&&ca(this)}function ca(f){var d=f.buffer,c,e=d.length,b=new (C?Uint8Array:Array)(e<<1);if(C)b.set(d);else for(c=0;c<e;++c)b[c]=d[c];return f.buffer=b}
K.prototype.b=function(f,d,c){var e=this.buffer,b=this.index,a=this.e,g=e[b],m;c&&1<d&&(f=8<d?(L[f&255]<<24|L[f>>>8&255]<<16|L[f>>>16&255]<<8|L[f>>>24&255])>>32-d:L[f]>>8-d);if(8>d+a)g=g<<d|f,a+=d;else for(m=0;m<d;++m)g=g<<1|f>>d-m-1&1,8===++a&&(a=0,e[b++]=L[g],g=0,b===e.length&&(e=ca(this)));e[b]=g;this.buffer=e;this.e=a;this.index=b};K.prototype.finish=function(){var f=this.buffer,d=this.index,c;0<this.e&&(f[d]<<=8-this.e,f[d]=L[f[d]],d++);C?c=f.subarray(0,d):(f.length=d,c=f);return c};
var da=new (C?Uint8Array:Array)(256),M;for(M=0;256>M;++M){for(var N=M,S=N,ea=7,N=N>>>1;N;N>>>=1)S<<=1,S|=N&1,--ea;da[M]=(S<<ea&255)>>>0}var L=da;function ia(f){this.buffer=new (C?Uint16Array:Array)(2*f);this.length=0}ia.prototype.getParent=function(f){return 2*((f-2)/4|0)};ia.prototype.push=function(f,d){var c,e,b=this.buffer,a;c=this.length;b[this.length++]=d;for(b[this.length++]=f;0<c;)if(e=this.getParent(c),b[c]>b[e])a=b[c],b[c]=b[e],b[e]=a,a=b[c+1],b[c+1]=b[e+1],b[e+1]=a,c=e;else break;return this.length};
ia.prototype.pop=function(){var f,d,c=this.buffer,e,b,a;d=c[0];f=c[1];this.length-=2;c[0]=c[this.length];c[1]=c[this.length+1];for(a=0;;){b=2*a+2;if(b>=this.length)break;b+2<this.length&&c[b+2]>c[b]&&(b+=2);if(c[b]>c[a])e=c[a],c[a]=c[b],c[b]=e,e=c[a+1],c[a+1]=c[b+1],c[b+1]=e;else break;a=b}return{index:f,value:d,length:this.length}};function ka(f,d){this.d=la;this.i=0;this.input=C&&f instanceof Array?new Uint8Array(f):f;this.c=0;d&&(d.lazy&&(this.i=d.lazy),"number"===typeof d.compressionType&&(this.d=d.compressionType),d.outputBuffer&&(this.a=C&&d.outputBuffer instanceof Array?new Uint8Array(d.outputBuffer):d.outputBuffer),"number"===typeof d.outputIndex&&(this.c=d.outputIndex));this.a||(this.a=new (C?Uint8Array:Array)(32768))}var la=2,na={NONE:0,h:1,g:la,n:3},T=[],U;
for(U=0;288>U;U++)switch(w){case 143>=U:T.push([U+48,8]);break;case 255>=U:T.push([U-144+400,9]);break;case 279>=U:T.push([U-256+0,7]);break;case 287>=U:T.push([U-280+192,8]);break;default:throw"invalid literal: "+U;}
ka.prototype.f=function(){var f,d,c,e,b=this.input;switch(this.d){case 0:c=0;for(e=b.length;c<e;){d=C?b.subarray(c,c+65535):b.slice(c,c+65535);c+=d.length;var a=d,g=c===e,m=n,k=n,p=n,t=n,u=n,l=this.a,h=this.c;if(C){for(l=new Uint8Array(this.a.buffer);l.length<=h+a.length+5;)l=new Uint8Array(l.length<<1);l.set(this.a)}m=g?1:0;l[h++]=m|0;k=a.length;p=~k+65536&65535;l[h++]=k&255;l[h++]=k>>>8&255;l[h++]=p&255;l[h++]=p>>>8&255;if(C)l.set(a,h),h+=a.length,l=l.subarray(0,h);else{t=0;for(u=a.length;t<u;++t)l[h++]=
a[t];l.length=h}this.c=h;this.a=l}break;case 1:var q=new K(C?new Uint8Array(this.a.buffer):this.a,this.c);q.b(1,1,w);q.b(1,2,w);var s=oa(this,b),x,fa,z;x=0;for(fa=s.length;x<fa;x++)if(z=s[x],K.prototype.b.apply(q,T[z]),256<z)q.b(s[++x],s[++x],w),q.b(s[++x],5),q.b(s[++x],s[++x],w);else if(256===z)break;this.a=q.finish();this.c=this.a.length;break;case la:var B=new K(C?new Uint8Array(this.a.buffer):this.a,this.c),ta,J,O,P,Q,La=[16,17,18,0,8,7,9,6,10,5,11,4,12,3,13,2,14,1,15],X,ua,Y,va,ga,ja=Array(19),
wa,R,ha,y,xa;ta=la;B.b(1,1,w);B.b(ta,2,w);J=oa(this,b);X=pa(this.m,15);ua=qa(X);Y=pa(this.l,7);va=qa(Y);for(O=286;257<O&&0===X[O-1];O--);for(P=30;1<P&&0===Y[P-1];P--);var ya=O,za=P,F=new (C?Uint32Array:Array)(ya+za),r,G,v,Z,E=new (C?Uint32Array:Array)(316),D,A,H=new (C?Uint8Array:Array)(19);for(r=G=0;r<ya;r++)F[G++]=X[r];for(r=0;r<za;r++)F[G++]=Y[r];if(!C){r=0;for(Z=H.length;r<Z;++r)H[r]=0}r=D=0;for(Z=F.length;r<Z;r+=G){for(G=1;r+G<Z&&F[r+G]===F[r];++G);v=G;if(0===F[r])if(3>v)for(;0<v--;)E[D++]=0,
H[0]++;else for(;0<v;)A=138>v?v:138,A>v-3&&A<v&&(A=v-3),10>=A?(E[D++]=17,E[D++]=A-3,H[17]++):(E[D++]=18,E[D++]=A-11,H[18]++),v-=A;else if(E[D++]=F[r],H[F[r]]++,v--,3>v)for(;0<v--;)E[D++]=F[r],H[F[r]]++;else for(;0<v;)A=6>v?v:6,A>v-3&&A<v&&(A=v-3),E[D++]=16,E[D++]=A-3,H[16]++,v-=A}f=C?E.subarray(0,D):E.slice(0,D);ga=pa(H,7);for(y=0;19>y;y++)ja[y]=ga[La[y]];for(Q=19;4<Q&&0===ja[Q-1];Q--);wa=qa(ga);B.b(O-257,5,w);B.b(P-1,5,w);B.b(Q-4,4,w);for(y=0;y<Q;y++)B.b(ja[y],3,w);y=0;for(xa=f.length;y<xa;y++)if(R=
f[y],B.b(wa[R],ga[R],w),16<=R){y++;switch(R){case 16:ha=2;break;case 17:ha=3;break;case 18:ha=7;break;default:throw"invalid code: "+R;}B.b(f[y],ha,w)}var Aa=[ua,X],Ba=[va,Y],I,Ca,$,ma,Da,Ea,Fa,Ga;Da=Aa[0];Ea=Aa[1];Fa=Ba[0];Ga=Ba[1];I=0;for(Ca=J.length;I<Ca;++I)if($=J[I],B.b(Da[$],Ea[$],w),256<$)B.b(J[++I],J[++I],w),ma=J[++I],B.b(Fa[ma],Ga[ma],w),B.b(J[++I],J[++I],w);else if(256===$)break;this.a=B.finish();this.c=this.a.length;break;default:throw"invalid compression type";}return this.a};
function ra(f,d){this.length=f;this.k=d}
var sa=function(){function f(b){switch(w){case 3===b:return[257,b-3,0];case 4===b:return[258,b-4,0];case 5===b:return[259,b-5,0];case 6===b:return[260,b-6,0];case 7===b:return[261,b-7,0];case 8===b:return[262,b-8,0];case 9===b:return[263,b-9,0];case 10===b:return[264,b-10,0];case 12>=b:return[265,b-11,1];case 14>=b:return[266,b-13,1];case 16>=b:return[267,b-15,1];case 18>=b:return[268,b-17,1];case 22>=b:return[269,b-19,2];case 26>=b:return[270,b-23,2];case 30>=b:return[271,b-27,2];case 34>=b:return[272,
b-31,2];case 42>=b:return[273,b-35,3];case 50>=b:return[274,b-43,3];case 58>=b:return[275,b-51,3];case 66>=b:return[276,b-59,3];case 82>=b:return[277,b-67,4];case 98>=b:return[278,b-83,4];case 114>=b:return[279,b-99,4];case 130>=b:return[280,b-115,4];case 162>=b:return[281,b-131,5];case 194>=b:return[282,b-163,5];case 226>=b:return[283,b-195,5];case 257>=b:return[284,b-227,5];case 258===b:return[285,b-258,0];default:throw"invalid length: "+b;}}var d=[],c,e;for(c=3;258>=c;c++)e=f(c),d[c]=e[2]<<24|
e[1]<<16|e[0];return d}(),Ha=C?new Uint32Array(sa):sa;
function oa(f,d){function c(b,c){var a=b.k,d=[],e=0,f;f=Ha[b.length];d[e++]=f&65535;d[e++]=f>>16&255;d[e++]=f>>24;var g;switch(w){case 1===a:g=[0,a-1,0];break;case 2===a:g=[1,a-2,0];break;case 3===a:g=[2,a-3,0];break;case 4===a:g=[3,a-4,0];break;case 6>=a:g=[4,a-5,1];break;case 8>=a:g=[5,a-7,1];break;case 12>=a:g=[6,a-9,2];break;case 16>=a:g=[7,a-13,2];break;case 24>=a:g=[8,a-17,3];break;case 32>=a:g=[9,a-25,3];break;case 48>=a:g=[10,a-33,4];break;case 64>=a:g=[11,a-49,4];break;case 96>=a:g=[12,a-
65,5];break;case 128>=a:g=[13,a-97,5];break;case 192>=a:g=[14,a-129,6];break;case 256>=a:g=[15,a-193,6];break;case 384>=a:g=[16,a-257,7];break;case 512>=a:g=[17,a-385,7];break;case 768>=a:g=[18,a-513,8];break;case 1024>=a:g=[19,a-769,8];break;case 1536>=a:g=[20,a-1025,9];break;case 2048>=a:g=[21,a-1537,9];break;case 3072>=a:g=[22,a-2049,10];break;case 4096>=a:g=[23,a-3073,10];break;case 6144>=a:g=[24,a-4097,11];break;case 8192>=a:g=[25,a-6145,11];break;case 12288>=a:g=[26,a-8193,12];break;case 16384>=
a:g=[27,a-12289,12];break;case 24576>=a:g=[28,a-16385,13];break;case 32768>=a:g=[29,a-24577,13];break;default:throw"invalid distance";}f=g;d[e++]=f[0];d[e++]=f[1];d[e++]=f[2];var k,m;k=0;for(m=d.length;k<m;++k)l[h++]=d[k];s[d[0]]++;x[d[3]]++;q=b.length+c-1;u=null}var e,b,a,g,m,k={},p,t,u,l=C?new Uint16Array(2*d.length):[],h=0,q=0,s=new (C?Uint32Array:Array)(286),x=new (C?Uint32Array:Array)(30),fa=f.i,z;if(!C){for(a=0;285>=a;)s[a++]=0;for(a=0;29>=a;)x[a++]=0}s[256]=1;e=0;for(b=d.length;e<b;++e){a=
m=0;for(g=3;a<g&&e+a!==b;++a)m=m<<8|d[e+a];k[m]===n&&(k[m]=[]);p=k[m];if(!(0<q--)){for(;0<p.length&&32768<e-p[0];)p.shift();if(e+3>=b){u&&c(u,-1);a=0;for(g=b-e;a<g;++a)z=d[e+a],l[h++]=z,++s[z];break}0<p.length?(t=Ia(d,e,p),u?u.length<t.length?(z=d[e-1],l[h++]=z,++s[z],c(t,0)):c(u,-1):t.length<fa?u=t:c(t,0)):u?c(u,-1):(z=d[e],l[h++]=z,++s[z])}p.push(e)}l[h++]=256;s[256]++;f.m=s;f.l=x;return C?l.subarray(0,h):l}
function Ia(f,d,c){var e,b,a=0,g,m,k,p,t=f.length;m=0;p=c.length;a:for(;m<p;m++){e=c[p-m-1];g=3;if(3<a){for(k=a;3<k;k--)if(f[e+k-1]!==f[d+k-1])continue a;g=a}for(;258>g&&d+g<t&&f[e+g]===f[d+g];)++g;g>a&&(b=e,a=g);if(258===g)break}return new ra(a,d-b)}
function pa(f,d){var c=f.length,e=new ia(572),b=new (C?Uint8Array:Array)(c),a,g,m,k,p;if(!C)for(k=0;k<c;k++)b[k]=0;for(k=0;k<c;++k)0<f[k]&&e.push(k,f[k]);a=Array(e.length/2);g=new (C?Uint32Array:Array)(e.length/2);if(1===a.length)return b[e.pop().index]=1,b;k=0;for(p=e.length/2;k<p;++k)a[k]=e.pop(),g[k]=a[k].value;m=Ja(g,g.length,d);k=0;for(p=a.length;k<p;++k)b[a[k].index]=m[k];return b}
function Ja(f,d,c){function e(a){var b=k[a][p[a]];b===d?(e(a+1),e(a+1)):--g[b];++p[a]}var b=new (C?Uint16Array:Array)(c),a=new (C?Uint8Array:Array)(c),g=new (C?Uint8Array:Array)(d),m=Array(c),k=Array(c),p=Array(c),t=(1<<c)-d,u=1<<c-1,l,h,q,s,x;b[c-1]=d;for(h=0;h<c;++h)t<u?a[h]=0:(a[h]=1,t-=u),t<<=1,b[c-2-h]=(b[c-1-h]/2|0)+d;b[0]=a[0];m[0]=Array(b[0]);k[0]=Array(b[0]);for(h=1;h<c;++h)b[h]>2*b[h-1]+a[h]&&(b[h]=2*b[h-1]+a[h]),m[h]=Array(b[h]),k[h]=Array(b[h]);for(l=0;l<d;++l)g[l]=c;for(q=0;q<b[c-1];++q)m[c-
1][q]=f[q],k[c-1][q]=q;for(l=0;l<c;++l)p[l]=0;1===a[c-1]&&(--g[0],++p[c-1]);for(h=c-2;0<=h;--h){s=l=0;x=p[h+1];for(q=0;q<b[h];q++)s=m[h+1][x]+m[h+1][x+1],s>f[l]?(m[h][q]=s,k[h][q]=d,x+=2):(m[h][q]=f[l],k[h][q]=l,++l);p[h]=0;1===a[h]&&e(h)}return g}
function qa(f){var d=new (C?Uint16Array:Array)(f.length),c=[],e=[],b=0,a,g,m,k;a=0;for(g=f.length;a<g;a++)c[f[a]]=(c[f[a]]|0)+1;a=1;for(g=16;a<=g;a++)e[a]=b,b+=c[a]|0,b<<=1;a=0;for(g=f.length;a<g;a++){b=e[f[a]];e[f[a]]+=1;m=d[a]=0;for(k=f[a];m<k;m++)d[a]=d[a]<<1|b&1,b>>>=1}return d};function Ka(f,d){this.input=f;this.a=new (C?Uint8Array:Array)(32768);this.d=V.g;var c={},e;if((d||!(d={}))&&"number"===typeof d.compressionType)this.d=d.compressionType;for(e in d)c[e]=d[e];c.outputBuffer=this.a;this.j=new ka(this.input,c)}var V=na;
Ka.prototype.f=function(){var f,d,c,e,b,a,g=0;a=this.a;switch(8){case 8:f=Math.LOG2E*Math.log(32768)-8;break;default:throw Error("invalid compression method");}d=f<<4|8;a[g++]=d;switch(8){case 8:switch(this.d){case V.NONE:e=0;break;case V.h:e=1;break;case V.g:e=2;break;default:throw Error("unsupported compression type");}break;default:throw Error("invalid compression method");}c=e<<6|0;a[g++]=c|31-(256*d+c)%31;var m=this.input;if("string"===typeof m){var k=m.split(""),p,t;p=0;for(t=k.length;p<t;p++)k[p]=
(k[p].charCodeAt(0)&255)>>>0;m=k}for(var u=1,l=0,h=m.length,q,s=0;0<h;){q=1024<h?1024:h;h-=q;do u+=m[s++],l+=u;while(--q);u%=65521;l%=65521}b=(l<<16|u)>>>0;this.j.c=g;a=this.j.f();g=a.length;C&&(a=new Uint8Array(a.buffer),a.length<=g+4&&(this.a=new Uint8Array(a.length+4),this.a.set(a),a=this.a),a=a.subarray(0,g+4));a[g++]=b>>24&255;a[g++]=b>>16&255;a[g++]=b>>8&255;a[g++]=b&255;return a};ba("Zlib.Deflate",Ka);ba("Zlib.Deflate.compress",function(f,d){return(new Ka(f,d)).f()});ba("Zlib.Deflate.prototype.compress",Ka.prototype.f);var Ma={NONE:V.NONE,FIXED:V.h,DYNAMIC:V.g},Na,Oa,W,Pa;if(Object.keys)Na=Object.keys(Ma);else for(Oa in Na=[],W=0,Ma)Na[W++]=Oa;W=0;for(Pa=Na.length;W<Pa;++W)Oa=Na[W],ba("Zlib.Deflate.CompressionType."+Oa,Ma[Oa]);}).call(this); //@ sourceMappingURL=deflate.min.js.map
Trap.Authentication = function()
{
	
	/**
	 * Fetches a collection of keys (strings) that this TrapAuthentication
	 * instance wants from the TrapTransport. The TrapAuthentication instance
	 * must not change this collection after this call, and must not assume the
	 * transport will call this function more times than one. It can assume the
	 * transport calls this function at least once.
	 * <p>
	 * As an argument, the transport supplies the context keys available from
	 * this transport. The returned collection may not contain a key that does
	 * not exist in the <i>availableKeys</i> collection. If the
	 * TrapAuthentication's implementation requires a context value whose key
	 * not exist in <i>availableKeys</i>, it may generate a value, as long as it
	 * does not significantly compromise the integrity of the authentication.
	 * <p>
	 * If there is not enough context information for this TrapAuthentication
	 * instance to successfully work, it may throw a TrapException.
	 * 
	 * @param availableKeys
	 *            A collection containing all keys that the TrapTransport can
	 *            fill in with meaningful values.
	 * @throws TrapException
	 *             If there is not enough context information for this
	 *             TrapAuthentication instance to successfully work
	 * @return A collection of keys that the TrapAuthentication instance wants
	 *         the TrapTransport to provide on every call.
	 */
	this.getContextKeys = function(availableKeys) { return []; };
	
	/**
	 * Verifies the authentication of a message. Checks the authentication
	 * header against Trapauthentication's internal state, and checks if it is
	 * correct. Additional data is provided by the transport in the form of
	 * other message headers (if any), as well as the message body (if
	 * available) and, finally, the additional context keys requested.
	 * 
	 * @param authenticationString
	 *            The authentication string provided by the other side. This
	 *            does not include beginning or trailing whitespaces, newlines,
	 *            etc, and does not include an eventual header name this was
	 *            sent in, nor the authentication type (e.g. DIGEST). If the
	 *            authentication string was transferred as part of a message
	 *            header, that header may be present in the <i>headers</I> map
	 *            if and only if it is called exactly "Authorization".
	 * @param headers
	 *            A map (String, String) of eventual other message headers. May
	 *            contain any number of headers (including zero). May not be
	 *            null. May not be modified by verifyAuthentication.
	 * @param body
	 *            A message body, if present. May be null. May not be modified
	 *            by verifyAuthentication.
	 * @param context
	 *            A non-null map of the context values requested by this
	 *            TrapAuthentication in {@link #getContextKeys(Collection)}.
	 *            Every key that was returned by getContextKeys MUST be filled
	 *            in.
	 * @return <i>true</i> if the authentication string is correct, <i>false</i>
	 *         otherwise (incorrect, could not be verified, etc).
	 */
	this.verifyAuthentication = function(authenticationString, headers, body, context) {return true;};
	
	/**
	 * Creates an authentication challenge.
	 * 
	 * @param context
	 *            A map of key/value pairs deduced from the transport and
	 *            environment
	 * @return A finished authentication challenge, to be inserted into the
	 *         message to the remote end.
	 */
	this.createAuthenticationChallenge = function(context) { return "";};
	
	/**
	 * Creates an authentication string to answer an authentication challenge,
	 * or sign a message. The TrapTransport provides the challenge
	 * authentication header of the last message(if any). If there is no
	 * authentication header, the TrapAuthentication instance should attempt to
	 * generate an authentication string from the current context and state. If
	 * that fails, it may throw a TrapException.
	 * <p>
	 * The call additionally includes the message header(s) and body (if any) to
	 * be signed in the authentication header, as well as the TrapTransport's
	 * context, as requested by this TrapAuthentication instance.
	 * 
	 * @param challengeString
	 *            A challenge string received by the TrapTransport, or null if
	 *            there was no new challenge.
	 * @param headers
	 *            Eventual message headers
	 * @param body
	 *            Eventual body
	 * @param context
	 *            A non-null map of the context values requested by this
	 *            TrapAuthentication in {@link #getContextKeys(Collection)}.
	 *            Every key that was returned by getContextKeys MUST be filled
	 *            in.
	 * @return An authentication response corresponding to the challenge, to be
	 *         inserted into a message with no further modifications
	 */
	this.createAuthenticationResponse = function(challengeString, headers, body, context) {return "";};
};
/**
 * <b>Never instantiate a channel manually</b>. Trap will manage the channels.
 * 
 * @constructor
 * @param {Trap.Endpoint} endpoint The endpoint that spawned this channel.
 * @param {Number} channelID The channel ID being created
 * @classdesc A channel is a logical stream of Trap messages, multiplexed on the
 *            same Trap connection. Essentially, this allows sending multiple
 *            streams over the same connection. This is useful when multiple
 *            forms of data may need to be transported over a Trap session (e.g.
 *            short and long messages mixed), where a long/large message should
 *            not hold up a short/small message.
 *            <p>
 *            In the default case, there are two channels on every TrapEndpoint.
 *            Channel ID 0 will consist of control traffic, ensuring the
 *            endpoint is alive, managing transports, etc. It will have the
 *            highest priority, ensuring the endpoint can manage itself. Channel
 *            ID 1 will consist of application data. It will yield to Channel ID
 *            0, ensuring that the application sending large messages will not
 *            cause control traffic to time out.
 *            <p>
 *            Trap version 1.2 supports up to 256 different channels. It is not
 *            recommended that Channel ID 0 is used for application data,
 *            leaving 255 channels for the application to use. Each channel can
 *            have its features individually configured.
 *            <p>
 *            When instantiated, channels have certain default settings. Trap's
 *            default implementation will use a chunk size of 16KB, and limit to
 *            128KB in-flight bytes per channel. The channels will not operate
 *            in streaming mode by default. The default priority will be 0,
 *            except for Channel ID 0 which has the maximum priority.
 *            <p>
 *            The in-flight window will limit the throughput on fast links,
 *            while preventing us from oversaturating slow links. As an example,
 *            assuming 100ms latency and 128kb window size, we will at most
 *            process 10 windows per second, or 1280kb/s. 10ms latency yields
 *            12800kb/s. Increasing the window size on a faster link will yield
 *            more throughput, but may risk oversaturating a slower link.
 * 
 * @property {Boolean} streamingEnabled Controls the <i>streaming</i> flag of
 *           the channel. When a channel works in streaming mode, it will
 *           dispatch trapData events as data is received, although always in
 *           the correct order. With streaming mode disabled, each trapData
 *           event will represent a single send() event on the other side.
 *           <p>
 *           Streaming mode is useful for when Trap is used to transfer larger
 *           chunks of data, whose framing is internal to the data transferred.
 *           For example, an image, a song, or a video stream. Streaming mode
 *           will reduce – but not eliminate – the amount of buffering done in
 *           Trap.
 * 
 * @property {Integer} chunkSize The maximum number of bytes allowed in each
 *           message. Note that the chunk size includes the Trap message header,
 *           and this will be automatically subtracted from <i>numBytes</i>,
 *           unless numBytes is in the range of [1, TRAP_HEADER_SIZE]. If
 *           numBytes is zero or negative, chunking will be disabled.
 *           <p>
 *           Note that a chunkSize of Integer.MAX_VALUE will disable chunking. A
 *           channel will have that value set if the remote endpoint is
 *           suspected of not supporting chunking. Excepting that, chunkSize
 *           will automatically be reduced to the trap config option
 *           {@link TrapEndpoint#OPTION_MAX_CHUNK_SIZE}, which is automatically
 *           negotiated between the peers.
 * 
 * @property {Integer} maxInFlightBytes The maximum number of in-flight bytes.
 *           Combined with the chunk size, this limits the number of messages
 *           that the channel will allow to be in transit at any given time.
 *           <p>
 *           Increasing the number of in flight bytes will increase the required
 *           buffer sizes on both the local and remote ends, as well as the
 *           system's network buffers. It may also increase throughput,
 *           especially on congested links or when using multiple transports.
 *           <p>
 *           Note that in-flight bytes differs from the queue size. The queue
 *           denotes how many messages/bytes this channel can buffer, while
 *           in-flight bytes denotes how many messages/bytes we allow on the
 *           network.
 * 
 * @property {Integer} priority The channel priority, relative to the other
 *           channels. Channel ID 0 has priority {@link Integer#MAX_VALUE},
 *           meaning any traffic on 0 takes precedence of any other traffic, for
 *           any reason.
 *           <p>
 *           Priority is byte based. A channel with priority <i>n</i> will be
 *           allowed to send up to <i>n</i> bytes before ceding transmission
 *           rights to a transport with lower priority. Note that if
 *           <i>chunkSize</i> exceeds priority, the transport will nevertheless
 *           be allowed to send <i>chunkSize</i> number of bytes.
 *           <p>
 *           Priority only affects the scheduling order of messages, and not the
 *           throughput. For the exact buffering, one must consider the
 *           channel's in-flight limit, the endpoint's in-flight limit (if any),
 *           as well as the transports' in-flight limit.
 */
Trap.Channel = function(endpoint, channelID) {
	this._parentEP = endpoint;
	this._channelID = channelID;
	this._streamingEnabled = false;
	this._chunkSize = 16 * 1024;
	this._maxInFlightBytes = this._chunkSize * 8;
	this._bytesInFlight = 0;
	this._available = false;

	this._messageId = 1;
	this._maxMessageId = 0x8000000;
	this._priority = 0;

	this._outQueue = new Trap.List();
	this._inBuf = new Trap.MessageBuffer(50, 1000, 1, 1, this.maxMessageId);

	this.failedMessages = new Trap.List();

	this.tmp = {};
	this.buf = new Trap.ByteArrayOutputStream();
	this.receivingFragment = false;

};

Trap._compat.__defineGetterSetter(Trap.Channel.prototype, "parentEP");
Trap._compat.__defineGetterSetter(Trap.Channel.prototype, "channelID");
Trap._compat.__defineGetterSetter(Trap.Channel.prototype, "streamingEnabled");
Trap._compat.__defineGetter(Trap.Channel.prototype, "chunkSize");
Trap._compat.__defineGetterSetter(Trap.Channel.prototype, "maxInFlightBytes");
Trap._compat.__defineGetterSetter(Trap.Channel.prototype, "bytesInFlight");
Trap._compat.__defineGetterSetter(Trap.Channel.prototype, "available");
Trap._compat.__defineGetterSetter(Trap.Channel.prototype, "messageId");
Trap._compat.__defineGetterSetter(Trap.Channel.prototype, "maxMessageId");
Trap._compat.__defineGetterSetter(Trap.Channel.prototype, "outQueue");
Trap._compat.__defineGetterSetter(Trap.Channel.prototype, "inBuf");
Trap._compat.__defineGetterSetter(Trap.Channel.prototype, "priority");

Trap._compat.__defineSetter(Trap.Channel.prototype, "chunkSize", function(
		numBytes) {
	var newSize = numBytes;

	if (newSize > 16)
		newSize -= 16;

	if (newSize > this.parentEP.getMaxChunkSize())
		newSize = this.parentEP.getMaxChunkSize();

	if (newSize <= 0)
		newSize = Integer.MAX_VALUE;

	this._chunkSize = newSize;
	return this;
});

/*
 * @param {Trap.Message} message @return null
 */
Trap.Channel.prototype.assignMessageID = function(message) {
	if (message.getMessageId() == 0) {
		// Assign message id (if not already set)
		var messageId = this.messageId++;

		if (messageId > this.maxMessageId)
			this.messageId = messageId = 1;

		message.setMessageId(messageId);
	}
};

/*
 * Send a message on the channel. If required, splits up the message in multiple
 * component parts. Note that calling this method guarantees the message will be
 * serialized.
 * 
 * @param {Trap.Message} message The message to send. @throws TrapException If
 * an error occurs during sending @return void
 */
Trap.Channel.prototype.send = function(message, disableChunking) {

	this.assignMessageID(message);

	// Perform the estimate computation.
	if (!disableChunking) {
		var data = message.getCompressedData();
		if (data != null && data.length > this.chunkSize) {

			// We need to chunk it up.
			for ( var i = 0; i < data.length; i += this.chunkSize) {
				var chunk = Trap.subarray(data, i, Math.min(i + this.chunkSize,
						data.length));
				var m = new Trap.Message();
				m.setData(chunk);

				if (i == 0) {
					m.setOp(Trap.Message.Operation.FRAGMENT_START);
					m.setMessageId(message.getMessageId());
				} else if (i + this.chunkSize >= data.length)
					m.setOp(Trap.Message.Operation.FRAGMENT_END);
				else
					m.setOp(Trap.Message.Operation.MESSAGE);

				m.setCompressed(message.getCompressed());
				m.setFormat(message.getFormat());
				this.send(m, true);
			}

			return;

		}
	}

	message.setChannel(this.channelID);
	this.outQueue.addLast(message);

	if (this.bytesInFlight < this.maxInFlightBytes)
		this.available = true;
};

/*
 * @param {Trap.Message} message @return void
 */
Trap.Channel.prototype.messageSent = function(message) {
	this.bytesInFlight -= message.length();

	if (this.bytesInFlight < this.maxInFlightBytes
			&& this.outQueue.peek() != null)
		this.available = true;

	this.parentEP.kickSendingThread();
};

/*
 * @param {Trap.Message} failedMessage @return null;
 */
Trap.Channel.prototype.addFailedMessage = function(failedMessage) {
	this.failedMessages.add(failedMessage);
};

Trap.Channel.prototype.rebuildMessageQueue = function() {

	if (this.failedMessages.isEmpty())
		return;

	// We should iterate over the failed messages and remove them from the
	// transit messages

	var fit = this.failedMessages.iterator();
	while (fit.hasNext())
		this.bytesInFlight -= fit.next().length();

	var newMessageQueue = new Trap.List();

	// Rebuild the queue easily.
	var newQueue = new LinkedList();

	var it = this.failedMessages.iterator();

	var failed = it.next();

	while (failed != null && failed.getMessageId() == 0) {
		if (it.hasNext())
			failed = it.next();
		else
			failed = null;
	}

	var queued = this.outQueue.peek();

	while ((failed != null) || (queued != null)) {

		if (queued != null)
			this.outQueue.pop();

		if ((queued != null) && (failed != null)) {
			if (queued.getMessageId() < failed.getMessageId()) {
				newQueue.add(queued);
				queued = null;
			} else {
				newQueue.add(failed);
				failed = null;
			}
		} else if (failed == null) {
			newQueue.add(queued);
			queued = null;
		} else {
			newQueue.add(failed);
			failed = null;
		}

		if ((failed == null) && it.hasNext())
			failed = it.next();

		if (queued == null)
			queued = this.outQueue.peek();
	}

	// We'll need a new loop to eliminate duplicates.
	// This loop will actually defer the messages.
	var lastMessageId = -1;

	var ni = newQueue.iterator();

	while (ni.hasNext()) {
		var m = ni.next();

		if (m.getMessageId() != lastMessageId) {

			lastMessageId = m.getMessageId();
			newMessageQueue.put(m);
		}
	}

	this.outQueue = newMessageQueue;
	this.failedMessages.clear();

	if (this.bytesInFlight < this.maxInFlightBytes
			&& this.outQueue.peek() != null) {
		this.available = true;
	}

};

/*
 * 
 * @returns {Boolean}
 */
Trap.Channel.prototype.messagesAvailable = function() {
	return this.available;
};

/*
 * 
 * @returns {Trap.Message}
 */
Trap.Channel.prototype.peek = function() {
	if (this.messagesAvailable())
		return this.outQueue.peek();

	return null;
};

/*
 * @returns {Trap.Message}
 */
Trap.Channel.prototype.pop = function() {
	var message = null;

	message = this.outQueue.pop();

	if (message != null)
		this.bytesInFlight += message.length();

	if (this.outQueue.peek() == null
			|| this.bytesInFlight >= this.maxInFlightBytes)
		this.available = false;

	return message;
};

/*
 * @param {Trap.Message} m @param {Trap.Transport} t @returns void
 */
Trap.Channel.prototype.receiveMessage = function(m, t) {
	this.inBuf.put(m, t);

	for (;;) {
		try {
			while (this.inBuf.fetch(this.tmp, false)) {

				if (!this.streamingEnabled) {
					if (this.receivingFragment) {
						switch (this.tmp.m.getOp()) {
						case Trap.Message.Operation.FRAGMENT_END:
							this.receivingFragment = false;
							this.tmp.m.setOp(Trap.Message.Operation.MESSAGE);
						case Trap.Message.Operation.MESSAGE:
							this.buf.write(this.tmp.m.getData());
							break;

						default:
							break;
						}

						if (!this.receivingFragment) {
							this.tmp.m.setData(this.buf.toArray());

							if (this.tmp.m.getCompressed())
								this.tmp.m.setData(new Zlib.Inflate(this.tmp.m
										.getData()).decompress());

							this.buf = new Trap.ByteArrayOutputStream();
						} else {
							continue;
						}
					} else {
						if (this.tmp.m.getOp() == Trap.Message.Operation.FRAGMENT_START) {
							this.receivingFragment = true;
							this.buf.write(this.tmp.m.getData());
							continue;
						}
					}
				}

				this.parentEP.executeMessageReceived(this.tmp.m, this.tmp.t);
			}

		} catch (e) {
			console.log(e.stack);
		} finally {
			// System.out.println("Exiting run loop with available: " +
			// this.inBuf.available());
		}

		if (this.inBuf.available() > 0)
			continue;

		return;
	}
};

Trap.Channel.prototype.toString = function() {
	return "(" + this.channelID + "/o:" + this.outQueue.length() + "/i:"
			+ this.inBuf.toString() + ")";
};

Trap.ChannelMessageQueue = function()
{
	this.priorities = [];
	this.cPrio = 0;
	this.cPrioIndex = 0;
	this.cPrioBytes = 0;
	
	var RoundRobinChannelSelector = function()
	{
		this.channels = [];
		this.currChannel = 0;
		
		this.getPriority = function()
		{
			return this.channels.length > 0 ? this.channels[0].getPriority() : Number.MIN_VALUE;
		};
		
		this.peek = function()
		{
			try
			{
				var start = this.currChannel;
				var end = this.currChannel + this.channels.length;
				
				for ( var i = start; i < end; i++)
				{
					var m = this.channels[this.currChannel % this.channels.length].peek();
					
					if (m != null) return m;
					
					this.currChannel++;
				}
				
				return null;
			}
			finally
			{
				this.currChannel = this.currChannel % this.channels.length;
			}
		};
		
		this.pop = function()
		{
			var rv = this.channels[this.currChannel].pop();
			this.currChannel++;
			return rv;
		};
		
		this.addChannel = function(c)
		{
			this.channels.push(c);
		};
		
		this.toString = function()
		{
			var sb = new StringBuilder();
			
			sb.append("{");
			
			for ( var i = 0; i < this.channels.length; i++)
			{
				if (i > 0) sb.append(", ");
				sb.append(this.channels[i].toString());
			}
			
			sb.append("}");
			
			return sb.toString();
		};
		
	};
	
	this.rebuild = function(channels)
	{
		var sortedChannels = new Trap.List();
		sortedChannels.addAll(channels);
		sortedChannels.sort(function(a, b) { return b.getPriority() - a.getPriority(); });
		
		var prioList = [];
		var it = sortedChannels.iterator();
		var lastPriority = Number.MIN_VALUE;
		var sel = null;
		
		while (it.hasNext())
		{
			var c = it.next();
			
			if (c.getPriority() != lastPriority)
			{
				sel = new RoundRobinChannelSelector();
				prioList.push(sel);
				lastPriority = c.getPriority();
			}
			sel.addChannel(c);
		}
		
		this.priorities = prioList;
		this.setPrioIndex(0);
	};
	
	this.peek = function()
	{
		
		for ( var i = this.cPrioIndex; i < this.priorities.length; i++)
		{
			var msg = this.priorities[i].peek();
			if (msg != null)
			{
				this.cPrioIndex = i;
				return msg;
			}
		}
		
		return null;
	};
	
	this.pop = function()
	{
		var popped = this.priorities[this.cPrioIndex].pop();
		var bs = popped.length();
		this.cPrioBytes += bs;
		
		if (this.cPrioBytes > this.cPrio) this.setPrioIndex(this.cPrioIndex++);
		
		return popped;
	};
	
	this.rewind = function()
	{
		this.setPrioIndex(0);
	};
	
	this.setPrioIndex = function(idx)
	{
		if (idx < this.priorities.length)
		{
			this.cPrioIndex = idx;
			this.cPrio = this.priorities[this.cPrioIndex].getPriority();
			this.cPrioBytes = 0;
		}
	};
	
	this.toString = function()
	{
		var sb = new Trap.StringBuilder();
		
		sb.append("[\n");
		
		for ( var i = 0; i < this.priorities.length; i++)
		{
			var cs = this.priorities[i];
			
			sb.append("\t");
			sb.append(cs.getPriority());
			sb.append(": ");
			sb.append(cs.toString());
			sb.append("\n");
		}
		
		sb.append("\n]");
		
		return sb.toString();
	};
	
};
Trap.Configuration = function(cfgString)
{
	
	// Set up local fields
	this.config = new Trap.Map();
	
	if (cfgString != null)
		this.initFromString(cfgString);
};

Trap.Configuration.CONFIG_HASH_PROPERTY = "trap.confighash";

Trap.Configuration.prototype.initFromString = function(configString)
{
	var strings = configString.split('\n');
	
	for (var i=0; i<strings.length; i++)
	{
		var c = strings[i].trim();
		
		var pos = c.indexOf('=');
		
		// Not found, alternatively no value
		if(pos < 0 || pos >= c.length - 1)
			continue;
		
		this.config.put(c.substring(0, pos).trim(), c.substring(pos+1).trim());
	}
};

Trap.Configuration.prototype.createPuttableGettableMap = function(optionsPrefix, cutPrefixes)
{
	var mt = this;
	var m = new Trap.Map();
	
	if (typeof(cutPrefixes) == "undefined")
		cutPrefixes = true;
	
	m.prefixKey = function(key) 
	{
		sb = (cutPrefixes?optionsPrefix:key);
		
		if(cutPrefixes) 
		{
			if(!optionsPrefix.endsWith("."))
				sb += ".";
			sb += key;
		}
		return sb;
	};
	
	m.put = function(key, value)
	{
		if(key == null || value == null)
			throw "Cannot put nil key or value";
		
		mt.config.put(m.prefixKey(key), value);
		Trap.Map.prototype.put.call(m, key, value);
	};
	
	return m;
};

Trap.Configuration.prototype.getOptions = function(optionsPrefix, cutPrefixes)
{
	if (typeof(cutPrefixes) == "undefined")
		cutPrefixes = true;
	
	var x = (cutPrefixes && !optionsPrefix.endsWith("."))?1:0;
	var m = this.createPuttableGettableMap(optionsPrefix, cutPrefixes);
	
	var keys = this.config.allKeys();
	
	for (var i=0; i<keys.length; i++)
	{
		var key = keys[i];
		var value = this.config.get(key);
		if(key.startsWith(optionsPrefix)) {
			if(cutPrefixes)
				key = key.substring(optionsPrefix.length+x);
			m.put(key, value);
		}
	}
	return m;
};

Trap.Configuration.prototype.toString = function()
{
	var keys = this.config.allKeys().sort();
	
	var sb = new Trap.StringBuilder();
	
	for (var i = 0; i < keys.length; i++)
	{
		sb.append(keys[i]);
		sb.append(" = ");
		sb.append(this.config.get(keys[i]));
		sb.append("\n");
	}
	return sb.toString();
};

/*
 * This code is unreadable. Refer to the Java implementation for what it does.
 * The mess here is because JavaScript doesn't support multiple signatures for
 * the same function name.
 */
Trap.Configuration.prototype.getOption = function(a1, a2)
{
	return this.config.get((typeof(a2) != "undefined"?a1+"."+a2:a1));
};

Trap.Configuration.prototype.getIntOption = function(option, defaultValue)
{
	
	var rv = parseInt(this.getOption(option));
	
	if (isNaN(rv))
		return defaultValue;
	
	return rv;

};

Trap.Configuration.prototype.getBooleanOption = function(option, defaultValue)
{
	
	var rv = this.getOption(option);
	
	if (typeof(rv) != "string")
		return defaultValue;
	
	if ("true" === rv.toLowerCase())
		return true;
	
	if ("false" === rv.toLowerCase())
	
	return defaultValue;

};

Trap.Configuration.prototype.setOption = function(a1, a2, a3)
{
	this.config.put((typeof(a3) != "undefined"?a1+"."+a2:a1), (typeof(a3) != "undefined"?a3:a2));
};

Trap.CustomConfiguration = function(cfgString) 
{
	// "super"
	Trap.Configuration.prototype.constructor.call(this, cfgString);
	
	this.setStaticConfiguration(cfgString);
	
};

Trap.CustomConfiguration.prototype = new Trap.Configuration;
Trap.CustomConfiguration.prototype.constructor = Trap.CustomConfiguration;

Trap.CustomConfiguration.prototype.setStaticConfiguration = function(configuration) {
	this.staticConfig = new Trap.Configuration(configuration);
};

Trap.CustomConfiguration.prototype.getOptions = function(optionsPrefix, cutPrefixes) {
	var options = this.createPuttableGettableMap(optionsPrefix, cutPrefixes);
	options.putAll(this.staticConfig.getOptions(optionsPrefix, cutPrefixes));
	options.putAll(Trap.Configuration.prototype.getOptions.call(this, optionsPrefix, cutPrefixes));
	return options;
};

Trap.CustomConfiguration.prototype.getOption = function() {
	var val = Trap.Configuration.prototype.getOption.apply(this, arguments);
	if (val == null)
		val = this.staticConfig.getOption.apply(this.staticConfig, arguments);
	return val;
};

Trap.CustomConfiguration.prototype.toString = function() 
{
	var sb = new Trap.StringBuffer();
	var keys = new Array();
	keys.push.apply(keys, this.staticConfig.config.allKeys());
	keys.push.apply(keys, this.config.allKeys());
	keys.sort();
	
	// Eliminate duplicate keys
	var len=keys.length,
	out=[],
	obj={};

	for (var i=0;i<len;i++) {
		obj[keys[i]]=0;
	}
	for (i in obj) {
		out.push(i);
	}

	for(var i=0;i<out.length;i++) {
		var key = out[i];
		sb.append(key);
		sb.append(" = ");
		sb.append(this.getOption(key));
		sb.append("\n");
	}
	return sb.toString();
};
Trap.Keepalive = {};
Trap.Keepalive.Policy = {
		DISABLED: -1,
		DEFAULT: 0
};

// Note that unlike api classes, the predictor classes are carbon copies of Java. I figure, internally
// we will only waste time with niceties exposed to developers
Trap.Keepalive.StaticPredictor = function ()
{
	
	this.keepaliveInterval	= Trap.Keepalive.Policy.DISABLED;
	
	// Keepalive engine stuff
	/**
	 * The default keepalive interval.
	 */
	this.mKeepaliveInterval	= 30;
	
	/**
	 * Number of seconds to wait at least between keepalives
	 */
	this.minKeepalive		= 1;
	
	/**
	 * Number of seconds to wait at most between keepalives
	 */
	this.maxKeepalive		= 999999;
	
	// Automatic keepalive interval optimisation
	
	this.lastInterval		= this.mKeepaliveInterval;
	
	this.growthStep			= 0;

	this.nextInterval		= this.mKeepaliveInterval + this.growthStep;
	
	/**
	 * The minimum keepalive value that the automatic keepalive algorithm is
	 * allowed to decrease the keepalive to. The auto keepalive algorithm is
	 * only active on transports that can connect (i.e. reconnect) if it fails
	 * them.
	 */
	this.minAutoKeepalive	= 1;
	
	/**
	 * The minimum keepalive value that the automatic keepalive algorithm is
	 * allowed to decrease the keepalive to. The auto keepalive algorithm is
	 * only active on transports that can connect (i.e. reconnect) if it fails
	 * them. In addition, if keepalivepolicy != default, the automatic algorithm
	 * is disabled, as well as when min/max auto keepalives are negative
	 * numbers.
	 */
	this.maxAutoKeepalive	= 28 * 60;
	
	/**
	 * Timestamp of last recorded keepalive received
	 */
	this.lastDataReceived	= 0;
	this.lastDataSent	= 0;
	this.lastSentKeepalive		= 0;

	this.keepaliveTask			= null;
	this.keepaliveTaskTime		= 0;
	this.keepaliveExpiryMsec	= 5000;

	/**
	 * Byte array containing the most recently sent keepalive message from this
	 * predictor.
	 */
	this.keepaliveData			= null;
	
	this.started				= false;

	this.delegate = null;

	this.setMinKeepalive = function(min)
	{
		this.minKeepalive = min;
	}
	
	this.setMaxKeepalive = function(max)
	{
		this.maxKeepalive = max;
	}
	
	this.setMinAutoKeepalive = function(min)
	{
		this.minAutoKeepalive = min;
	}
	
	this.setMaxAutoKeepalive = function(max)
	{
		this.maxAutoKeepalive = max;
	}
	
	this.setKeepaliveInterval = function(interval)
	{
		this.keepaliveInterval = interval;
		
		if (interval == Trap.Keepalive.Policy.DEFAULT)
			this.nextInterval = this.mKeepaliveInterval;
		else if (interval == Trap.Keepalive.Policy.DISABLED)
			this.nextInterval = -1;
		else
		{
			// Basically, ensure that the interval is within the allowed range
			if ((interval > this.maxKeepalive) || (interval < this.minKeepalive))
				this.nextInterval = this.mKeepaliveInterval;
			else
				this.nextInterval = interval;
		}
		
	}
	
	this.getKeepaliveInterval = function()
	{
		return this.keepaliveInterval;
	}

	this.getNextKeepaliveSend = function()
	{
		// When do we expect the next keepalive?
		var earliestTime = Math.min(this.lastDataReceived, this.lastDataSent);
		var expected = earliestTime + (this.nextInterval * 1000);
		var actual = new Date().valueOf();
		var difference = expected - actual;
		
		return difference;
	}
	
	this.keepaliveReceived = function(isPing, pingType, timer, data)
	{
		if (!isPing)
		{

			// Check if this is a PING we have sent
			if (data != this.keepaliveData)
				return;
			
			this.keepaliveData = null;
			
			switch (pingType)
			{
			// Keepalives disabled

				case '1':
					break; // Do nothing; we will not auto-adjust
					
				case '2':
					this.setKeepaliveInterval(timer); // Manual adjustment
					break;
				
				case '3': // Manually triggered keepalive
					break;
				
				default: // no-error
			}
			
			// Now reschedule ourselves. The received keepalive will already have been recorded as per dataReceived()
			this.schedule();
		}
		else
		{
			this.delegate.get().shouldSendKeepalive(false, this.getPingType(), this.nextInterval, data);
		}
	}
	
	this.nextKeepaliveReceivedDelta = function()
	{
		
		if (this.keepaliveData == null)
			return Number.MAX_VALUE; // NEVER! We haven't sent a keepalive so we don't expect one. DUH.
		
		var expected = this.lastSentKeepalive;
		var actual = new Date().valueOf();
		return expected - actual;
	}
	
	this.setDelegate = function(delegate)
	{
		this.delegate = { get : function() { return delegate; } };
	}
	
	this.setKeepaliveExpiry = function(msec)
	{
		this.keepaliveExpiryMsec = msec;
		this.schedule();
	}
	
	this.getKeepaliveExpiry = function()
	{
		return this.keepaliveExpiryMsec;
	}
	
	this.start = function()
	{
		if (this.getKeepaliveInterval() == Trap.Keepalive.Policy.DISABLED)
			return;
		
		if (this.started)
		{
			this.schedule();
			return;
		}
		
		
		if (this.nextKeepaliveReceivedDelta() <= 0)
			this.lastReceivedKeepalive = new Date().valueOf();
		
		this.keepaliveData = null;
		this.lastSentKeepalive = 0;
		this.keepaliveTaskTime = 0;

		this.started = true;

		this.schedule();

	}
	
	this.stop = function()
	{
		
		if (!this.started)
			return;

		if (this.keepaliveTask != null)
		{
			this.keepaliveTask.cancel();
			this.keepaliveTask = null;
		}
		
		this.started = false;
	}
	
	this.schedule = function()
	{
		
		if (this.getKeepaliveInterval() == Trap.Keepalive.Policy.DISABLED)
			return;
		
		if (!this.started)
			return;
		
		// Next send should auto-disable if there is an outstanding ping/pong waiting
		var nextSend = (this.keepaliveData == null ? this.getNextKeepaliveSend() : Number.MAX_VALUE);
		var nextReceive = this.nextKeepaliveReceivedDelta() + (this.keepaliveData == null ? 0 : this.keepaliveExpiryMsec);
		var msec = Math.min(nextSend, nextReceive);
		
		if (msec <= 500)
		{
			//this.run();
			//return;
			msec = 501;
		} 
		
		var scheduledTime = msec + new Date().valueOf();
		
		// no-op: we want to schedule for longer time than the current expiry (expiry will re-schedule)
		// cancel: we want to schedule for shorter time than the current expiry
		if (this.keepaliveTask != null)
		{
			// Ensure we don't schedule if a task is going to happen closer, but in the future
			if ((this.keepaliveTaskTime <= (scheduledTime+250)) && (this.keepaliveTaskTime > new Date().valueOf()))
				return;
			
			this.keepaliveTask.cancel();
		}
		
		this.keepaliveTaskTime = scheduledTime;

		var mt = this;
		
		this.keepaliveTask = {
				run : function() { mt.run(); },
				cancel : function() { clearTimeout(this.timeout); }
		};
		
		this.keepaliveTask.timeout = setTimeout(this.keepaliveTask.run, msec);

	}
	
	this.run = function()
	{
		var delegate = this.delegate.get();
		
		if (delegate == null)
		{
			this.stop();
			return; // Delegate garbage collected; nothing to keep notifying about
		}
		
		// Check if we have been disabled...
		if (this.getKeepaliveInterval() == Trap.Keepalive.Policy.DISABLED)
		{
			this.stop();
			return;
		}
		// Now check for timeout
		
		var msec = this.nextKeepaliveReceivedDelta();
		
		if ((msec < 0) && (-msec > this.keepaliveExpiryMsec))
		{
			// Don't re-schedule this task on non-expired timeout
			delegate.predictedKeepaliveExpired(this, -msec);
			this.stop();
			return;
		}
		
		// Is it time to send a keepalive?
		
		msec = this.getNextKeepaliveSend();
		if (msec <= 0)
		{
			
			if (this.keepaliveData != null)
			{
				// OOps?
				//System.err.println("EXPERIMENTAL: keepalive data != null when expired timer... Dropping sending a keepalive.");
			}
			else
			{
				
				this.keepaliveData  = 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
				    var r = Math.random()*16|0, v = c == 'x' ? r : (r&0x3|0x8);
				    return v.toString(16);
				}); 
				this.lastSentKeepalive = new Date().valueOf();
				this.lastInterval = this.nextInterval;
				delegate.shouldSendKeepalive(true, this.getPingType(), this.nextInterval, this.keepaliveData);
				
			}
		}
		
		// reschedule ourselves for a default time
		this.keepaliveTaskTime = 0;
		
		var mt = this;
		
		// This timeout prevents us from super-recursing
		setTimeout(function() { mt.schedule(); }, 1);
	}
	
	this.getPingType = function()
	{
		var type = '1';
		
		if (this.getKeepaliveInterval() > 0)
			type = '2';
		
		return type;
	}
	
	this.dataReceived = function() {
		this.lastDataReceived = new Date().valueOf();
	};
	
	this.dataSent = function() {
		this.lastDataSent = new Date().valueOf();
	};
	
	
};
//<needs(trap)
/**
 * Circular buffer of TrapMessages and TrapTransports they arrived on. The
 * message buffer works using random inserts and regular reads. The buffer needs
 * to be initialised with a size (can be automatically increased) and initial
 * expected message id. The buffer will use this expected message ID to seed
 * itself with and be able to receive messages from history.
 * <p>
 * The buffer will automatically sort messages for reading. If messages are read
 * in order, there is no performance penalty for accesses. If messages come from
 * outside the buffer's range, there is a performance penalty, based on buffer
 * settings.
 * <p>
 * To put it another way, it is a self-growing, circular object buffer
 * implementing random write and sequential read.
 * 
 * @author Vladimir Katardjiev
 * @param {int} bufSize 
 * @param {int} maxBufSize 
 * @param {int} startMessageId 
 * @param {int} minMessageId 
 * @param {int} maxMessageId 
 */

//> (Trap.MessageBuffer) fn(int bufSize, int maxBufSize, int startMessageId, int minMessageId, int maxMessageId)
Trap.MessageBuffer = function(bufSize, maxBufSize, startMessageId, minMessageId, maxMessageId)
{

	/**
	 * @private
	 */
	this.buffer = new Array(bufSize);
	
	this.minMessageId = minMessageId;
	this.maxMessageId = maxMessageId;
	
	this.bufGrowthSize = bufSize;
	this.maxBufSize = maxBufSize;
	
	this.readMessageID = this.writeMessageID = startMessageId;
	
	this.fillEmptyBuf = function(buffer)
	{
		for (var i=0; i<buffer.length; i++)
		{
			var m = buffer[i];
			
			if (m == null)
				buffer[i] = {m: null, t: null};
		}
	};
	
	this.fillEmptyBuf(this.buffer);
	
	this.available = function()
	{
		return this.writeMessageID - this.readMessageID;
	};

	this.put = function(m, t)
	{
		
		// Step 1: Input validation.
		var messageId = m.getMessageId();
		
		if (messageId > this.maxMessageId || messageId < this.minMessageId)
			throw "Message ID [" + messageId + "] outside of acceptable range [" + this.minMessageId + ", " + this.maxMessageId + "].";
		
		// Message IDs can be reused (and reusing them won't cleanly fit in the buffer.
		// In those wrapping cases, we'll need to up the effective messageId appropriately.
		// TODO: Better constant?
		if (messageId < this.readMessageID)
		{
			if  ((this.readMessageID - messageId) > (this.maxMessageId - this.minMessageId) / 2)
				messageId += this.maxMessageId - this.minMessageId + 1;
			else
				return; // Skip duplicated message.
		}
		
		// Assert that the message has a chance at fitting inside the buffer
		if (messageId > (this.readMessageID + this.maxBufSize))
			throw "Message ID [" + messageId + "] outside of buffer size. First message has ID [" + this.readMessageID + "] and max buffer size is " + this.maxBufSize;
		
		// Assert the message has not already been written.
		if (messageId < this.writeMessageID)
			return; // This should be a safe operation. It just means the message is duplicated.
			
		// At this point in time we know that:
		// 1) The message will fit in the buffer [writeMessageID, readMessageId+maxBufSize]
		// 2) The message is in that range and has not already been written.
		// We now need to ensure the buffer is large enough
		// This is a STRICT equality. Proof: buffer.length == 1, buffer[0] != null => buffer is full
		if (messageId >= (this.readMessageID + this.buffer.length))
		{
			// This is a simple operation. Resize the old buffer into a new one by flushing out the current messages.
			var newSize = messageId - this.readMessageID;
			// The new size should be a multiple of bufGrowthSize
			newSize /= this.bufGrowthSize;
			newSize++;
			newSize *= this.bufGrowthSize;
			
			var newBuf = new Array(newSize);
			this.fillEmptyBuf(newBuf);
			
			// Move all slots from the old buffer to the new one, recalculating the modulus as applicable.
			// We have to move all slots as we don't track which ones have been filled.
			for (var i = 0; i < this.buffer.length; i++)
			{
				var tmp = this.buffer[i];
				if (tmp.m != null)
					newBuf[tmp.m.getMessageId() % newBuf.length] = tmp;
			}
			
			this.buffer = newBuf;
			
		}
		
		// Where are we now? Well, that's the rad part. We now know that messageId will comfortably fit in our world so all we need to do is fill it.
		var slot = this.buffer[messageId % this.buffer.length];
		slot.m = m;
		slot.t = t;
		
		//System.out.println("Wrote message with ID " + messageId + " and expected ID " + writeMessageID);
		
		// Final step is to increment the writeMessageId entry, if applicable.
		if (messageId == this.writeMessageID)
		{
			do
			{
				var expectedMessageId = this.writeMessageID;
				
				if (expectedMessageId > this.maxMessageId)
					expectedMessageId -= this.maxMessageId - this.minMessageId + 1;
				
				// Bug catch verification. Logically, writeMessageID should be the message ID of the current slot's message. If they don't match, we're in deep doodoo
				if (slot.m.getMessageId() != expectedMessageId)
					throw "Trap Message Buffer corrupted. Unexpected message ID found. This needs debugging...";
				
				// Increment by one.
				this.writeMessageID++;
				// Fetch the next entry
				slot = this.buffer[this.writeMessageID % this.buffer.length];
				
			} while (slot.m != null && (this.writeMessageID - this.readMessageID) < this.buffer.length);
		}

	};
	
	this.fetch = function(target, skipEmpty)
	{
		// Nothing to read here, move along...
		if (this.readMessageID >= this.writeMessageID)
		{
			target.m = null;
			target.t = null;
			return false;
		}
		try
		{
			var m = null;
			for (;;)
			{
				
				m = this.buffer[this.readMessageID % this.buffer.length];
				
				if (m.m != null)
				{
					//System.out.println("Read message with ID " + m.m.getMessageId() + " and expected ID " + this.readMessageID);
					this.readMessageID++;
					target.m = m.m;
					target.t = m.t;
					m.m = null;
					m.t = null;
					return true;
				}
				else
				{
					if (skipEmpty && this.readMessageID < this.writeMessageID)
					{
						this.readMessageID++;
					}
					else
					{
						return false;
					}
				}
			}
		}
		catch(e)
		{
			throw e;
		}
		finally
		{
			// If we have wrapped around the messages, we can finally throw ourselves a bone and reduce the message IDs to handle wrapping gracefully.
			if (this.readMessageID > this.maxMessageId)
			{
				// The easiest way is to just create a new buffer and refill it.
				// This is a fairly expensive operation, but it should only happen once every billion messages or so, so we can consider
				// the cost amortized.
				var newBuf = new Array(this.buffer.length);
				this.fillEmptyBuf(newBuf);
				
				this.readMessageID -= this.maxMessageId - this.minMessageId + 1;
				this.writeMessageID -= this.maxMessageId - this.minMessageId + 1;
				
				// Recalculation can and should be based on the message IDs. This prevents us from doing expensive errors.
				for (var i = 0; i < newBuf.length; i++)
				{
					var tmp = this.buffer[i];
					if (tmp.m != null)
						newBuf[tmp.m.getMessageId() % newBuf.length] = tmp;
				}
				
				this.buffer = newBuf;
			}
		}
	};
	
};

//< needs(compat)

/**
 * Do not instantiate an endpoint directly. Instead, use Trap.ClientEndpoint or Trap.ListenerEndpoint.
 * 
 * @classdesc
 * The main interface to Trap, a TrapEndpoint is the shared interface between servers and clients. It provides most of
 * the methods that should be necessary to configure and use a Trap connection.
 * <p>
 * In general, a TrapEndpoint should be configured either when it has just been created.Reconfiguring a TrapEndpoint
 * while it is in use can have unintended consequences.
 * <p>
 * Common tasks on a TrapEndpoint are:
 * <h3>Configuring Transports</h3>
 * <p>
 * Enabling and disabling transports, as well as configuring each transport, is possible from TrapEndpoint. Adding new
 * transports to an existing endpoint is not possible.
 * <h3>Sending and Receiving Messages</h3>
 * <p>
 * To send messages, simply use {@link Trap.Endpoint#send} to send data. When data is received, it will be received in the onmessage listener.
 * <h3>Checking Liveness</h3>
 * <p>
 * Trap provides a simple facility to check if the other endpoint is alive, that is, communication is active and the
 * other application layer is responding. The {@link Trap.Endpoint#isAlive} method can be used to check if the
 * endpoint has been alive recently, or a combination of the two.
 * <h3>Configuring Keepalives</h3>
 * <p>
 * By default, Trap will attempt to use a per-transport keepalive policy that strikes a moderate balance between
 * liveness and chattiness. It will take into account traffic only on the current TrapTransport, and without using
 * device integration. This simple implementation can be tweaked to use a static keepalive interval instead (every X
 * milliseconds), or disabled, using the {@link #setKeepaliveInterval(int)} method. More advanced keepalives can be
 * user-supplied on a transport basis using {@link TrapTransport#setKeepalivePredictor(TrapKeepalivePredictor)}.
 * <h3>Customizing the Message Queue</h3>
 * <p>
 * Trap has a number of buffers it uses, and some (such as the message queue) can impact performance significantly under
 * different usage patterns. The message queue is the first front-end that feeds Trap, and plays a large role. An
 * endpoint can be instructed to either choose the "best" buffer available of a given type (see constants) using
 * {@link #setQueueType(String)}, or it can be told to explicitly use a specific queue using
 * {@link #setQueue(MessageQueue)}. User-supplied queues can be use, as long as they fulfil the requirements.


 * @constructor
 * @property {Trap.Endpoint.State} state The current Trap.Endpoint.State
 * @property {Trap.Endpoint.Queue} queueType The message queue type
 * @property {Integer} maxActiveTransports The maximum number of active transports. Default is 1.
 * @property {Trap.Authentication} authentication The current authentication provider 
 * @property {Trap.Endpoint.Format} format The message format used. Default is 8-bit.
 * 
 */
Trap.Endpoint = function()
{
	Trap.EventObject.constructor.call(this);
	this.transportsMap = new Trap.Map();
	this.transports = new Trap.List();
	this.config = new Trap.Configuration();
	
	this.availableTransports = new Trap.List();
	this._state = Trap.Endpoint.State.CLOSED;
	
	this.channels = new Array();
	this.messageQueue = new Trap.ChannelMessageQueue();
	
	this.messageQueueType = Trap.Endpoint.Queue.REGULAR;
	
	this._maxActiveTransports = 65535;
	
	this.sending = false;
	
	this.trapID = Trap.Constants.ENDPOINT_ID_UNDEFINED;
	this.trapFormat = (this.useBinary ? Trap.Message.Format.REGULAR : Trap.Message.Format.SEVEN_BIT_SAFE);
	
	this._authentication = new Trap.Authentication();
	this.logger = Trap.Logger.getLogger("TrapEndpoint"); // The number of
	// milliseconds
	// async mode is
	// allowed to wait
	// for messages (to
	// reorder)
	
	// Timeouts & Keepalives.
	/**
	 * Last known activity of the connection. Activity is defined as any form of
	 * message FROM the client. In general, the TrapEndpoint will not concern
	 * itself with ensuring this.value is continually updated, as that is mostly
	 * unnecessary overhead. It will update it during the following conditions:
	 * <p>
	 * <ul>
	 * <li>A transport disconnects. Even in this.case, the lastActivity field
	 * will only represent some most recent communication with the remote side,
	 * unless all transports have disconnected.
	 * <li>The application specifically queries. In this.case, the TrapEndpoint
	 * will specifically ensure that lastActivity has the most recent value.
	 * </ul>
	 */
	this._lastAlive = 0;
	
	/**
	 * The last known timestamp where we can reliably wake up the underlying
	 * transports. If we have a wakeup mechanism, this.will be a non-negative
	 * value, and represents when we can unilaterally tell the application the
	 * connection is permanently dead (unless we can extend the wakeup
	 * mechanism).
	 */
	this.canWakeupUntil = 0;
	
	/**
	 * The last permitted timestamp for the client to re-establish connectivity.
	 * this.must be equal to or greater than canWakeupUntil, in order to
	 * maintain the same promise to the application.
	 */
	this.canReconnectUntil = 0;
	
	/**
	 * The number of milliseconds that the endpoint should wait for a response
	 * (and/or attempt to reconnect/resend) to do an orderly close. After this
	 * time, the transport will simply deallocate all of its resources and
	 * vanish.
	 */
	this.keepaliveExpiry = 5000;
	this.keepaliveInterval = Trap.Keepalive.Policy.DEFAULT;
	this.keepaliveTask = null;
	
	this.reconnectTimeout = 180000;
	this.async = true;
	this.compressionEnabled = this.useBinary;
	
	Trap._compat.__defineGetter(this, "state", function()
	{
		return this._state;
	});
	
	Trap._compat.__defineGetter(this, "queueType", function()
	{
		return this.messageQueueType;
	});
	
	Trap._compat.__defineSetter(this, "queueType", function(t)
	{
		this.messageQueueType = t;
	});
	
	Trap._compat.__defineGetter(this, "maxActiveTransports", function()
	{
		return this._maxActiveTransports;
	});
	
	Trap._compat.__defineSetter(this, "maxActiveTransports", function(l)
	{
		this._maxActiveTransports = l;
	});
	
	Trap._compat.__defineGetter(this, "authentication", function()
	{
		return this._authentication;
	});
	
	Trap._compat.__defineSetter(this, "authentication", function(a)
	{
		this._authentication = a;
		
		for ( var i = 0; i < this.transports.size(); i++)
			this.transports.get(i).setAuthentication(a);
	});
	
	Trap._compat.__defineGetter(this, "format", function()
	{
		return this.trapFormat;
	});
	
	Trap._compat.__defineSetter(this, "format", function(f)
	{
		this.trapFormat = f;
		
		var it = this.transports.iterator();
		
		while (it.hasNext())
			it.next().setFormat(f);
	});
	
	if (this.useBinary)
	{
		
		if (Trap._useGetters)
		{
			this._dispatchMessageEvent = function(message)
			{
				var evt = new Trap._GetterMessageEvent(message);
				this._dispatchEvent(evt);
			};
		}
		else
		{
			// Fallback approach
			this._dispatchMessageEvent = function(message)
			{
				var evt = {
					type : "message",
					message : message.getData(),
					// data: message.getData(),
					dataAsString : message.getString(),
					// string: message.getString(),
					buffer : message.getData().buffer.slice(message.data.byteOffset, message.data.byteOffset + message.data.byteLength),
					compression: message.getCompression(),
					channel: message.getChannel(),
					object: JSON.parse(message.getString())
				};
				
				// Remove redundant calls to message to increase performance.
				evt.data = evt.message;
				evt.string = evt.dataAsString;
				
				this._dispatchEvent(evt);
			};
		}
	}
	else
	{
		this._dispatchMessageEvent = function(message)
		{
			var evt = {
				type : "message",
				message : message.getString(),
				channel: message.getChannel()
			// data: message.getString(),
			// dataAsString: message.getString(),
			// string: message.getString()
			};
			
			evt.data = evt.message;
			evt.dataAsString = evt.data;
			evt.string = evt.data;
			
			this._dispatchEvent(evt);
		};
	}
	
	this.channels[0] = new Trap.Channel(this, 0);
	this.channels[0].setPriority(Number.MAX_VALUE);
	
	this.messageQueue.rebuild(this.channels);
	
};
Trap.Endpoint.prototype = new Trap.EventObject;
Trap.Endpoint.prototype.constructor = Trap.Endpoint;

/**
 * @namespace
 */
Trap.Endpoint.State = {
	CLOSED : "Trap.Endpoint.State.CLOSED",
	OPENING : "Trap.Endpoint.State.OPENING",
	OPEN : "Trap.Endpoint.State.OPEN",
	SLEEPING : "Trap.Endpoint.State.SLEEPING",
	ERROR : "Trap.Endpoint.State.ERROR",
	CLOSING : "Trap.Endpoint.State.CLOSING"
};

	/**
	 * @namespace
	 */
Trap.Endpoint.Queue = {
	REGULAR : "Trap.Endpoint.Queue.REGULAR"
};

/* Settings methods */
  /**
     * Enables a transport with a given name. Enabling a transport merely switches the enabled flag of the transport in
     * the resident Trap configuration. It does not necessarily connect it.
     * <p>
     * Enabling a transport after a client endpoint has been asked to connect will cause the transport to be ignored
     * until the reconnect procedure is triggered. For this reason, it is recommended that enabling/disabling transports
     * is done before an endpoint is connected.
     * 
     * @param {String} transportName
     *            The transport to enable.
     * @throws TrapException
     *             If the transport does not exist. This exception is thrown to prevent applications that rely on a
     *             certain transport from being surprised when the transport never connects. A typical case is a client
     *             disabling the http transport when websockets is not available.
     */
Trap.Endpoint.prototype.enableTransport = function(transportName)
{
	if (this.isTransportEnabled(transportName)) return;
	
	this.getTransport(transportName).enable();
};
    

    /**
	 * Disables a transport. Unlike {@link #enableTransport(String)}, this
	 * method will always succeed, even if the transport does not exist. This
	 * allows applications to safely remove transports they do not wish to use
	 * – if the transport does not exist, it will not be used.
	 * 
	 * @param {String} transportName The transport to disable
	 */
Trap.Endpoint.prototype.disableTransport = function(transportName)
{
	if (!this.isTransportEnabled(transportName)) return;
	
	this.getTransport(transportName).disable();
};

    /**
	 * Convenience method to disable all transports at a given configurable
	 * object. This is primarily used to create controlled-environment Trap
	 * connections, where the available transports are not automatically set.
	 * Primarily targeted towards tests or other controlled environments, or
	 * when all transports need to be clamped down.
	 */
Trap.Endpoint.prototype.disableAllTransports = function()
{
	for ( var i = 0; i < this.transports.size(); i++)
		this.transports.get(i).disable();
};

    /**
     * Queries if a transport is enabled. Preferable to calling {@link #getTransport(String)} and
     * {@link TrapTransport#isEnabled()} as this method will not throw if a transport does not exist.
     * 
     * @param transportName
     *            The transport whose state to query.
     * @return <i>true</i> if a transport exists, and is enabled, <i>false</i> otherwise.
     */
Trap.Endpoint.prototype.isTransportEnabled = function(transportName)
{
	try
	{
		return this.getTransport(transportName).isEnabled();
	}
	catch (e)
	{
		return false;
	}
};

    /**
     * Gets the configuration of this endpoint. This is the configuration as it applies for this node; it does not
     * represent the configuration another endpoint needs to have to connect here. See
     * {@link TrapListener#getClientConfiguration()} for that. This method is useful for debugging.
     * 
     * @return {String} A string representing the current configuration of this TrapEndpoint.
     */
Trap.Endpoint.prototype.getConfiguration = function()
{
	return this.config.toString();
};

Trap.Endpoint.prototype.parseConfiguration = function(configuration)
{
	return new Trap.Configuration(configuration);
};
    /**
	 * Configures this TrapEndpoint, overwriting any previous configuration, and
	 * setting the new string to the new configuration. This will also
	 * reconfigure any constituent transports. This method should be used before
	 * using any of the programmatic configuration methods, as it may override
	 * them.
	 * 
	 * @param {String} configuration A string representing the new
	 *            configuration.
	 */
Trap.Endpoint.prototype.configure = function(configuration)
{
	this.config = this.parseConfiguration(configuration);
	
	// Iterate over all transports
	for ( var i = 0; i < this.transports.size(); i++)
		this.transports.get(i).setConfiguration(this.config);
	
	var option = this.config.getIntOption("trap.keepalive.interval", this.keepaliveInterval);
	this.setKeepaliveInterval(option);
	
	option = this.config.getIntOption("trap.keepalive.expiry", this.keepaliveExpiry);
	this.setKeepaliveExpiry(option);
	
	this.compressionEnabled = this.config.getBooleanOption(Trap.Constants.OPTION_ENABLE_COMPRESSION, this.compressionEnabled);
};

    /**
	 * Changes the configuration of a given transport. This is an alias to
	 * accessing the transport and configuring it directly. After configuration,
	 * the transport settings will be updated.
	 * <p>
	 * Care should be taken when using this method to not conflict with Trap's
	 * general management. In most cases, an endpoint will manage transport
	 * settings automatically, although some tweaks can be done on a
	 * per-transport basis.
	 * <p>
	 * General configuration keys can be found as static properties in
	 * {@link TrapTransport}. Specific options are relevant only to the given
	 * transport.
	 * 
	 * @param transportName The name of the transport to configure.
	 * @param configurationKey The <i>unprefixed</i> configuration key
	 * @param configurationValue The new value of the key.
	 * @throws TrapException If the transport does not exist (and thus cannot be
	 *             configured).
	 */
Trap.Endpoint.prototype.configureTransport = function(transportName, configurationKey, configurationValue)
{
	this.getTransport(transportName).configure(configurationKey, configurationValue);
};
    /**
	 * Gets the current set of transports associated with this TrapEndpoint.
	 * These transports represent all the instances available to the endpoint,
	 * not necessarily the ones that are currently in use. Each transport has an
	 * individual state that determines if it is connected or not.
	 * 
	 * @return A collection of transports associated with this endpoint.
	 */
Trap.Endpoint.prototype.getTransports = function()
{
	return this.transports;
};

    /**
     * Accesses a single transport by name. This is useful for advanced configuration of transports, debugging or highly
     * specialised tweaking.
     * 
     * @param transportName
     *            The name of the transport.
     * @return The {@link TrapTransport} instance representing the transport
     * @throws TrapException
     *             If the transport does not exist.
     */
Trap.Endpoint.prototype.getTransport = function(transportName)
{
	
	var t = this.transportsMap.get(transportName);
	
	if (t == null) throw "Unknown Transport";
	
	return t;
};

Trap.Endpoint.prototype.addTransport = function(t, message)
{
	
	if (!t.canConnect() && !t.canListen() && t.getState() == Trap.Transport.State.DISCONNECTED)
	{
		this.logger.debug("Attempting to add transport class [{}] for handler [{}] that can neither connect nor listen. Skipping...", t, t.getTransportName());
		return false;
	}
	
	var old = this.transportsMap.get(t.getTransportName());
	
	if (old != null)
	{

		var oldPrio = old.getTransportPriority();
		var newPrio = t.getTransportPriority();
		
		// Strict lesser equality. This allows us to be replaced by, well, the same transport.
		if (oldPrio < newPrio)
		{
			this.logger.debug("Attempting to add new handler for [{}] when the old handler had a higher priority. New class was [{}]/{}, old class was [{}]{}. Skipping...", t.getTransportName(), t.getClass().getName(), t.getTransportPriority(), old.getClass().getName(), old.getTransportPriority() );
			return false;
		}
		
		this.transports.remove(old);
	}

	this.transportsMap.put(t.getTransportName(), t);
	this.transports.add(t);
	t.setFormat(this.getFormat());
	
	// Hook the delegate methods
	var mt = this;
	t.onmessage = function(e)
	{
		mt.ttMessageReceived(e.message, t, null);
	};
	t.onmessagesent = function(e)
	{
		mt.ttMessageSent(e.message, t, null);
	};
	t.onstatechange = function(e)
	{
		mt.ttStateChanged(e.newState, e.oldState, t, null);
	};
	t.onfailedsending = function(e)
	{
		mt.ttMessagesFailedSending(e.messages, t, null);
	};
	
	// See public synchronized void addTransport(TrapTransport t, TrapMessage
	// message)
	// Used to add a transport to a listener.
	if (message)
	{
		
		t.setConfiguration(this.config);
		t.setAuthentication(this.authentication);
		t.setFormat(this.getFormat());

		var l = new Trap.List(this.availableTransports);
		var aliveCB =  function(i) {
			if (i >= l.size())
				return;
			
			var t = l.get(i);
			t.isAlive(0, true, true, 4000, function(rv) {
				if (!rv)
					t.forceError();
				else if (t.getState() == Trap.Transport.State.AVAILABLE)
					this.addTransportToAvailable(t);
			});
			
		};
		
		if (t.getState() == Trap.Transport.State.AVAILABLE)
		{
			// This, in general, means we get a second transport on an existing session. We should re-check the liveness of the existing transports, in case
			// this is a disconnect
			
			// We should temporarily clear the available transports.
			this.availableTransports.clear();
			aliveCB(0);
			this.addTransportToAvailable(t);
		}
		else
		{
			// The second case is trickier. We get a new unavailable transport (=sporadic availability). We can't make any assumptions
			// but it is nevertheless wise to check the available transports.
			aliveCB(0);
		}
		
		// Trigger incoming message (=OPEN) in order to reply properly.
		this.ttMessageReceived(message, t, null);
	}
};

Trap.Endpoint.prototype.setTrapID = function(newId)
{
	this.trapID = newId;
};

Trap.Endpoint.prototype.getTrapID = function()
{
	return this.trapID;
};

Trap.Endpoint.prototype.removeTransport = function(t)
{
	this.transportsMap.remove(t.getTransportName());
	this.transports.remove(t);
};

    /**
     * Closes this Trap endpoint, terminating any outstanding Trap transports. Does nothing if the endpoint is already
     * closed, is closing, or is in an error state.
     */
Trap.Endpoint.prototype.close = function()
{
	if (this.getState() != Trap.Endpoint.State.OPEN)
	{
		// We can't close a non-open connection.
		
		if (this.getState() == Trap.Endpoint.State.SLEEPING)
		{
			// TODO: We should WAKE UP then DISCONNECT.
			// Since SLEEPING is NYI, we'll leave this
			this.setState(Trap.Endpoint.State.CLOSING);
			this.onEnd(null, null);
		}
		else
		{
			if (this.getState() == Trap.Endpoint.State.CLOSING || this.getState() == Trap.Endpoint.State.CLOSED)
			{
				// Harmless call.
				return;
			}
			
			if (this.getState() == Trap.Endpoint.State.ERROR)
			{
				// Technically harmless call, but we will log it to point out
				// potential laziness in the coding of the error handling of our
				// parent.
				this.logger.debug("Called close() on an endpoint in state ERROR. This might be caused by recovery code shared between regular and normal states");
				return;
			}
			
			if (this.getState() == Trap.Endpoint.State.OPENING)
			{
				// TODO: This one is troublesome. close() has been called on a
				// connection that is opening().
				// I think we can handle it normally (i.e. switch to closing and
				// just end()) but it might be worth investigating
				// We will log.
				this.logger.debug("Called close() on an endpoint in state OPENING. This message is logged for debug purposes (if we don't fully close).");
			}
		}
	}
	this.setState(Trap.Endpoint.State.CLOSING);
	
	// We'll send END to the other side
	// After that has happened, we'll close (in onend)
	
	try
	{
		this.sendMessage(this.createMessage().setOp(Trap.Message.Operation.END));
	}
	catch (e)
	{
		this.logger.error("Setting Trap.Endpoint.State to ERROR due to an error while disconnecting that may have left the implementation in an inconsistent state");
		this.setState(Trap.Endpoint.State.ERROR);
		// TODO: Cleanup/recovery?
	}
	;
};

/**
 * Attempts to queue data for sending. If the queue length is exceeded, it may
 * block or throw an exception, as per the queue type.
 * <p>
 * Please note that while send will accurately send the data over to the other
 * endpoint, it is advisable to instead use {@link #send(TrapObject)} if the
 * data being sent is a serialized object. If the other endpoint is locally
 * deployed, the TrapObject will never be serialized, thus saving on large
 * amounts of processing power.
 * 
 * @param {Object|String} data The data to send. If an object, is serialised as JSON automatically.
 * @param {Number}
 *            channel The channel to send on
 * @param {Boolean}
 *            useCompression Whether to use compression for this message or not.
 * @throws TrapException
 *             if the queue length is exceeded, or a timeout occurs on a
 *             blocking queue
 */
Trap.Endpoint.prototype.send = function(data, channel, useCompression)
{
	var m = this.createMessage().setOp(Trap.Message.Operation.MESSAGE).setData(data);
	
	if (useCompression)
		m.setCompressed(useCompression && this.compressionEnabled && this.useBinary);
	else
		m.setCompressed(false);
	
	if (typeof (channel) == "number") m.setChannel(channel);
	
	this.sendMessage(m);
};



Trap.Endpoint.prototype.sendMessage = function(message)
{
	// All other states do not allow the sending of messages.
	if (this.getState() != Trap.Endpoint.State.OPEN && message.getOp() != Trap.Message.Operation.END && this.getState() != Trap.Endpoint.State.SLEEPING) throw "Tried to send to non-open Trap session";
	
	var channel = this.getChannel(message.getChannel());
	channel.send(message);
	this.kickSendingThread();
};

Trap.Endpoint.prototype._sendFun = function()
{
	try
	{
		
		for (;;)
		{
			
			// Unlike Java, we don't need to check for a message queue rebuild here.
			
			var first = null;
			if (this.messageQueue.peek() != null)
			{
				try
				{
					first = this.availableTransports.get(0);
				}
				catch (t)
				{
				}
				if (first != null)
				{
					while (first.isAvailable())
					{
						try
						{
							var m = this.messageQueue.peek();
							if (m == null || typeof (m) == "undefined") break;
							
							this.logger.debug("Attempting to send message with op {}", m.getOp());
							
							first.send(m, true);
							this.messageQueue.pop();
						}
						catch (e)
						{
							this.logger.debug(e);
							
							// What should happen if we get an exception here?
							// We
							// don't want this loop to continue, that's for
							// sure.
							// The first transport is clearly inadequate for the
							// task.
							if (first.isAvailable())
							{
								// Now, the problem here is that the regular API
								// only allows us to do a graceful disconnect.
								// If we do that, though, recovery code won't be
								// initialised.
								this.logger.warn("Forcibly removing transport {} from available due to infinite loop protection. This code should not occur with a well-behaved transport.", first.getTransportName());
								this.logger.warn("Caused by {}", e);
								
								first.forceError();
							}
							else
							{
								// Transport is no longer unavailable, loop
								// should
								// be broken.
							}
						}
						
					}
					
					if (first.isAvailable())
						first.flushTransport();
				}
			}
			if (this.messageQueue.peek() == null || first == null)
			{
				this.sending = false;
				return;
			}
		}
	}
	catch (t)
	{
		this.logger.error(t);
	}
	finally
	{
		this.messageQueue.rewind();
	}
};

Trap.Endpoint.prototype.kickSendingThread = function()
{
	if (!this.sending)
	{
		this.sending = true;
		var mt = this;
		setTimeout(function()
		{
			mt._sendFun();
		}, 10);
	}
};

Trap.Endpoint.prototype.ttStateChanged = function(newState, oldState, transport)
{
	if (newState == Trap.Transport.State.AVAILABLE)
	{
		this.addTransportToAvailable(transport);
		this.kickSendingThread();
		return;
	}
	
	// newState is NOT available. Remove the transport from availableTransports,
	// if it was there
	this.availableTransports.remove(transport);
	
	// Now we'll enter failure modes.
	if (newState == Trap.Transport.State.DISCONNECTED || newState == Trap.Transport.State.ERROR)
	{
		if (this.getState() == Trap.Endpoint.State.CLOSED || this.getState() == Trap.Endpoint.State.CLOSING)
		{
			
			// Make sure we update our state properly when all transports have
			// disconnected.
			if (this.getState() == Trap.Endpoint.State.CLOSING)
			{
				
				// Verify if this was the last open transport.
				for ( var i = 0; i < this.transports.size(); i++)
				{
					var t = this.transports.get(i);
					if (t.getState() != Trap.Transport.State.ERROR && t.getState() != Trap.Transport.State.DISCONNECTED) return; // If
				}
				
				this.setState(Trap.Endpoint.State.CLOSED);
				
			}
		}
	}
};

// Abstract method (for subclass usage)
Trap.Endpoint.prototype.reconnect = function(timeout, callback)
{
};

// These callbacks replace the Delegate pattern used in Java.
/**
 * Called when the Trap endpoint has received byte data from the other end.
 * this.method executes in a Trap thread, so it should only perform minimal
 * operations before returning, in order to allow for maximum throughput.
 * 
 * @param {ArrayBuffer} evt.buffer The bytes received
 * @param {String} evt.string A string constructed by parsing the bytes received as UTF-8
 * @param {Object} evt.object An object formed by parsing the string as JSON.
 * @param {Integer} evt.channel The Trap Channel the event was received on.
 */
Trap.Endpoint.prototype.onmessage = function(evt)
{
};

/**
 * Called when Trap changes state. Includes both the new state, and the previous
 * one.
 * 
 * @param evt.newState
 *            The state Trap changed to.
 * @param evt.oldState
 *            The previous state.
 */
Trap.Endpoint.prototype.onstatechange = function(evt)
{
};

/**
 * Called when a Trap Endpoint knows it has failed to send some messages.
 * this.can occur when the Trap Endpoint is killed forcibly, loses all its
 * transports while still having an outgoing buffer, or fails to wake up a
 * client that has disconnected all its transports normally.
 * <p>
 * Note that there are conditions when Trap may unwittingly lose data (such as
 * data sent during a switch from unauthenticated -> authenticated session, when
 * the authentication is triggered from the remote side), so the sum of data
 * received by the other end, and called on this.method, may be different.
 * Nevertheless, any data returned by this.method definitely failed to send.
 * 
 * @param evt.datas
 *            A collection of transportable objects that failed sending. Usually
 *            byte arrays, but may contain TrapObject instances.
 */
Trap.Endpoint.prototype.onfailedsending = function(evt)
{
};

/* Internal methods follow */

Trap.Endpoint.prototype.createMessage = function()
{
	return new Trap.Message().setFormat(this.trapFormat);
};

Trap.Endpoint.prototype.addTransportToAvailable = function(t)
{
	
	var added = false;
	
	for ( var i = 0; i < this.availableTransports.size(); i++)
	{
		var c = this.availableTransports.get(i);
		
		// Priority goes from negative to positive (most to least preferred)
		if (c.getTransportPriority() > t.getTransportPriority())
		{
			this.availableTransports.add(i, t);
			added = true;
			break;
		}
		else if (c == t)
			return; // don't double add
	}
	
	if (!added) this.availableTransports.addLast(t);
	
	if (this.availableTransports.size() > this.maxActiveTransports)
	{
		var t = this.availableTransports.getLast();
		this.logger.debug("Disconnecting transport [{}] as the max active transports were exceeded. ({} active, {} max)", t.getTransportName(), this.availableTransports.size(), this._maxActiveTransports);
		t.disconnect();
	}
};

Trap.Endpoint.prototype.ttMessageReceived = function(message, transport)
{
	
	this.logger.debug("Received message with op {}", message.getOp());
	if (this.async && (message.getMessageId() != 0))
	{
		this.getChannel(message.getChannel()).receiveMessage(message, transport);
	}
	else
	{
		this.executeMessageReceived(message, transport);
	}
};

Trap.Endpoint.prototype.executeMessageReceived = function(message, transport)
{
	switch (message.getOp())
	{
		case 1:
			this.onOpen(message, transport);
			break;
		
		case 2:
			this.onOpened(message, transport);
			break;
		
		case 3:
			this.onClose(message, transport);
			break;
		
		case 4:
			this.onEnd(message, transport);
			break;
		
		case 5:
			this.onChallenge(message, transport);
			break;
		
		case 6:
			this.onError(message, transport);
			break;
		
		case 8:
			this.onMessage(message, transport);
			break;
		
		case 16:
			this.onOK(message, transport);
			break;
		
		case 17:
			this.onPing(message, transport);
			break;
		
		case 18:
			this.onPong(message, transport);
			break;
		
		case 19:
			this.onTransport(message, transport);
			break;
		
		default:
			return;
			
	}
	
};

Trap.Endpoint.prototype.onTransport = function(message, transport)
{
	// Transport specific messages. May require us to reconfigure a different
	// transport.
	// This is our hook for future extensions.
};

/*
 * Ping/Pong events are generally a transport-specific concern. The events will
 * be received by the TrapEndpoint, but handled by the transports.
 */
Trap.Endpoint.prototype.onPong = function(message, transport)
{
};

Trap.Endpoint.prototype.onPing = function(message, transport)
{
};

/*
 * An OK will acknowledge a successful operation. This should be a TODO...
 */
Trap.Endpoint.prototype.onOK = function(message, transport)
{
};

Trap.Endpoint.prototype.onMessage = function(message, transport)
{
	this._dispatchMessageEvent(message);
};

Trap._GetterMessageEvent = function(message)
{
	this._orig = message;
	this.type = "message";
};

Trap._compat.__defineGetter(Trap._GetterMessageEvent.prototype, "message", function()
{
	return this._orig.data;
});

Trap._compat.__defineGetter(Trap._GetterMessageEvent.prototype, "data", function()
{
	return this._orig.data;
});

Trap._compat.__defineGetter(Trap._GetterMessageEvent.prototype, "string", function()
{
	return this._orig.string;
});

Trap._compat.__defineGetter(Trap._GetterMessageEvent.prototype, "channel", function()
{
	return this._orig.channel;
});

Trap._compat.__defineGetter(Trap._GetterMessageEvent.prototype, "dataAsString", function()
{
	return this._orig.string;
});

Trap._compat.__defineGetter(Trap._GetterMessageEvent.prototype, "object", function()
{
	return JSON.parse(this._orig.string);
});

Trap._compat.__defineGetter(Trap._GetterMessageEvent.prototype, "buffer", function()
{
	return this._orig.message.data.buffer.slice(message.data.byteOffset, message.data.byteOffset + message.data.byteLength);
});

/*
 * Errors should be handled. Onerror will most likely mean that the connection
 * has reached an unrecoverable state and must be discarded. The application
 * MUST be notified of this state.
 */
Trap.Endpoint.prototype.onError = function(message, transport)
{
	this.setState(Trap.Endpoint.State.ERROR);
};

Trap.Endpoint.prototype.onChallenge = function(message, transport)
{
};

Trap.Endpoint.prototype.onEnd = function(message, transport)
{
	
	if (this.getState() == Trap.Endpoint.State.CLOSING)
	{
		
		for ( var i = 0; i < this.transports.size(); i++)
			this.transports.get(i).disconnect();
		
		this.setState(Trap.Endpoint.State.CLOSED);
		
		// TODO: Should this do some more cleanup here? Can we reopen this
		// object? If we can't reopen, should we note it in the state somehow?
	}
	else
	{
		this.setState(Trap.Endpoint.State.CLOSING);
		try
		{
			this.sendMessage(this.createMessage().setOp(Trap.Message.Operation.END));
		}
		catch (e)
		{
			// TODO: Can we handle this error gracefully-er?
			this.logger.warn(e);
			
			for ( var i = 0; i < this.transports.size(); i++)
				this.transports.get(i).disconnect();
		}
	}
	
};

/*
 * @param {Trap.Message} message
 * @param {Trap.Transport} transport
 */
Trap.Endpoint.prototype.onClose = function(message, transport)
{
};

/*
 * @param {Trap.Message} message
 * @param {Trap.Transport} transport
 */
Trap.Endpoint.prototype.onOpened = function(message, transport)
{
	
	if (this.getState() == Trap.Endpoint.State.CLOSED) return;
	
	if (this.getState() == Trap.Endpoint.State.CLOSING) return;
	
	if (this.getState() == Trap.Endpoint.State.ERROR) return;
	
	if (this.trapID == Trap.Constants.ENDPOINT_ID_CLIENT)
	{
		var cfg = new Trap.Configuration(message.string);
		var id = cfg.getOption(Trap.Constants.ENDPOINT_ID);
		this.setTrapID(id);
	}
	
	this.setState(Trap.Endpoint.State.OPEN);
	
};

Trap.Endpoint.prototype.setState = function(newState)
{
	if (newState == this._state) return; // Department of redundancy
	// department.
	
	var oldState = this._state;
	this._state = newState;
	
	this.logger.debug("TrapEndpoint changing state from {} to {}.", oldState, newState);
	
	this._dispatchEvent({
		type : "statechange",
		newState : newState,
		oldState : oldState
	});
	
	if (newState == Trap.Endpoint.State.OPEN) this._dispatchEvent({
		type : "open"
	});
	
	if (newState == Trap.Endpoint.State.CLOSED) this._dispatchEvent({
		type : "close"
	});
	
	if (newState == Trap.Endpoint.State.SLEEPING) this._dispatchEvent({
		type : "sleep"
	});
	
	if (newState == Trap.Endpoint.State.SLEEPING) this._dispatchEvent({
		type : "sleeping"
	});
	
	if (newState == Trap.Endpoint.State.OPENING) this._dispatchEvent({
		type : "opening"
	});
	
	if (newState == Trap.Endpoint.State.CLOSING) this._dispatchEvent({
		type : "closing"
	});
	
	if (newState == Trap.Endpoint.State.ERROR)
	{
		this._dispatchEvent({
			type : "error"
		});
		
		for ( var i = 0; i < this.transports.size(); i++)
			this.transports.get(i).disconnect();
	}
	
	if (newState == Trap.Endpoint.State.CLOSED || newState == Trap.Endpoint.State.CLOSING || newState == Trap.Endpoint.State.ERROR) if (this.keepaliveTask) clearTimeout(this.keepaliveTask);
};

/*
 * @param {Trap.Message} message
 * @param {Trap.Transport} transport
 */
Trap.Endpoint.prototype.onOpen = function(message, transport)
{
	
	if (this.getState() == Trap.Endpoint.State.CLOSED || this.getState() == Trap.Endpoint.State.CLOSING || this.getState() == Trap.Endpoint.State.ERROR)
	{
		this.logger.debug("Connection Error: Received OPEN message on {}. Returning with END", this);
		transport.sendTransportSpecific(this.createMessage().setOp(Trap.Message.Operation.END));
		var mt = this;
		
		// Ensure the transport is disconnected.
		setTimeout(function() {
			
				if (transport.getState() != TrapTransportState.DISCONNECTED && transport.getState() != TrapTransportState.ERROR)
				{
					mt.logger.debug("Disconnect Error: {} failed to disconnect, despite ending the session on {}", transport, Tmt);
					transport.forceError();
				}
		}, 5000);
		return;
	}

	try
	{
		transport.sendTransportSpecific(this.createOnOpenedMessage(message), false);
		this.setState(Trap.Endpoint.State.OPEN);
	}
	catch (e)
	{
		this.logger.warn(e);
	}
};

Trap.Endpoint.prototype.createOnOpenedMessage = function(message)
{
	// Send new OPENED message
	return this.createMessage().setOp(Trap.Message.Operation.OPENED);
};

/*
 * @param {Array} messages
 * @param {Trap.Transport} transport
 */
Trap.Endpoint.prototype.ttMessagesFailedSending = function(messages, transport)
{
	for ( var i = 0; i < messages.length; i++)
	{
		var message = messages[i];
		this.getChannel(message.getChannel).addFailedMessage();
	}
	
	for (var i=0; i<this.channels.length; i++)
		if (this.channels[i] != null)
			this.channels[i].rebuildMessageQueue();
	
	this.kickSendingThread();
};



/**
 * Fetches the last known liveness timestamp of the endpoint. this.is the last
 * time it received a message from the other end. this.includes all messages
 * (i.e. also Trap messages such as keepalives, configuration, etc) so must not
 * be confused with the last known activity of the other application. For
 * example, in the case of a JavaScript remote endpoint, this.does not guarantee
 * an evaluation error has not rendered the JSApp's main run loop as inoperable.
 * 
 * @see #isAlive(long, boolean, boolean, long) if all that is needed is an
 *      evaluation of the liveness status.
 * @return The timestamp of the last message received from the remote side.
 */
Trap.Endpoint.prototype.lastAlive = function()
{
	// Go through all transports and fetch lastAlive
	
	for ( var i = 0; i < this.transports.size(); i++)
	{
		var t = this.transports.get(i);
		var tLastAlive = t.lastAlive;
		
		if (this._lastAlive < tLastAlive) this._lastAlive = tLastAlive;
	}
	
	return this._lastAlive;
};

/**
 * Attempts to verify if the endpoint is alive, or has been alive within a
 * certain number of milliseconds. Effectively, this.can be used to trigger a
 * keepalive check of the endpoint if used with a <i>within</i> parameter of 0
 * and a <i>check</i> parameter of true.
 * <p>
 * this.function has a two-part purpose. The first is for the application to be
 * able to check the last known liveness of the endpoint, to reduce the
 * discovery time of a dead connection. The second is to trigger a check for a
 * dead endpoint, when the application needs to know that it has active
 * connectivity.
 * <p>
 * Note that in normal operation, the endpoint itself will report when it has
 * disconnected; the application does not need to concern itself with
 * this.detail unless it specifically needs to know that it has connectivity
 * right now.
 * <p>
 * <b>Warning:</b> Calling <i>isAlive</i> on a Server Trap Endpoint (i.e. when
 * none of the transports can perform the open() function) may cause a client to
 * lose its connectivity. The client may not have discovered the underlying
 * transport is dead yet, and may require more time to reconnect. A wakeup
 * mechanism can be used to establish liveness, but the server's <i>timeout</i>
 * value should be significantly more generous in order to accommodate the
 * client's reconnect and/or wakeup procedure!
 * 
 * @param {long} within
 *            Within how many milliseconds the last activity of the endpoint
 *            should have occurred before the endpoint should question whether
 *            it is alive.
 * @param {boolean} check
 *            Whether the endpoint should attempt to check for liveness, or
 *            simply return false if the last known activity of the endpoint is
 *            not later than within.
 * @param {boolean} reconnect
 *            Whether to attempt to force reconnection if the transports are not
 *            available within the given timeout. this.will ensure available
 *            liveness value reflects what is possible right now, although it
 *            may mean disconnecting transports that still may recover.
 * @param {long} timeout
 *            If check is true, how many milliseconds at most the liveness check
 *            should take before returning false anyway. The application can use
 *            this.value if it has a time constraint on it.
 * @param {Function} callback <i>true</i> if the connection is currently alive (including if
 *           this.function successfully re-established the connection), <i>false</i>
 *           otherwise.
 */
Trap.Endpoint.prototype.isAlive = function(within, check, reconnect, timeout, callback)
{
	// Ensure lastAlive is up to date.
	this.lastAlive();
	
	// Define within
	var mustBeAliveAfter = new Date().valueOf() - within;
	
	// We're within the allotted time window.
	if (this._lastAlive > mustBeAliveAfter)
	{
		callback(true);
		return;
	}
	
	// We're not allowed to perform the liveness check...
	if (!check)
	{
		callback(false);
		return;
	}
	
	// Unlike Java, we have to unroll the loop and handle it with timeouts.
	
	var i = 0;
	
	// Temporary redefinition to cure a compiler warning.
	// Compiler warnings show useful stuff (especially in JS) so I want to keep
	// them on
	var loop = function()
	{
	};
	var mt = this;
	loop = function(success)
	{
		
		if (success)
		{
			callback(true);
			return;
		}
		
		if (i < mt.availableTransports.size())
		{
			mt.availableTransports.get(i).isAlive(within, check, timeout, loop);
			i++;
		}
		else
		{
			// It appears all available transports are dead. We should reconnect
			if (!reconnect) callback(false);
			
			try
			{
				
				mt.setState(Trap.Endpoint.State.SLEEPING);
				mt.reconnect(timeout, function()
				{
					callback(mt.getState() == Trap.Endpoint.State.OPEN);
				});
				
			}
			catch (e)
			{
				mt.logger.error("Setting TrapEndpoint to state ERROR because reconnect failed. We don't know currently how to recover from this state, so the connection is dropped");
				mt.setState(Trap.Endpoint.State.ERROR);
			}
			
			callback(false);
		}
	};
	
	// Kick the callback loop into action
	loop(false);
	
};

Trap.Endpoint.prototype.getKeepaliveInterval = function()
{
	return this.keepaliveInterval;
};
    /**
	 * Sets a new keepalive interval for the trap endpoint. The keepalive
	 * interval has one of three possible meanings:
	 * <ul>
	 * <li>A value of {@link TrapKeepalivePolicy#DISABLED} will disable the
	 * keepalives.
	 * <li>A value of {@link TrapKeepalivePolicy#DEFAULT} will cause each
	 * transport to use its internal estimate of what a good keepalive is.
	 * <li>A value of 1 &lt;= n &lt;= 999999 will specify the number of seconds
	 * between keepalive messages.
	 * </ul>
	 * Any change on the TrapEndpoint level will affect all transports
	 * associated with this endpoint, overwriting any individual configuration
	 * the transports may have had. The inverse does not apply.
	 * <p>
	 * See <a href=
	 * "http://www.cf.ericsson.net/confluence/display/warp/Trap+Keepalives">the
	 * Trap Keepalive documentation</a> for details on the keepalives.
	 * 
	 * @param newInterval The new keepalive interval or policy.
	 */
Trap.Endpoint.prototype.setKeepaliveInterval = function(newInterval)
{
	this.keepaliveInterval = newInterval;
	
	// Forward apply on all transports
	for ( var i = 0; i < this.transports.size(); i++)
		this.transports.get(i).setKeepaliveInterval(newInterval);
	
	var mTimer = this.keepaliveInterval;
	
	if ((mTimer == Trap.Keepalive.Policy.DEFAULT) || (mTimer == Trap.Keepalive.Policy.DISABLED)) return;
	
	if (this.keepaliveTask != null) clearTimeout(this.keepaliveTask);
	
	var mt = this;
	this.keepaliveTask = setTimeout(function()
	{
		mt._keepaliveFun();
	}, mTimer * 1000);
};

Trap.Endpoint.prototype._keepaliveFun = function()
{
	// Conditions that should cause this task to exit.
	if ((this.getState() == Trap.Endpoint.State.CLOSING) || (this.getState() == Trap.Endpoint.State.CLOSED) || (this.getState() == Trap.Endpoint.State.ERROR)) return;
	
	if ((this.getKeepaliveInterval() == Trap.Keepalive.Policy.DISABLED) || (this.getKeepaliveInterval() == Trap.Keepalive.Policy.DEFAULT)) return;
	
	// Calculate the expected time we would need for keepalives to be working
	var expectedTime = new Date().valueOf() - (this.keepaliveInterval * 1000) - this.keepaliveExpiry;
	
	// Now verify all transports are within that time.
	for ( var i = 0; i < this.transports.size(); i++)
	{
		var t = this.transports.get(i);
		
		// Check that the transport is active
		if (!t.isConnected())
		{
			// Inactive transports are excused from keepalives
			continue;
		}
		
		if (t.lastAlive < expectedTime)
		{
			// This transport is not conforming.
			this.logger.debug("Transport {} is not compliant with the keepalive timers. Last alive reported was {}, but expected {}", t.getTransportName(), t.lastAlive, expectedTime);
			
			try
			{
				// Perform a manual check
				var mt = this;
				t.isAlive(this.keepaliveExpiry, true, this.keepaliveExpiry, function(rv)
				{
					if (!rv)
					{
						mt.logger.info("Disconnecting transport {} because it had timed out while not performing its own checks", t.getTransportName());
						t.disconnect();
					}
				});
			}
			catch (e)
			{
				this.logger.error("Exception while checking non-conforming transport", e);
			}
		}
	}
	
	// Now reschedule ourselves
	// Performing this jump will prevent a race condition from making us spiral
	// out of control
	var mTimer = this.keepaliveInterval;
	
	if ((mTimer == Trap.Keepalive.Policy.DEFAULT) || (mTimer == Trap.Keepalive.Policy.DISABLED)) return;
	
	var mt = this;
	this.keepaliveTask = setTimeout(function()
	{
		mt._keepaliveFun();
	}, mTimer * 1000);
};
    /**
	 * Sets the keepalive expiry timeout. Alias for
	 * {@link TrapKeepalivePredictor#setKeepaliveExpiry(long)} on the currently
	 * set predictor.
	 * 
	 * @param newExpiry The new keepalive expiry time.
	 * @see TrapKeepalivePredictor#setKeepaliveExpiry(long)
	 */
Trap.Endpoint.prototype.setKeepaliveExpiry = function(newExpiry)
{
	this.keepaliveExpiry = newExpiry;
	for ( var i = 0; i < this.transports.size(); i++)
		this.transports.get(i).setKeepaliveExpiry(newExpiry);
};

/**
 * Fetches the channel object associated with the given channel ID. If the
 * channel was not created, creates it, allocating all required buffers.
 * 
 * @param {Number} channelID The channel object to retrieve
 * @returns {Trap.Channel} A new or existing channel object
 */
Trap.Endpoint.prototype.getChannel = function(channelID)
{
	var c = this.channels[channelID];
	
	if (c == null)
	{
		c = new Trap.Channel(this, channelID);
		var chunkSize = this.getMaxChunkSize();
		chunkSize = Math.min(chunkSize, c.getChunkSize());
		if (chunkSize <= 0)
			chunkSize = Number.MAX_VALUE;
		c.setChunkSize(chunkSize);
		
		this.channels[channelID] = c;
		this.messageQueue.rebuild(this.channels);
	}
	
	return c;
};

/**
 * @returns {Number}
 */
Trap.Endpoint.prototype.getMaxChunkSize = function()
{
	return this.config.getIntOption("trap." + Trap.Constants.OPTION_MAX_CHUNK_SIZE, 64*1024);
};

Trap.Endpoint.prototype.ttMessageSent = function(message, transport, context)
{
	this.getChannel(message.getChannel()).messageSent(message);
};
	/*
	 * 
	 * 
	 * @author Vladimir Katardjiev
	 * @param {String|Uint8Array} inData A string or bytearray containing a full message.
	 * 
	 * @property {any[]} data
	 * @property 
	 * @constructor
	 */

Trap.Message = function(inData)
{
	
	this._data = [];
	this._authData = null;
	this._format = Trap.Message.Format.SEVEN_BIT_SAFE;
	this._op = Trap.Message.Operation.OK;
	this._compressed = false;
	this._channel = 0;
	
	this._messageId = 0;
	
	if (typeof(inData) != "undefined")
		this.deserialize(inData, 0, inData.length);
};


// Getters/setters
Trap._compat.__defineGetterSetter(Trap.Message.prototype, "messageId");
Trap._compat.__defineGetter(Trap.Message.prototype, "channel", function() {
	return this.format == Trap.Message.Format.REGULAR ? this._channel : 0;
});
Trap._compat.__defineSetter(Trap.Message.prototype, "channel");
Trap._compat.__defineGetterSetter(Trap.Message.prototype, "compressed");

Trap._compat.__defineGetter(Trap.Message.prototype, "data", function() {
	return this._data;
});

Trap._compat.__defineGetter(Trap.Message.prototype, "dataAsString", function() {
	return String.fromUTF8ByteArray(this._data);
});

Trap._compat.__defineGetter(Trap.Message.prototype, "string", function() {
	return String.fromUTF8ByteArray(this._data);
});
Trap._compat.__defineGetter(Trap.Message.prototype, "authData", function() {
	return this._authData;
});
Trap._compat.__defineGetter(Trap.Message.prototype, "format", function() {
	return this._format;
});
Trap._compat.__defineGetter(Trap.Message.prototype, "op", function() {
	return this._op;
});
Trap._compat.__defineGetter(Trap.Message.prototype, "channelID", function() {
	return this._channel;
});

Trap._compat.__defineSetter(Trap.Message.prototype, "data", function(newData) {
	
	if (typeof(newData) == "string")
		this._data = newData.toUTF8ByteArray();
	else if (typeof(newData.length) == "number" || typeof(newData.byteLength) == "number")
		this._data = newData;
	else if (typeof(newData) == "number")
		this._data = [newData];
	else if (typeof(newData) == "object")
		this._data = JSON.stringify(newData).toUTF8ByteArray();
	else
		throw "Invalid data supplied; not an array, not a string, not a number";
	
	return this;
});

Trap._compat.__defineSetter(Trap.Message.prototype, "authData", function(newAuthData){
	
	if (!!newAuthData && newAuthData.length > 65535) 
		throw "authData cannot be more than 65535 bytes";
	
	if (!!newAuthData && newAuthData.length != newAuthData.toUTF8ByteArray().length)
		throw "authData was not a US-ASCII string";
	
	this._authData = newAuthData;
	return this; 
});

Trap._compat.__defineSetter(Trap.Message.prototype, "format", function(newFormat){
	this._format = newFormat;
	return this;
});

Trap._compat.__defineSetter(Trap.Message.prototype, "op", function(newOp){
	this._op = newOp;
	return this;
});

Trap._compat.__defineSetter(Trap.Message.prototype, "channelID", function(newID){
	this._channel= newID;
	return this;
});

Trap.Message.Operation =
{
		
		OPEN: 1,
		OPENED: 2,
		CLOSE: 3,
		END: 4,
		CHALLENGE: 5,
		ERROR: 6,
		MESSAGE: 8,
		ACK: 9,
		FRAGMENT_START: 10,
		FRAGMENT_END: 11,
		OK: 16,
		PING: 17,
		PONG: 18,
		TRANSPORT: 19,
		name: function(op)
		{
			switch (op)
			{
				case 1:
					return "OPEN";
					
				case 2:
					return "OPENED";
					
				case 3:
					return "CLOSE";
					
				case 4:
					return "END";
					
				case 5:
					return "CHALLENGE";
					
				case 6:
					return "ERROR";
					
				case 8:
					return "MESSAGE";
					
				case 9:
					return "ACK";
					
				case 10:
					return "FRAGMENT_START";
					
				case 11:
					return "FRAGMENT_END";
					
				case 16:
					return "OK";
					
				case 17:
					return "PING";
					
				case 18:
					return "PONG";
					
				case 19:
					return "TRANSPORT";
					
				default:
					return "Unknown op type: " + op;
			}
		},
		
		getType: function(t)
		{
			return t;
		}
};

Trap.Message.Format = 
{
	REGULAR: "Trap.Message.Format.Regular",
	SEVEN_BIT_SAFE: "Trap.Message.Format.7bit",
	DEFAULT: "Trap.Message.Format.Regular"
};

Trap.Constants.MESSAGE_FORMAT_DEFAULT = Trap.Message.Format.DEFAULT;

Trap.Message.prototype.getBits = function(src, startBit, endBit)
{
	var mask = (Math.pow(2, endBit - startBit + 1) - 1);
	mask = mask << (32 - endBit);
	var rv = (src & mask) >> (32 - endBit);
	return rv;
};

Trap.Message.prototype.writeInt7 = function(src, bos)
{
	bos.write(this.getBits(src, 5, 11));
	bos.write(this.getBits(src, 12, 18));
	bos.write(this.getBits(src, 19, 25));
	bos.write(this.getBits(src, 26, 32));
};

Trap.Message.prototype.writeInt8 = function(src, bos)
{
	bos.write(this.getBits(src, 1, 8));
	bos.write(this.getBits(src, 9, 16));
	bos.write(this.getBits(src, 17, 24));
	bos.write(this.getBits(src, 25, 32));
};

Trap.Message.prototype.serialize = function(useBinary)
{
	var bos = (useBinary ? new Trap.ByteArrayOutputStream() : new Trap.ByteStringOutputStream());
	
	if (this.format == Trap.Message.Format.SEVEN_BIT_SAFE)
		this.serialize7bit(bos, useBinary);
	else
		this.serialize8bit(bos, useBinary);
	
	if (useBinary)
		return bos.toArray();
	else
		return bos.toString();
};

Trap.Message.prototype.getCompressedData = function()
{
	if (!this.compressed)
		return this.data;
	
	if (!this._compressedData)
		this._compressedData = new Zlib.Deflate(this.data).compress();
	
	return this._compressedData;
};


Trap.Message.prototype.serialize8bit = function(bos, useBinary)
{
	// Make 8-bit assertions
	if (this.data.length >= Math.pow(2, 32))
		throw "Asked to serialize more than 2^32 bytes data into a 8-bit Trap message";
	
	var b = 0;
	
	// First byte: |1|0| MESSAGEOP |
	b |= this.op | 0x80;
	bos.write(b);
	
	var authLen = (this.authData != null ? this.authData.length : 0);
	var mData = this.getCompressedData();
	
	// Second byte: |C|RSV1
	b = 0;
	
	if (this.compressed && useBinary)
		b |= 0x80;
	
	bos.write(b);
	
	// Third byte: Bits 3 - 9 of authLen
	bos.write(this.getBits(authLen, 17, 24));
	
	// Fourth byte: Bits 10 - 16 of authLen
	bos.write(this.getBits(authLen, 25, 32));
	
	// Bytes 5-8: MessageID
	this.writeInt8(this.getMessageId(), bos, true);
		
	// Byte 9: RSV2
	bos.write(0);
		
	// Byte 10: ChannelID
	bos.write(this.channelID);
		
	// Byte 11-12: RSV3
	bos.write(0);
	bos.write(0);
	
	// Byte 13-16: Data length
	this.writeInt8((mData.byteLength ? mData.byteLength : mData.length), bos, false);
	
	if (authLen > 0)
		bos.write(this.authData);
	
	bos.write(useBinary ? mData : String.fromUTF8ByteArray(mData));
};

Trap.Message.prototype.serialize7bit = function(bos, useBinary)
{
	if (this.data.length >= Math.pow(2, 28))
		throw "Asked to serialize more than 2^28 bytes data into a 7-bit Trap message";
	
	var b = 0;
	
	// First byte: |0|0| MESSAGEOP |
	b |= this.op;
	bos.write(b);
	
	var authLen = (this.authData != null ? this.authData.length : 0);
	
	// Second byte: First two bits of authLen
	bos.write(this.getBits(authLen, 17, 18));
	
	// Third byte: Bits 3 - 9 of authLen
	bos.write(this.getBits(authLen, 19, 25));
	
	// Fourth byte: Bits 10 - 16 of authLen
	bos.write(this.getBits(authLen, 26, 32));
	
	// Skip four bytes (RSV2, CHANID, RSV3)
	this.writeInt7(this.getMessageId(), bos, true);
	this.writeInt7(0, bos, true);
	this.writeInt7((this.data.byteLength ? this.data.byteLength : this.data.length), bos, false);
	
	// This will corrupt non-US-ASCII authData. Trap spec forbids it, so we're correct in doing so. 
	if (authLen > 0)
		bos.write(this.authData);

	bos.write(useBinary ? this.data : String.fromUTF8ByteArray(this.data));
	
};
	
	/**
	 * Attempts to deserialize a TrapMessage.
	 * 
	 * @param rawData
	 * @param length
	 * @param offset
	 * @return -1 if it could not parse a message from the data, the number of
	 *         bytes consumed otherwise.
	 * @throws UnsupportedEncodingException
	 *             if the message encoding is not supported
	 */
Trap.Message.prototype.deserialize = function(rawData, offset, length)
{

	
	if ((offset + length) > rawData.length)
		throw "Offset and length specified exceed the buffer";
	
	if (length < 16)
		return -1;
	
	var authLen;
	var contentLen;
	
	if ((rawData[offset + 0] & 0x80) != 0)
	{
		// 8-bit
		this.format = Trap.Message.Format.REGULAR;
		this.op = Trap.Message.Operation.getType(rawData[offset + 0] & 0x3F);
		this.compressed = (rawData[offset+1] & 0x80) != 0;
		this.channel = rawData[offset+9] & 0xFF;
		
		authLen = rawData[offset + 2] << 8 | rawData[offset + 3];
		this.messageId = rawData[offset + 4] << 24 | rawData[offset + 5] << 16 | rawData[offset + 6] << 8 | rawData[offset + 7];
		
		contentLen = rawData[offset + 12] << 24 | rawData[offset + 13] << 16 | rawData[offset + 14] << 8 | rawData[offset + 15];
	}
	else
	{
		// 7-bit
		this.format = Trap.Message.Format.SEVEN_BIT_SAFE;
		this.op = Trap.Message.Operation.getType(rawData[offset + 0] & 0x3F);
		
		authLen = ((rawData[offset + 1] & 0x03) << 14) | ((rawData[offset + 2] & 0x7F) << 7) | ((rawData[offset + 3] & 0x7F) << 0);
		this.messageId = ((rawData[offset + 4] & 0x7F) << 21) | ((rawData[offset + 5] & 0x7F) << 14) | ((rawData[offset + 6] & 0x7F) << 7) | ((rawData[offset + 7] & 0x7F) << 0);
		contentLen = ((rawData[offset + 12] & 0x7F) << 21) | ((rawData[offset + 13] & 0x7F) << 14) | ((rawData[offset + 14] & 0x7F) << 7) | ((rawData[offset + 15] & 0x7F) << 0);
		
		this.compressed = false;
		this.channel = 0;
	}
	
	// Verify that there's enough remaining content to read the message.
	var messageSize = 16 + authLen + contentLen;
	
	if (length < messageSize)
		return -1; // Cannot successfully read the remaining values.
		
	// Range of authHeader = (12, authLen)
	var startByte = offset + 16;
	
	// We have an authentication header!
	if (authLen > 0)
	{
		this.authData = Trap.subarray(rawData, startByte, startByte + authLen);
		
		// AuthData is a string, we should decode it...
		this.authData = String.utf8Decode(this.authData);
		
		startByte += authLen;
	}
	else
	{
		this.authData = null;
	}
	
	// Copy the data
	// We won't UTF-8 decode at this stage. If we do, it'll be harder to construct the .data and .string
	// properties when we dispatch the event. Instead, store data as an array and leave it to higher ups
	// to decide the representation
	this.data = Trap.subarray(rawData, startByte, startByte + contentLen);
	
	if (this.compressed)
		this.data = new Zlib.Inflate(this.data).decompress();
	
	// The number of bytes consumed. This allows multiple messages to be parsed from the same data block.
	return messageSize;
};

Trap.Message.prototype.length = function() {
	var l = 16;
	
	if (this.authData != null) l += this.authData.toUTF8ByteArray().length;
	if (this.getCompressedData() != null) l += this.getCompressedData().byteLength || this.getCompressedData().length;
	
	return l;
};
//< needs(endpoint)

/**
 * Instantiates a new Trap Client.
 * 
 * @classdesc A Trap.ClientEndpoint is a Trap.Endpoint capable of opening an
 *            outgoing connection, commonly to a TrapListener. It is able to
 *            reconnect transports at will, unlike a ServerTrapEndpoint. This is
 *            the main entry point for JavaScript. Create a Trap.ClientEndpoint
 *            like so
 *            <p>
 * 
 * <pre>
 * client = new Trap.ClientEndpoint(&quot;http://trapserver.com:8888&quot;);
 * client.onopen = function() {
 * };
 * client.onmessage = function(evt) {
 * };
 * </pre>
 * 
 * <p>
 * The client will open automatically once instantiated. Assign at least the
 * <b>onopen</b> and <b>onmessage</b> callbacks (and preferably <b>onerror</b>)
 * in order to get a working client.
 * @constructor
 * @param {Boolean} configuration.useBinary Enables (default) or disables binary
 *            support.
 * @param {Boolean} configuration.autoConfigure Enables (default) or disables
 *            automatic configuration
 * @param {String} configuration.remote A URI or TrapConfiguration to the remote
 *            host.
 * 
 * @param {String} configuration The alternate means to instantiate a
 *            ClientEndpoint is to supply it with a single string constituting a
 *            complete configuration.
 * @extends Trap.Endpoint
 */
Trap.ClientEndpoint = function(configuration, autoConfigure) {

	if (typeof (configuration) == "object") {
		this.useBinary = typeof (configuration.useBinary) == "boolean" ? configuration.useBinary
				: true;

		this.autoConfigure = (typeof (configuration.autoConfigure) == "boolean" ? configuration.autoConfigure
				: true);

		configuration = configuration.remote;
	} else {
		this.autoConfigure = (typeof (autoConfigure) == "boolean" ? autoConfigure
				: true);
		this.useBinary = typeof (Trap.useBinary) != "undefined" ? Trap.useBinary
				: Trap.supportsBinary;
	}

	if (this.useBinary && !Trap.supportsBinary)
		throw "Cannot enable binary mode; it is unsupported on this client. Either no transport supported it, or Uint8Array is not present.";

	this.cTransport;

	/**
	 * The list of transports that have not been tried before or after all
	 * transports failed and being tried again.
	 */
	this.transportsToConnect = new Trap.Set();

	/**
	 * The transports that have failed in some non-fatal way. There will be an
	 * attempt taken in the future to recover them.
	 */
	this.failedTransports = new Trap.List();

	this.activeTransports = new Trap.List();

	this.recovering = false;

	Trap.Endpoint.prototype.constructor.call(this);
	this._maxActiveTransports = 1;

	this.trapID = Trap.Constants.ENDPOINT_ID_CLIENT; // Allow the server to
														// override our trap ID.

	// Load the appropriate transports
	for ( var tName in Trap.Transports) {
		var t = Trap.Transports[tName];

		if (t.prototype
				&& typeof (t.prototype.setTransportPriority) == "function") // is a
																			// transport
		{
			var transport = new t();
			this.logger.trace("Initialising new Transport for client: {}",
					transport.getTransportName());

			if (!transport.canConnect()) {
				this.logger.trace("Skipping it; it cannot connect");
				continue;
			}

			if (this.useBinary && !transport.supportsBinary) {
				this.logger
						.info("Skipping it; Trap Binary Mode requested, but transport only supports text");
				continue;
			}

			transport.useBinary = this.useBinary;

			// Unlike Java, TrapEndpoint only defines one addTransport, and that
			// one sets
			// this object as delegate. Thus, we're done.
			this.addTransport(transport);
		}
	}

	if (this.transports.size() == 0)
		throw "No transports could be initialised; either no transports could connect, or transports did not support binary mode (if requested)";

	if ((configuration != null) && (configuration.trim().length > 0)) {

		// Check if we need to redo the configuration.
		if (configuration.startsWith("ws://")
				|| configuration.startsWith("wss://")) {
			// WebSocket Transport
			configuration = "trap.transport.websocket.wsuri = " + configuration;
		} else if (configuration.startsWith("socket://")) {
			// TODO: Socket URI
		} else if (configuration.startsWith("http://")
				|| configuration.startsWith("https://")) {
			configuration = "trap.transport.http.url = " + configuration;
		} else if (!configuration.startsWith("trap"))
			throw "Unknown configuration; invalid format or garbage characters entered";

	}

	this.configure(configuration);

	this.transportRecoveryTimeout = 15 * 60 * 1000;

	var mt = this;
	setTimeout(function() {

		mt.logger.trace("##### CLIENT OPEN ####");
		mt.logger.trace("Config is: {}", mt.config.toString());
		for ( var i = 0; i < mt.transports.size(); i++) {
			var t = mt.transports.get(i);
			mt.logger.trace("Transport [{}] is enabled: {}", t
					.getTransportName(), t.isEnabled());
		}

		mt.setState(Trap.Endpoint.State.OPENING);
		mt.doOpen();

		// Also start recovery
		var recoveryFun = function() {

			mt.failedTransports.clear();

			for ( var i = 0; i < mt.transports.size(); i++) {
				if (mt.getState() == Trap.Endpoint.State.CLOSING
						|| mt.getState() == Trap.Endpoint.State.CLOSED
						|| mt.getState() == Trap.Endpoint.State.CLOSED)
					return;

				var t = mt.transports.get(i);

				// Check if t is active
				var active = false;
				for ( var j = 0; j < mt.activeTransports.size(); j++) {
					if (mt.activeTransports.get(j) == t) {
						active = true;
						break;
					}
				}

				if (!active)
					mt.transportsToConnect.add(t);
			}

			if (!mt.recovering)
				mt.kickRecoveryThread();

			setTimeout(recoveryFun, mt.transportRecoveryTimeout);
		}

		setTimeout(recoveryFun, mt.transportRecoveryTimeout);

	}, 0);
};

Trap.ClientEndpoint.prototype = new Trap.Endpoint;
Trap.ClientEndpoint.prototype.constructor = Trap.ClientEndpoint;

Trap.ClientEndpoint.prototype.parseConfiguration = function(configuration) {
	return new Trap.CustomConfiguration(configuration);
};

Trap.ClientEndpoint.prototype.open = function() {

};

//> (void)fn()
Trap.ClientEndpoint.prototype.doOpen = function() {
	// If the list of transports that still can be used and is empty -> die!
	if (this.transports.size() == 0) {
		this.setState(Trap.Endpoint.State.ERROR);
		throw "No transports available";
	}
	// Clean all the failed transports so far, we'll retry all of them anyway.
	this.failedTransports.clear();
	this.activeTransports.clear();
	this.availableTransports.clear();
	this.transportsToConnect.clear();

	// Let transportsToConnect be the list of transports that we haven't tried.
	this.transportsToConnect.addAll(this.transports);

	// Pick the first untested transport (the one with the highest priority)
	this.kickRecoveryThread();
};

// One of our transports has changed the state, let's see what happened...
//> (void) fn(Trap.Endpoint.State, Trap.Endpoint.State, Trap.Transport)
Trap.ClientEndpoint.prototype.ttStateChanged = function(newState, oldState,
		transport) {

	this.logger.debug("Transport {} changed state to {}", transport
			.getTransportName(), newState);
	// Super will manage available transports. All we need to consider is what
	// action to take.
	Trap.Endpoint.prototype.ttStateChanged.call(this, newState, oldState,
			transport);

	if (this.getState() == Trap.Endpoint.State.CLOSED
			|| this.getState() == Trap.Endpoint.State.CLOSING
			|| this.getState() == Trap.Endpoint.State.ERROR)
		return;

	// This is fine. We're not interested in disconnecting transports; super has
	// already managed this for us.
	if (oldState == Trap.Transport.State.DISCONNECTING) {
		this.activeTransports.remove(transport);
		this.availableTransports.remove(transport);
		return;
	}

	// What to do if we lose a transport
	if (newState == Trap.Transport.State.DISCONNECTED
			|| newState == Trap.Transport.State.ERROR) {

		this.activeTransports.remove(transport);

		// This was an already connected transport. If we have other transports
		// available, we should silently try and reconnect it in the background
		if (oldState == Trap.Transport.State.AVAILABLE
				|| oldState == Trap.Transport.State.UNAVAILABLE
				|| oldState == Trap.Transport.State.CONNECTED) {

			if (this.activeTransports.size() != 0) {
				this.transportsToConnect.add(transport);
				this.kickRecoveryThread();
				return;
			}

			if (this.getState() == Trap.Endpoint.State.OPENING) {
				// The current transport failed. Just drop it in the failed
				// transports pile.
				// (Failed transports are cycled in at regular intervals)
				this.failedTransports.add(transport);

				// Also notify recovery that we have lost a transport. This may
				// schedule another to be reconnected.
				this.kickRecoveryThread();
				return;
			} else {

				var openTimeout = 1000;

				if (this.getState() == Trap.Endpoint.State.OPEN) {
					// We have to report that we've lost all our transports.
					this.setState(Trap.Endpoint.State.SLEEPING);

					// Adjust reconnect timeout
					this.canReconnectUntil = new Date().valueOf()
							+ this.reconnectTimeout;

					// This is the first time, just reconnect immediately
					openTimeout = 0;
				}

				if (this.getState() != Trap.Endpoint.State.SLEEPING) {
					// We have nothing to do here
					return;
				}

				var mt = this;

				if (new Date().valueOf() < this.canReconnectUntil) {
					setTimeout(
							function() {

								try {
									mt.doOpen();
								} catch (e) {
									mt.logger
											.error(
													"Error while reconnecting after all transports failed",
													e);
									return;
								}

							}, openTimeout);

				}

			}
		} else if (oldState == Trap.Transport.State.CONNECTING) {
			this.cycleTransport(transport, "connectivity failure");
		} else {
			// disconnecting, so do nothing
		}
	}

	if (newState == Trap.Transport.State.CONNECTED) {
		if (oldState == Trap.Transport.State.CONNECTING) {
			this.sendOpen(transport);
		} else {
			this.logger
					.error("Reached Trap.Transport.State.CONNECTED from a non-CONNECTING state. We don't believe in this.");
		}
	}
};

Trap.ClientEndpoint.prototype.ttMessageReceived = function(message, transport) {
	if (transport == this.cTransport) {
		if (message.getOp() == Trap.Message.Operation.OPENED) {
			this.cTransport = null;
			// received configuration from the server
			if (this.autoConfigure && message.getData().length > 0)
				this.config.setStaticConfiguration(message.getData());
		} else {
			this.cycleTransport(transport, "illegal open reply message op");
			return;
		}
	}
	Trap.Endpoint.prototype.ttMessageReceived.call(this, message, transport);
};

Trap.ClientEndpoint.prototype.sendOpen = function(transport) {
	var m = this.createMessage().setOp(Trap.Message.Operation.OPEN);
	var body = new Trap.Configuration();
	if (this.autoConfigure) {
		try {
			var str = this.getConfiguration().toString();
			var hashed = Trap.MD5(str); // Represented as hex string
			body.setOption(Trap.Configuration.CONFIG_HASH_PROPERTY, hashed);
		} catch (e) {
			this.logger.warn("Could not compute client configuration hash", e);
		}
		;
	}

	if (this.connectionToken == null)
		this.connectionToken = Trap._uuid();

	body.setOption("trap.connection-token", this.connectionToken);
	body.setOption(Trap.Constants.OPTION_MAX_CHUNK_SIZE, "" + (16 * 1024));
	body.setOption(Trap.Constants.OPTION_ENABLE_COMPRESSION, "true"); // Forcibly
																		// enable
																		// compression
																		// for
																		// the
																		// time
																		// being

	if (!!this.config.getOption(Trap.Constants.OPTION_AUTO_HOSTNAME))
		body.setOption(Trap.Constants.OPTION_AUTO_HOSTNAME, this.config
				.getOption(Trap.Constants.OPTION_AUTO_HOSTNAME));

	m.setData(body.toString());

	try {
		transport.send(m, false);
	} catch (e) {
		this.cycleTransport(transport, "open message send failure");
	}
};

Trap.ClientEndpoint.prototype.cycleTransport = function(transport, reason) {
	this.logger.debug("Cycling transports due to {} {}...", transport
			.getTransportName(), reason);
	transport.onmessage = function() {
	};
	transport.onstatechange = function() {
	};
	transport.onfailedsending = function() {
	};
	transport.onmessagesent = function() {
	};
	transport.disconnect();

	this.activeTransports.remove(transport);
	this.failedTransports.add(transport);

	// Recover only if we have active transports. Otherwise do open...
	if (this.transportsToConnect.size() == 0) {

		if (this.activeTransports.size() > 0) {
			// Let recovery take care of reopening.
			return;
		}

		if (this.getState() == Trap.Endpoint.State.OPENING) {
			this.logger
					.error("Could not open a connection on any transport...");
			this.setState(Trap.Endpoint.State.ERROR);
			return;
		}

		var mt = this;

		// Don't recycle!
		if (this._cycling)
			return;

		this._cycling = setTimeout(function() {
			try {
				this._cycling = null;
				mt.doOpen();
			} catch (e) {
				mt.logger.warn(e);
			}
		}, 1000);
	} else
		this.kickRecoveryThread();
};

Trap.ClientEndpoint.prototype.kickRecoveryThread = function() {
	if (this.recovering)
		return;

	var mt = this;

	this.recovering = setTimeout(
			function() {

				// Don't reconnect transports if the endpoint doesn't want them
				// to be.
				if (mt.state == Trap.Endpoint.State.CLOSED
						|| mt.state == Trap.Endpoint.State.CLOSING
						|| mt.state == Trap.Endpoint.State.ERROR)
					return;

				try {
					for (;;) {

						// Sort the connecting transports
						// This ensures we always get the first transport

						mt.transportsToConnect.sort(function(o1, o2) {
							return o1.getTransportPriority()
									- o2.getTransportPriority();
						});

						var first = null;
						if (mt.transportsToConnect.size() > 0) {
							try {

								first = mt.transportsToConnect.remove(0);

								if (first != null) {

									var t = first;

									mt.logger
											.trace(
													"Now trying to connect transport {}",
													t.getTransportName());

									if (!t.canConnect()) {
										mt.logger
												.trace("Skipping: Transport cannot connect");
										continue;
									}

									if (!t.isEnabled()) {
										mt.logger
												.trace("Skipping: Transport is disabled");
										continue;
									}

									// Abort connection attempts if head
									// transport is downprioritised.
									var downPrioritised = false;
									for ( var i = 0; i < mt.availableTransports
											.size(); i++)
										if (mt.availableTransports.get(i)
												.getTransportPriority() < t
												.getTransportPriority())
											downPrioritised = true;

									if (downPrioritised) {
										mt.transportsToConnect.add(0, t);
										mt.logger
												.trace("Skipping: Transport is downprioritised (we'll try a higher prio transport first)");
										break;
									} // */

									t.init(); // Hook the delegate methods

									t.onmessage = function(e) {
										mt.ttMessageReceived(e.message,
												e.target, null);
									};
									t.onstatechange = function(e) {
										mt.ttStateChanged(e.newState,
												e.oldState, e.target, null);
									};
									t.onfailedsending = function(e) {
										mt.ttMessagesFailedSending(e.messages,
												e.target, null);
									};
									t.onmessagesent = function(e) {
										mt.ttMessageSent(e.message, e.target,
												null);
									};

									t.setAuthentication(mt.authentication);
									t.setConfiguration(mt.config);
									t.setFormat(mt.getFormat());

									mt.activeTransports.add(t);
									t.connect();

								}
							} catch (e) {
								if (!!first) {
									mt.failedTransports.add(first);
									mt.activeTransports.remove(first);
								}
							}

						}

						if (mt.transportsToConnect.size() == 0 || first == null) {
							mt.recovering = null;
							return;
						}
					}
				} catch (t) {
					mt.logger.warn(t);
					mt.recovering = null;
				}
				mt.recovering = null;
			}, 0);
};

Trap.ClientEndpoint.prototype.reconnect = function(timeout) {
	// On the client, we'll use the transports list in order to reconnect, so we
	// have to just d/c and clear available transports.
	for ( var i = 0; i < this.transports.size(); i++)
		this.transports.get(i).disconnect();

	// After having jettisonned all transports, create new data structs for them
	this.availableTransports = new Trap.List();

	// Restart connection attempts
	this.doOpen();

	// Set a timeout reconnect
	var mt = this;
	if (timeout > 0)
		this.reconnectFunTimer = setTimeout(function() {

			if (mt.getState() != Trap.Endpoint.State.OPEN)
				mt.setState(Trap.Endpoint.State.CLOSED);

			mt.reconnectFunTimer = null;

		}, timeout);
};

Trap.ClientEndpoint.prototype.onOpened = function(message, transport) {

	var rv = Trap.Endpoint.prototype.onOpened.call(this, message, transport);

	if (this.trapID != Trap.Constants.ENDPOINT_ID_CLIENT && this.autoConfigure) {
		if (!!message.string && message.string.length > 0) {
			// Message data should contain new configuration
			this.logger.debug("Received new configuration from server...");
			this.logger.debug("Configuration was [{}]", message.string);
			this.configure(message.string);

			// Any transport that is currently non-active should be scheduled to
			// connect
			// This includes transports that weren't connected in the first
			// place (transport priorities may have changed)
			this.failedTransports.clear();

			for ( var i = 0; i < this.transports.size(); i++) {
				var t = this.transports.get(i);

				// Check if t is active
				var active = false;
				for ( var j = 0; j < this.activeTransports.size(); j++) {
					if (this.activeTransports.get(j) == t) {
						active = true;
						break;
					}
				}

				if (!active)
					this.transportsToConnect.add(t);
			}

			// Now make them connect
			this.kickRecoveryThread();

		}
	}

	return rv;

};


/**
 * Defines the methods available for interacting with the various Trap
 * transports.
 * <p>
 * Applications should not need to spend too much effort in configuring the
 * specifics of the Trap transports. At most, applications should suffice with
 * using the enable/disable functionality, and leave Trap to manage the rest.
 * 
 * @author Vladimir Katardjiev
 */
Trap.Transport = function(){};

Trap.Transport.Options = 
{
		Enabled: "enabled",
		ENABLED: "enabled",
		Priority: "priority",
};

Trap.Transport.State =
{
		DISCONNECTED: "trap.transport.state.DISCONNECTED",
		CONNECTING: "trap.transport.state.CONNECTING",
		CONNECTED: "trap.transport.state.CONNECTED",
		AVAILABLE: "trap.transport.state.AVAILABLE",
		UNAVAILABLE: "trap.transport.state.UNAVAILABLE",
		DISCONNECTING: "trap.transport.state.DISCONNECTING",
		ERROR: "trap.transport.state.ERROR",
};

/**
 * Checks whether the given Trap transport is enabled (i.e. it will react to
 * any calls other than configuration).
 * 
 * @return <i>true</i> if the transport is enabled, <i>false</i> otherwise.
 */
Trap.Transport.prototype.isEnabled = function(){};

/**
 * Enables this transport. Does not imply that this transport should
 * connect; {@link #connect()} must be called separately for that to happen.
 */
Trap.Transport.prototype.enable = function(){};

/**
 * Disables this transport, preventing it from participating in the
 * transport abstraction. Unlike {@link #enable()}, disable <b>does imply
 * the transport must close</b> and refuse to carry other messages. Disable
 * may not fail.
 */
Trap.Transport.prototype.disable = function(){};

/**
 * Checks if this transport is currently connected to the other end. Does
 * not imply whether or not it is possible for this transport to connect.
 * 
 * @return <i>true</i> if this transport object represents an active
 *         connection to the other end, <i>false</i> otherwise.
 */
Trap.Transport.prototype.isConnected = function(){};

/**
 * Signals to this transport that it should attempt to connect to the remote
 * endpoint. The transport may attempt to connect synchronously,
 * asynchronously or not at all according to its own configuration.
 * <p>
 * Not all transport instances are able to open an outgoing connection (e.g.
 * server instances) and, as such, some instances may throw an exception
 * when calling this method. If the transport does not support outgoing
 * connections, it must throw an exception immediately.
 * 
 * @throws TrapException
 *             If this transport does not support outgoing connections.
 */
Trap.Transport.prototype.connect = function(){};

/**
 * Signals to this transport that it must disconnect. The transport must
 * immediately take all measures to close the connection, must clean up as
 * much as it can, and may not throw any exceptions while doing so.
 * <p>
 * May NOT block.
 */
Trap.Transport.prototype.disconnect = function(){};

/**
 * Fetches this transport's priority, which is used in the comparable
 * implementation to sort transports, if needed. Currently unused.
 * 
 * @return The transport's priority
 */
Trap.Transport.prototype.getTransportPriority = function(){};

/**
 * Sets this transport's priority.
 * 
 * @param priority
 */
Trap.Transport.prototype.setTransportPriority = function(priority){};

/**
 * Gets this transport's name. The transport name is used for, among other
 * things, log outputs and configuration settings, must be alphanumeric and
 * contain no spaces.
 * 
 * @return The transport's name.
 */
Trap.Transport.prototype.getTransportName = function(){};

/**
 * Configures a specific transport setting (key/value)
 * 
 * @param configurationKey
 * @param configurationValue
 * @throws TrapException
 */
Trap.Transport.prototype.configure = function(configurationKey, configurationValue){};

/**
 * Configures a specific transport setting (key/value)
 * 
 * @param configurationKey
 * @param configurationValue
 * @throws TrapException
 */
Trap.Transport.prototype.configure = function(configurationKey, configurationValue){};

/**
 * Sets the Transport's configuration object. This configuration object is
 * shared with the parent.
 * 
 * @param configuration
 */
Trap.Transport.prototype.setConfiguration = function(configuration){};

/**
 * Returns a configuration string representing this transport's
 * configuration.
 * 
 * @return A String representation of the configuration of this
 *         TrapTransport.
 */
Trap.Transport.prototype.getConfiguration = function(){};

/**
 * Set an authentication instance for this transport, to be used for
 * authenticating any messages that need authentication.
 * 
 * @param authentication
 * @throws TrapException
 *             If the transport and Authentication instances are not
 *             mutually compatible. This transport should then be discarded.
 */
Trap.Transport.prototype.setAuthentication = function(authentication){};

/**
 * Queries if this transport can perform a connection, i.e. if it can act as
 * a client transport.
 * 
 * @return <i>true</i> if this transport can perform an outgoing connection,
 *         <i>false</i> otherwise.
 */
Trap.Transport.prototype.canConnect = function(){};

/**
 * Queries if this transport can accept incoming connections, i.e. if it can
 * act as a server.
 * 
 * @return <i>true</i> if this transport can receive incoming connections,
 *         <i>false</i> otherwise.
 */
Trap.Transport.prototype.canListen = function(){};

/**
 * Attempts to send a message with this transport. If the transport cannot
 * send this message, in full, right now, it MUST throw an exception. The
 * transport MUST NOT buffer messages if the <i>expectMore</i> flag is
 * false. The transport MAY buffer messages if <i>expectMore</i> is
 * <i>true</i> but this is not required.
 * 
 * @param message
 *            The message to send.
 * @param expectMore
 *            A flag signifying to the transport that more messages will be
 *            sent in a short timespan (less than 1ms). Some transports may
 *            wish to buffer these messages before sending to optimise
 *            performance.
 * @throws TrapException
 *             If an error occurred during the sending of the message(s).
 *             The exception MUST contain the message(s) that failed
 *             sending. If it contains more than one message, the order must
 *             be in the same order that send() was called.
 */
Trap.Transport.prototype.send = function(message, expectMore){};

/**
 * Asks if the transport is available for sending. Effectively checks the
 * transport's state for available, but this way is faster.
 * 
 * @return <i>true</i> if the transport can be used to send a message,
 *         <i>false</i> otherwise.
 */
Trap.Transport.prototype.isAvailable = function(){};

/**
 * Sets the Trap ID of this transport.
 * 
 * @param id
 */
Trap.Transport.prototype.setTrapID = function(id){};

/**
 * Returns the Trap ID of this transport.
 * 
 * @return The Trap ID.
 */
Trap.Transport.prototype.getTrapID = function(){};

/**
 * Fetches the transport's current state
 * 
 * @return The transport's current state.
 */
Trap.Transport.prototype.getState = function(){};

/**
 * Fetches the last known liveness timestamp of the transport. This is the
 * last time it received a message from the other end.
 * 
 * @return The timestamp of the last message received from the remote side.
 */
Trap.Transport.prototype.lastAlive = function(){};

/**
 * Attempts to verify if the transport is alive, or has been alive within a
 * certain number of milliseconds. Effectively, this can be used to trigger
 * a keepalive check of the transport if used with a <i>within</i> parameter
 * of 0 and a <i>check</i> parameter of true.
 * <p>
 * This function has a two-part purpose. The first is for the upper layer to
 * be able to check the last known liveness of the transport, to reduce the
 * discovery time of a dead connection. The second is to trigger a check for
 * a dead transport, when the application needs to know that it has active
 * connectivity.
 * <p>
 * Note that in normal operation, the transport itself will report when it
 * has disconnected; the upper layer does not need to concern itself with
 * this detail unless it specifically needs to know that it has connectivity
 * right now.
 * 
 * @param within
 *            Within how many milliseconds the last activity of the
 *            transport should have occurred before the transport should
 *            question whether it is alive.
 * @param check
 *            Whether the transport should attempt to check for liveness, or
 *            simply return false if the last known activity of the
 *            transport is not later than within.
 * @param timeout
 *            If check is true, how many milliseconds at most the liveness
 *            check should take before returning false anyway. The
 *            application can use this value if it has a time constraint on
 *            it.
 * @return <i>true</i> if the connection is currently alive (including if
 *         this function successfully re-established the connection),
 *         <i>false</i> otherwise.
 */
Trap.Transport.prototype.isAlive = function(within, check, timeout, callback){};

/**
 * Called when a Trap Transport has received a TrapMessage.
 * 
 * @param message
 */
Trap.Transport.prototype.onmessage = function(evt){};

/**
 * Called when the Trap Transport changes state.
 * 
 * @param newState
 * @param oldState
 */
Trap.Transport.prototype.onstatechange = function(evt){};

/**
 * Called when the Trap Transport knows that it has failed to send message(s)
 * 
 * @param messages
 */
Trap.Transport.prototype.onfailedsending = function(evt){};
Trap.AbstractTransport = function()
{

	Trap.EventObject.call(this);

	this._headersMap		= new Trap.Map();
	this._configuration		= new Trap.Configuration();
	this._prefix 			= "trap.transport." + this.getTransportName().toLowerCase();
	this._authentication	= new Trap.Authentication();

	this._delegate			= null;
	this._delegateContext	= null;

	this._availableKeys		= [];
	this._contextKeys		= [];
	this._contextMap		= new Trap.Map();

	this._transportPriority	= 0;
	
	this._lastAckTimestamp 	= 0;
	this._acks				= null;
	this._ackTask			= 0;
	
	this._format 			= Trap.Constants.MESSAGE_FORMAT_DEFAULT;

	this.logger 			= Trap.Logger.getLogger(this.getTransportName());
	this.fillAuthenticationKeys(this.availableKeys);

	Trap._compat.__defineSetter(this, 'transportPriority', function(newPrio) {
		this._transportPriority = newPrio;
	});
	Trap._compat.__defineGetter(this, 'transportPriority', function() {
		return this._transportPriority;
	});

	Trap._compat.__defineSetter(this, 'configuration', function(newConfig){
		this._configuration = newConfig;
		this.updateConfig();
	});

	Trap._compat.__defineGetter(this, 'configuration', function() {
		return this._configuration.toString();
	});

	Trap._compat.__defineGetter(this, 'state', function() {
		return this._state;
	});

	Trap._compat.__defineSetter(this, 'trapID', function(newID){
		this._trapID = newID;
	});

	Trap._compat.__defineGetter(this, 'trapID', function() {
		return this._trapID;
	});

	Trap._compat.__defineGetter(this, "enabled", function() {
		return this._enabled;
	});

	Trap._compat.__defineSetter(this, "enabled", function(b) {

		if (typeof(b) != "boolean")
			throw "Cannot set to a non-boolean value. Please set enabled to true or false";

		if (b)
			this.enable();
		else
			this.disable();
	});

	Trap._compat.__defineGetter(this, 'keepaliveInterval', function() {
		return this._keepalivePredictor.getKeepaliveInterval();
	});

	Trap._compat.__defineSetter(this, 'keepaliveInterval', function(newInterval) {
		this._keepalivePredictor.setKeepaliveInterval(newInterval);

		if (this.state == Trap.Transport.State.CONNECTED || this.state == Trap.Transport.State.AVAILABLE)
			this._keepalivePredictor.start();
	});

	Trap._compat.__defineGetterSetter(this, 'keepalivePredictor', '_keepalivePredictor');

	Trap._compat.__defineGetterSetter(this, 'keepaliveExpiry', null, function() {
		return this._keepalivePredictor.getKeepaliveExpiry();
	}, function(newExpiry) {
		this._keepalivePredictor.setKeepaliveExpiry(newExpiry);
	});

	Trap._compat.__defineGetter(this, "format", function() {
		return this._format;
	});

	Trap._compat.__defineSetter(this, "format", function(f) {
		this._format = f;
	});

	Trap.AbstractTransport.prototype.init.call(this);
};

Trap.AbstractTransport.prototype = new Trap.EventObject;
Trap.AbstractTransport.prototype.constructor = Trap.AbstractTransport;

Trap.AbstractTransport.prototype.init = function()
{
	this._enabled			= Trap.Constants.TRANSPORT_ENABLED_DEFAULT;
	this._state				= Trap.Transport.State.DISCONNECTED;
	this._trapID			= 0;

	this.lastAlive			= 0;
	this._livenessCheckData	= null;

	this.connectTimeout 	= 30000;

	// Used by the receive method to buffer as needed

	this._bos	= this.useBinary ? new Trap.ByteArrayOutputStream() : new Trap.ByteStringOutputStream();

	// Keepalive information
	if (this._keepalivePredictor)
		this._keepalivePredictor.stop();

	this._keepalivePredictor = new Trap.Keepalive.StaticPredictor();
	this._keepalivePredictor.setDelegate(this);
	
	this.messagesInTransit = new Trap.List();
	this.transportMessageBuffer = new Trap.List();
	
	if (this.connectionTimeoutTask)
		clearTimeout(this.connectionTimeoutTask);
	this.connectionTimeoutTask = null;
	
	if (this.disconnectExpiry != null)
		clearTimeout(this.disconnectExpiry);
	this.disconnectExpiry = null;
	
};

/* **** ABSTRACT METHODS!!! MUST BE SUBCLASSED!!! */

/**
 * Asks the transport to fill the set with the available context keys it can
 * provide for authentication. These keys will be offered to the
 * authentication provider, and can not be changed after the call to this
 * function. The keys are set on a per-transport basis.
 * <p>
 * This function MUST NOT throw.
 * 
 * @param keys
 *            The keys to fill in. The transport should only add keys to
 *            this set.
 */
Trap.AbstractTransport.prototype.fillAuthenticationKeys = function(keys) {};

/**
 * Asks the subclass to update the context map, filling in the keys. This
 * can be called, for example, when a new authentication method is set that
 * may have modified contextKeys.
 */
Trap.AbstractTransport.prototype.updateContext = function() {};

/**
 * Performs the actual sending of a TrapMessage. This method MUST NOT
 * perform any checks on the outgoing messages. It may still perform checks
 * on the transport, and throw appropriately.
 * 
 * @param {Trap.Message} message
 *            The message to send.
 *            @param {Boolean} expectMore Deprecated. Must be set to true.
 * @throws TrapTransportException
 *             If an error occurred while trying to send the message. Before
 *             this exception is thrown, the transport MUST change its state
 *             to ERROR, as it means this transport can no longer be used.
 */
Trap.AbstractTransport.prototype.internalSend = function(message, expectMore) {};


/**
 * Triggers the connect call for the transport (if available). May throw if it cannot connect.
 */
Trap.AbstractTransport.prototype.internalConnect = function(){};

Trap.AbstractTransport.prototype.internalDisconnect = function(){};

Trap.AbstractTransport.prototype.getTransportName = function(){ return "abstract"; };

/*
 * Implementation follows. Feel free to ignore.
 */

Trap.AbstractTransport.prototype.isEnabled = function()
{
	return this._enabled;
};

Trap.AbstractTransport.prototype.isConnected = function()
{
	return this.getState() == Trap.Transport.State.CONNECTED || this.getState() == Trap.Transport.State.AVAILABLE;
};

Trap.AbstractTransport.prototype.configure = function(configurationKey, configurationValue) 
{
	if (!configurationKey.startsWith(this._prefix))
		configurationKey = this._prefix + "." + configurationKey;
	this._configuration.setOption(configurationKey, configurationValue);
	this.updateConfig();
};

Trap.AbstractTransport.prototype.updateConfig = function()
{
	var eString = this.getOption(Trap.Transport.Options.Enabled);
	if (eString != null)
	{
		try
		{
			this._enabled = ("true" == eString);
		}
		catch (e)
		{
			this.logger.warn("Failed to parse transport {} enabled flag; {}", this.getTransportName(), e);
		}
	}

	this.transportPriority = this._configuration.getIntOption(Trap.Transport.Options.Priority, this.transportPriority);

	this.keepaliveInterval = this._configuration.getIntOption("trap.keepalive.interval", this.keepaliveInterval);
	this.keepaliveExpiry = this._configuration.getIntOption("trap.keepalive.expiry", this.keepaliveExpiry);

};

Trap.AbstractTransport.prototype.canConnect = function()
{
	return false;
};

Trap.AbstractTransport.prototype.canListen = function()
{
	return false;
};

Trap.AbstractTransport.prototype.setTransportDelegate = function(newDelegate, newContext)
{
	this.delegate = newDelegate;
	this._delegateContext = newContext;

	this.onmessage = function(e)
	{
		newDelegate.ttMessageReceived(e.message, this, newContext);
	};
	this.onmessagesent = function(e)
	{
		newDelegate.ttMessageSent(e.message, this, newContext);
	};
	this.onstatechange = function(e)
	{
		newDelegate.ttStateChanged(e.newState, e.oldState, this, newContext);
	};
	this.onfailedsending = function(e)
	{
		newDelegate.ttMessagesFailedSending(e.messages, this, newContext);
	};
	
};

Trap.AbstractTransport.prototype.setAuthentication = function(authentication)
{
	this._authentication = authentication;
	this.contextKeys = authentication.getContextKeys(this.availableKeys);
	this.updateContext();
};

Trap.AbstractTransport.prototype.isAvailable = function()
{
	return this.state == Trap.Transport.State.AVAILABLE;
};

/**
 * Changes the state of the transport, and notifies the listener.
 * 
 * @param newState
 *            The state to change to.
 */
Trap.AbstractTransport.prototype.setState = function(newState)
{
	if (newState == this._state)
		return;

	var oldState = this._state;
	this._state = newState;

	if (this.delegate == null)
		this.logger.trace("Transport {} changed state from {} to {}", this.getTransportName(), oldState, newState );

	try
	{
		this._dispatchEvent({type: "statechange", newState:newState, oldState:oldState});
	}
	catch(e)
	{
		this.logger.error("Exception while dispatching statechange: {}", e);
	}
	
	if ((newState == Trap.Transport.State.AVAILABLE) && (this.connectionTimeoutTask != null))
	{
		clearTimeout(this.connectionTimeoutTask);
		this.connectionTimeoutTask = null;
	}

	// Autostart keepalives, if applicable.
	if (newState == Trap.Transport.State.CONNECTED)
	{
		this._keepalivePredictor.start();
	}

	// Autostart keepalives, if applicable.
	if ((newState == Trap.Transport.State.DISCONNECTED) || (newState == Trap.Transport.State.DISCONNECTING) || (newState == Trap.Transport.State.ERROR))
	{
		this._keepalivePredictor.stop();
		
		if (this.disconnectExpiry != null)
			clearTimeout(this.disconnectExpiry);
		this.disconnectExpiry = null;
		
		if (this.messagesInTransit.size() > 0)
		{
			this._dispatchEvent({type: "failedsending", data: this.messagesInTransit, messages: this.messagesInTransit});
		}
	}
	if ((newState == Trap.Transport.State.AVAILABLE) && (oldState == Trap.Transport.State.UNAVAILABLE))
	{
		var mt = this;
		setTimeout(function() {
			mt.flushTransportMessages(false);
		}, 5);
	}
};

Trap.AbstractTransport.prototype.enable = function()
{
	try
	{
		this.configure(Trap.Transport.Options.ENABLED, "true");
	}
	catch (e)
	{
		// Cannot happen.
		this.logger.warn(e.getMessage(), e);
	}
};

Trap.AbstractTransport.prototype.disable = function()
{
	try
	{
		this.configure(Trap.Transport.Options.ENABLED, "false");
	}
	catch (e)
	{
		this.logger.warn(e);
	}
	this.disconnect();
};

Trap.AbstractTransport.prototype.connect = function()
{
	if (!this.isEnabled())
		throw "Transport "+this.getTransportName()+" is unavailable...";

	if (!this.canConnect())
		throw "Transport "+this.getTransportName()+" cannot act as a client";

	if (this.getState() != Trap.Transport.State.DISCONNECTED)
		throw "Cannot connect from state that is not DISCONNECTED";
	
	if (!this.isClientConfigured())
	{
		this.logger.debug("Configuration Error. {} not properly configured... Unless autoconfigure is enabled (and another transport succeeds) this transport will not be available.", this);
		this.setState(Trap.Transport.State.ERROR);
		return;
	}

	var mt = this;

	this.connectionTimeoutTask = setTimeout(function() {
		if (mt.getState() == Trap.Transport.State.CONNECTING)
		{
			mt.logger.debug("Connection Error. {} failed to move to state OPEN after 15 seconds... purging it", mt);
			mt.setState(Trap.Transport.State.ERROR);
			mt.internalDisconnect();
		}
	}, this.connectTimeout);

	this.setState(Trap.Transport.State.CONNECTING);
	this.internalConnect();
};

Trap.AbstractTransport.prototype.disconnect = function()
{
	if (this.state == Trap.Transport.State.DISCONNECTING || this.state == Trap.Transport.State.DISCONNECTED || this.state == Trap.Transport.State.ERROR)
		return; // Cannot re-disconnect
		
	if (this.getState() == Trap.Transport.State.CONNECTING)
	{
		this.internalDisconnect();
		return;
	}

	this.setState(Trap.Transport.State.DISCONNECTING);
	this.internalSend(this.createMessage().setOp(Trap.Message.Operation.CLOSE), false);
	
	this.keepalivePredictor.dataSent();
	
	var mt = this;
	this.disconnectExpiry = setTimeout(function() {
		if (mt.getState() != Trap.Transport.State.DISCONNECTED && mt.getState == Trap.Transport.State.ERROR)
		{
			mt.internalDisconnect();
			mt.logger.debug("Disconnection Error. {} moving to state ERROR as failed to disconnect in time. Triggering state was {}", mt, cs);
			mt.setState(Trap.Transport.State.ERROR);
		}
		mt.disconnectExpiry = null;
		
	}, 5000);
	this.internalDisconnect();
};

/* Transport (Abstract) logic follows! This logic will refer to the MOST PARANOID TRANSPORT and MUST be overridden by LESS PARANOID transports */

/**
 * Send checks if the transport is in the correct state, if the message is
 * authenticated (otherwise adds authentication) and performs additional
 * checks when needed.
 * @param {Trap.Message} message
 * @param {Boolean} expectMore
 */
Trap.AbstractTransport.prototype.send = function(message, expectMore) 
{
	if (this.state != Trap.Transport.State.AVAILABLE && this.state != Trap.Transport.State.CONNECTED)
		throw {message: message, state: this.state};

		message.setAuthData(this._authentication.createAuthenticationResponse(null, this.headersMap, message.getData(), this.contextMap));
		
		if (this.logger.isTraceEnabled())
			this.logger.trace("Sending {}/{} on transport {} for {}.", message.getOp(), message.getMessageId(), this, this.delegate );
		
		this.internalSend(message, expectMore);

		if (message.getMessageId() != 0)
			this.addTransitMessage(message);

		this._keepalivePredictor.dataSent();
};

Trap.AbstractTransport.prototype.sendTransportSpecific = function(message)
{
	message.setAuthData(this._authentication.createAuthenticationResponse(null, this.headersMap, message.getData(), this.contextMap));
	this.transportMessageBuffer.add(message);
	this.flushTransportMessages(false);
};

Trap.AbstractTransport.prototype.flushTransportMessages = function(expectMoreAtEnd)
{

	while ((this.getState() == Trap.Transport.State.AVAILABLE || this.getState() == Trap.Transport.State.CONNECTED || this.getState() == Trap.Transport.State.CONNECTING || this.getState() == Trap.Transport.State.DISCONNECTING) && (this.transportMessageBuffer.size() > 0))
	{
		var message = null;
		try
		{
			message = this.transportMessageBuffer.remove();
			if (this.logger.isTraceEnabled())
				this.logger.trace("Sending {}/{} on transport {} for {}.", message.getOp(), message.getMessageId(), this, this.delegate );
			this.internalSend(message, expectMoreAtEnd ? true : this.transportMessageBuffer.size() > 0);
			this._keepalivePredictor.dataSent();
		}
		catch (e)
		{
			this.transportMessageBuffer.addFirst(message);
		}
	}
	
	this.flushTransport();
};

/**
 * Call this when data is received.
 * 
 * @param data
 */
Trap.AbstractTransport.prototype.receive = function(data, offset, length)
{
	try
	{
		// We need to handle the case where message data is spread out over two or more incoming data blobs (e.g. socket, udp, etc)...
		// Therefore, we'll need to do some buffer shuffling.

		this._bos.write(data, offset, length);
		var dataArray = this._bos.toArray();
		var consumed = 0;

		do
		{
			var m = new Trap.Message();
			var thisLoop = m.deserialize(dataArray, consumed, dataArray.length - consumed);

			if (thisLoop == -1)
			{
				break;
			}

			this.receiveMessage(m);

			consumed += thisLoop;
		} while (consumed < dataArray.length);

		if (consumed > 0)
		{
			this._bos = this.useBinary ? new Trap.ByteArrayOutputStream() : new Trap.ByteStringOutputStream();
			try
			{
				this._bos.write(dataArray, consumed, dataArray.length - consumed);
			}
			catch (t)
			{
				this.logger.warn(t);
			}
		}
	}
	catch (e)
	{
		this.logger.warn(e);
		try
		{
			this.sendTransportSpecific(this.createMessage().setOp(Trap.Message.Operation.END));
		}
		catch (e1)
		{
			this.logger.warn(e1);
		}

		// Close the transport, since it's invalid
		// It's illegal to raise an UnsupportedEncodingException at this point in time.
		this.disconnect();
	}
};

Trap.AbstractTransport.prototype.toString = function()
{
	return this.getTransportName() + "/" + this.getTrapID() + "/" + this.getState();
};

/**
 * Called when a message is received, in the most general case.
 * 
 * @param message
 */
Trap.AbstractTransport.prototype.receiveMessage = function(message)
{
	
	if (this.logger.isTraceEnabled())
		this.logger.trace("Received: {}/{} on {} for {}", message.getOp(), message.getMessageId(), this, this.delegate);

	this.lastAlive = new Date().getTime();
	this._keepalivePredictor.dataReceived();
	// Authenticated message.

	var propagate = true;

	// Note to leo: I hate retarded, k?
	switch (message.getOp())
	{
	case 1:
		propagate = this.onOpen(message);
		break;

	case 2:
		propagate = this.onOpened(message);
		break;

	case 3:
		propagate = this.onClose(message);
		break;

	case 4:
		propagate = this.onEnd(message);
		break;

	case 5:
		propagate = this.onChallenge(message);
		break;

	case 6:
		propagate = this.onError(message);
		break;

	case 8:
	case Trap.Message.Operation.FRAGMENT_START:
	case Trap.Message.Operation.FRAGMENT_END:
		propagate = this.onMessage(message);
		break;

	case 9:
		propagate = false;
		this.onAck(message);
		break;

	case 16:
		propagate = this.onOK(message);
		break;

	case 17:
		propagate = this.onPing(message);
		break;

	case 18:
		propagate = this.onPong(message);
		break;

	case 19:
		propagate = this.onTransport(message);
		break;

	default:
		return;

	}

	if (propagate)
	{
		this._dispatchEvent({type: "message", data: message, message: message});
		this.acknowledgeTransitMessage(message);
	}
};

/**
 * Transport messages are most often handled by the Trap layer, then
 * repropagated down. The transport CAN attempt to intercept some but it is
 * NOT recommended.
 * 
 * @param message
 * @return {Boolean} Whether to propagate
 */
Trap.AbstractTransport.prototype.onTransport = function(message)
{
	var authed = this.checkAuthentication(message);

	if (authed)
	{

	}

	return authed;
};

/**
 * Ping/Pong should be left to Trap.
 * 
 * @param message
 * @return {Boolean} Whether to propagate
 */
Trap.AbstractTransport.prototype.onPong = function(message)
{
	var authed = this.checkAuthentication(message);

	if (authed)
	{
		//if (this.livenessCheck)
		//	this.livenessCheck(message.getData());

		var bs = message.getDataAsString();
		var type = bs[0];
		var data = bs.substring(7);

		var timer = parseInt(bs.substring(1, 7));

		if (isNaN(timer))
			timer = 30;


		if (type != '3')
			this._keepalivePredictor.keepaliveReceived(false, type, timer, data);
		else if (this.livenessCheck)
			this.livenessCheck(data);
	}

	return authed;
};

/**
 * Ping/Pong should be left to Trap.
 * 
 * @param message
 * @return {Boolean} Whether to propagate
 */
Trap.AbstractTransport.prototype.onPing = function(message)
{
	var authed = this.checkAuthentication(message);

	if (authed)
	{
		
		// Prevent a silly error where an old PING would trigger a PONG while disconnecting
		if (this.getState() == Trap.Transport.State.DISCONNECTING || this.getState() == Trap.Transport.State.DISCONNECTED)
			return authed;
		
		try
		{
			var bs = message.string;
			var type = bs.substring(0, 1);
			var timer = parseInt(bs.substring(1, 7));
			var data = bs.substring(7);

			if (isNaN(timer))
				timer = 30;

			if (type != '3')
				this._keepalivePredictor.keepaliveReceived(true, type, timer, data);
			else
				// isAlive() call
				this.sendKeepalive(false, type, timer, data);
		}
		catch (e)
		{
			this.logger.warn(e);
		}
	}

	return authed;
};

Trap.AbstractTransport.prototype.padTimer = function(timerStr)
{
	while (timerStr.length < 6)
	{
		if (timerStr.startsWith("-"))
			timerStr = "-0" + timerStr.substring(1);
		else
			timerStr = "0" + timerStr;
	}
	return timerStr;
};

/**
 * General ack. Used by Trap; the transport need not apply.
 * 
 * @param message
 * @return {Boolean} Whether to propagate
 */
Trap.AbstractTransport.prototype.onOK = function(message)
{
	var authed = this.checkAuthentication(message);

	if (authed)
	{

	}

	return authed;
};

/**
 * Transport should not care for these.
 * 
 * @param message
 * @return {Boolean} Whether to propagate
 */
Trap.AbstractTransport.prototype.onMessage = function(message)
{
	var authed = this.checkAuthentication(message);

	if (authed)
	{

	}

	return authed;
};

/**
 * Transport MAY inspect these.
 * 
 * @param message
 * @return {Boolean} Whether to propagate
 */
Trap.AbstractTransport.prototype.onError = function(message)
{
	var authed = this.checkAuthentication(message);

	if (authed)
	{

	}

	return authed;
};

/**
 * Transport MUST intercept these.
 * 
 * @param message
 * @return {Boolean} Whether to propagate
 */
Trap.AbstractTransport.prototype.onChallenge = function(message)
{
	// We received a challenge.

	try
	{
		var original = new Trap.Message(message.getData());
		var response = this._authentication.createAuthenticationResponse(message.getAuthData(), this.headersMap, original.getData(), this.contextMap);
		original.setAuthData(response);
		this.sendTransportSpecific(original);
	}
	catch (e)
	{
		this.logger.warn(e);
	}

	return false;
};

/**
 * Transport MUST NOT intercept these
 * 
 * @param message
 * @return {Boolean} Whether to propagate
 */
Trap.AbstractTransport.prototype.onEnd = function(message)
{
	var authed = this.checkAuthentication(message);

	if (authed)
	{

	}

	return authed;
};

/**
 * Transport MUST intercept these.
 * 
 * @param message
 * @return {Boolean} Whether to propagate
 */
Trap.AbstractTransport.prototype.onClose = function(message)
{
	var authed = this.checkAuthentication(message);

	if (authed)
	{
		this.disconnect();
		this.internalDisconnect();
		this.setState(Trap.Transport.State.DISCONNECTED);
	}

	return false;
};

/**
 * Transport MUST intercept these.
 * 
 * @param message
 * @return {Boolean} Whether to propagate
 */
Trap.AbstractTransport.prototype.onOpened = function(message)
{
	var authed = this.checkAuthentication(message);

	if (authed)
	{
		if (this.getState() == Trap.Transport.State.UNAVAILABLE || this.getState() == Trap.Transport.State.CONNECTED)
			this.setState(Trap.Transport.State.AVAILABLE);
		else
			this.logger.debug("Potential race: Transport received onOpen while not connecting");
	}

	return authed;
};

/**
 * Transport MUST intercept these.
 * 
 * @param message
 * @return {Boolean} Whether to propagate
 */
Trap.AbstractTransport.prototype.onOpen = function(message)
{
	var authed = this.checkAuthentication(message);

	if (authed)
	{
		if (this.getState() == Trap.Transport.State.UNAVAILABLE || this.getState() == Trap.Transport.State.CONNECTED)
			this.setState(Trap.Transport.State.AVAILABLE);
		else
			this.logger.debug("Potential race: Transport received onOpen while not connecting");
	}
	else
	{
		// The challenge will have been sent by checkAuth
		// We don't really need to do anything; we'll receive a new
		// OPEN event...
	}

	return authed;
};

Trap.AbstractTransport.prototype.checkAuthentication = function(message)
{

	var authed = this._authentication.verifyAuthentication(message.getAuthData(), this.headersMap, message.getData(), this.contextMap);

	if (!authed)
	{
		// Challenge
		var authChallenge = this._authentication.createAuthenticationChallenge(this.contextMap);
		var challenge = this.createMessage();
		challenge.setOp(Trap.Message.Operation.CHALLENGE);
		challenge.setAuthData(authChallenge);

		try
		{
			challenge.setData(message.serialize());
			this.internalSend(challenge, false);
			this._keepalivePredictor.dataSent();
		}
		catch (e)
		{
			this.logger.warn("Something happened: {}", e);
		}
	}

	return authed;
};

Trap.AbstractTransport.prototype.getOption = function(option)
{
	if (!option.startsWith(this._prefix))
		option = this._prefix + "." + option;

	return this._configuration.getOption(option);
};

Trap.AbstractTransport.prototype.isAlive = function(within, check, timeout, callback)
{
	if (new Date().getTime() - within < this.lastAlive)
	{
		callback(true);
		return;
	}

	if (!check)
	{
		callback(false);
		return;
	}


	if (this.livenessCheckData == null)
	{
		var mt = this;
		this.livenessCheckData = "" + new Date().getTime();
		this.livenessCheck = function(data)
		{
			if (mt.livenessCheckData == data)
			{
				clearTimeout(mt.livenessCheckTimeout);
				callback(true);
			}
		};

		// Don't allow multiple calls to time us out
		if (this.livenessCheckTimeout)
			clearTimeout(this.livenessCheckTimeout);

		this.livenessCheckTimeout = setTimeout(function() {
			callback(false);
		}, timeout);
	}

	this.sendKeepalive(true, '3', this._keepalivePredictor.getNextKeepaliveSend(), this.livenessCheckData);

};

Trap.AbstractTransport.prototype.sendKeepalive = function(ping, type, timer, data)
{

	if (typeof(type) == "undefined" || typeof(timer) == "undefined" || typeof(data) == "undefined")
		throw "Invalid call; Bug.";

	if (type.length != 1)
		throw "Invalid type";

	timer = ""+timer;

	// Now perform the blaady check
	try
	{

		var m = this.createMessage();

		if (ping)
			m.setOp(Trap.Message.Operation.PING);
		else
			m.setOp(Trap.Message.Operation.PONG);

		// Prepare the data. Start with padding timer (0-padded to exactly six characters)
		timer = this.padTimer(timer);

		data = type + timer + data;
		m.setData(data);

		this.sendTransportSpecific(m);

	}
	catch (e)
	{
		this.logger.error(e);
	}


};

Trap.AbstractTransport.prototype.predictedKeepaliveExpired = function(predictor, msec)
{
	this.logger.debug("Keepalive timer for {} expired. Moving to DISCONNECTED.", this.getTransportName());
	this.setState(Trap.Transport.State.DISCONNECTED);
};

Trap.AbstractTransport.prototype.shouldSendKeepalive = function(isPing, type, timer, data)
{
	this.logger.trace("Sending keepalive: {} | {} | {} | {}", isPing, type, timer, data);
	this.sendKeepalive(isPing, type, timer, data);
};

Trap.AbstractTransport.prototype.warnAddressConfiguration = function()
{
	if (this.warnAddressConfigurationPerformed)
		return;
	
	if (!this.configuration.getBooleanOption("warnAddressConfiguration", true))
		return;
	
	this.warnAddressConfigurationPerformed = true;
	
	this.logger.warn("Configuration Error: {} could not detect a single public address; may need configuration!", this);
};

/**
 * Should try and resolve the hostname for an IP address. Servers should override.
 * @param {String} address
 * @param {Boolean} defaultConfig
 * @param {Boolean} failOnUnreachable
 * @return {String}
 */
Trap.AbstractTransport.prototype.getHostName = function(address, defaultConfig, failOnUnreachable)
{
	return "localhost";
};

/**
 * @param {Boolean} client
 * @param {Boolean} server
 * @return {Boolean}
 */
Trap.AbstractTransport.prototype.isConfigured = function(client, server)
{
	var rv = true;
	
	if (client)
		rv = rv && this.isClientConfigured();
	
	if (server)
		rv = rv && this.isServerConfigured();
	
	return rv;
};

/**
 * Asks whether the transport has the proper configuration for its server
 * role. Must return false if the transport cannot be a server.
 * 
 * @return {Boolean}
 */
Trap.AbstractTransport.prototype.isServerConfigured = function()
{
	// Most servers are configured by default, so we'll adjust the default accordingly
	return this.canListen();
};

/**
 * Asks whether the transport has the proper configuration for its client
 * role. Must return false if the transport cannot be a client.
 * 
 * @return {Boolean}
 */
Trap.AbstractTransport.prototype.isClientConfigured = function()
{
	return false;
};

Trap.AbstractTransport.prototype.forceError = function()
{
	if (this.logger.isTraceEnabled())
	{
		this.logger.trace("Error was forced");
	}
	this.setState(Trap.Transport.State.ERROR);
};

/**
 * The following methods deal with messages-in-transit
 * @param {Trap.Message} message
 */

Trap.AbstractTransport.prototype.onAck = function(message)
{
	// TODO Auto-generated method stub
	var data = message.getData();
	
	if (message.getFormat() == Trap.Message.Format.REGULAR)
	{
		var messageID, channelID;
		
		for (var i=0; i<data.length; i+=5)
		{
			channelID = data[i];
			messageID = Trap.ByteConverter.fromBigEndian(data, i+1);
			this.removeTransitMessageById(messageID, channelID);
		}
		
	}
	else
	{
		
		for (var i=0; i<data.length; i+=4)
		{
			var messageID = Trap.ByteConverter.fromBigEndian7(data, i);
			this.removeTransitMessageById(messageID, 0);
		}

	}
	
};

/**
 * Notes to the system that a message is in transit. Some transports (e.g.
 * loopback) are unconcerned about this. HTTP can also reasonably deduce
 * when a transit message has arrived. However, some transports (e.g.
 * socket) have no built-in acknowledgement sequence for a complete message,
 * and may be broken during the course of a transfer.
 * <p>
 * The transit message features allow transports to use built-in methods to
 * detect these failures and trigger ttMessagesFailedSending. This will
 * allow Trap to recover these messages.
 * <p>
 * Override this method to disable transit checking. For performance
 * reasons, {@link #acknowledgeTransitMessage(TrapMessage)} should be
 * overridden as well.
 *
 * @param m
 */
Trap.AbstractTransport.prototype.addTransitMessage = function(m)
{
	if (m.getMessageId() == 0)
		return;
	this.messagesInTransit.add(m);
};

Trap.AbstractTransport.prototype.removeTransitMessageById = function(id, channelID)
{
	var it = this.messagesInTransit.iterator();

	var first = true;

	while (it.hasNext())
	{
		var m = it.next();

		if (m.getMessageId() == id && m.getChannel() == channelID)
		{
			it.remove();

			if (!first)
			{
				// This implies dropped messages!!!
				this.logger.error("It appears we have dropped some messages on an otherwise working transport. Most likely, this transport is bugged; please report this.");
			}
			
			this._dispatchEvent({type: "messagesent", data: m, message: m});
			return;
		}

		first = false;
	}
};

Trap.AbstractTransport.prototype.acknowledgeTransitMessage = function(message)
{
	
	if (message.getMessageId() == 0)
		return;
	
	if (!this._acks)
		this._acks = this.useBinary && this.getFormat() == Trap.Message.Format.REGULAR ? new Trap.ByteArrayOutputStream() : new Trap.ByteStringOutputStream();
	
	if (this._acks.length > 512)
		this.flushAcks();
	
	
	if (this.getFormat() == Trap.Message.Format.REGULAR)
	{
		this._acks.write(message.getChannel());
		this._acks.write(Trap.ByteConverter.toBigEndian(message.getMessageId()));
	}
	else
	{
		this._acks.write(Trap.ByteConverter.toBigEndian7(message.getMessageId()));
	}
	
	
	var ctm = new Date().valueOf();
	
	// Delay acks if we've already sent an ack within the last 5 ms.
	if (this._lastAckTimestamp >= (ctm -5))
	{
		if (!this._ackTask)
		{
			var mt = this;
			this._ackTask = setTimeout(function() {mt.flushAcks();}, 6);
		}
	}
	else
	{
		this.flushAcks();
	}
	
};

Trap.AbstractTransport.prototype.flushAcks = function()
{
	this._lastAckTimestamp = new Date().valueOf();
	var ack = this.createMessage();

	ack.setOp(Trap.Message.Operation.ACK);
	
	ack.setData(this._acks.toArray());
	this._acks.clear();
	
	this.sendTransportSpecific(ack);
	
};


Trap.AbstractTransport.prototype.createMessage = function() {
	var m = new Trap.Message();
	m.setFormat(this.format);
	return m;
};

Trap.AbstractTransport.prototype.isObjectTransport = function() {
	return false;
};

/**
 * @returns {Trap.Transports.WebSocket}
 */
Trap.Transports.WebSocket = function()
{
	Trap.AbstractTransport.call(this);
	this._transportPriority	= 0;
	this.keepaliveInterval = 28; // 28 seconds keepalive should keep us open through most NATs...
};

Trap.Transports.WebSocket.prototype = new Trap.AbstractTransport;
Trap.Transports.WebSocket.prototype.constructor = new Trap.Transports.WebSocket;

Trap.Transports.WebSocket.CONFIG_URI = "wsuri";

// Binary detection
try 
{ 
	var ws = new WebSocket("wss://127.0.0.1");
	
	if (typeof ws.binaryType === "string")
	    Trap.Transports.WebSocket.prototype.supportsBinary = ws.binaryType === "blob"; 
	
} catch(e){}

Trap.Transports.WebSocket.prototype.canConnect = function()
{
	// Check for WebSocket interface
	return (typeof(WebSocket) != "undefined" && WebSocket.prototype && WebSocket.prototype.send ? true : false);
};

Trap.Transports.WebSocket.prototype.getTransportName = function()
{
	return "websocket";
};

Trap.Transports.WebSocket.prototype.init = function() 
{

	Trap.AbstractTransport.prototype.init.call(this);
	
	if (this.ws)
	{
		this.ws.onopen = function() { };
		this.ws.onerror = function() { };
		this.ws.onclose = function() { };
		this.ws.onmessage = function() { };
	}
		
	this.ws = null;
	
};

Trap.Transports.WebSocket.prototype.getProtocolName = function()
{
	return "websocket";
};

Trap.Transports.WebSocket.prototype.internalSend = function(message, expectMore) 
{
	var data = message.serialize(this.useBinary);
	this.ws.send(data.buffer ? data.buffer : data);
};

Trap.Transports.WebSocket.prototype.flushTransport = function()
{
};

Trap.Transports.WebSocket.prototype.isClientConfigured = function()
{
	return !!this.getOption(Trap.Transports.WebSocket.CONFIG_URI);
};

Trap.Transports.WebSocket.prototype.internalConnect = function()
{
	var uri = this.getOption(Trap.Transports.WebSocket.CONFIG_URI);
	if (!uri)
	{
		this.logger.debug("WebSocket Transport not properly configured... Unless autoconfigure is enabled (and another transport succeeds) this transport will not be available.");
		this.setState(Trap.Transport.State.ERROR);
		return;
	}
	
	var uri = this.getOption(Trap.Transports.WebSocket.CONFIG_URI);
	this.logger.debug("WS Transport Opening");
	this.ws = new WebSocket(uri);
	this._initWS();
};

Trap.Transports.WebSocket.prototype.internalDisconnect = function()
{
	
	if ((this.getState() != Trap.Transport.State.DISCONNECTED) && (this.getState() != Trap.Transport.State.DISCONNECTED) && (this.getState() != Trap.Transport.State.ERROR))
			this.setState(Trap.Transport.State.DISCONNECTING);

	if (this.ws)
		this.ws.close();
	
};

//TODO: Expose IP information on websocket level...
Trap.Transports.WebSocket.prototype.fillAuthenticationKeys = function(keys)
{
};

Trap.Transports.WebSocket.prototype.updateContext = function()
{
	// TODO Auto-generated method stub
	
};

Trap.Transports.WebSocket.prototype._initWS = function()
{
	var mt = this;
	this.ws.onopen = function() { mt.notifyOpen(); };
	this.ws.onerror = function() { mt.notifyError(); };
	this.ws.onclose = function() { mt.notifyClose(); };
	this.ws.onmessage = function(e) { mt.notifyMessage(e.data); };
	
	if (this.useBinary && this.supportsBinary)
		this.ws.binaryType = "arraybuffer";
};

Trap.Transports.WebSocket.prototype.notifyError = function()
{
	this.setState(Trap.Transport.State.ERROR);
};

Trap.Transports.WebSocket.prototype.notifyOpen = function()
{
	this.logger.debug("WS Transport Connected");
	this.setState(Trap.Transport.State.CONNECTED);
};

Trap.Transports.WebSocket.prototype.notifyClose = function()
{
	this.ws = null;
	if(this.getState() != Trap.Transport.State.ERROR)
		this.setState(Trap.Transport.State.DISCONNECTED);
	this.logger.debug("WS Transport Disconnected");
};

Trap.Transports.WebSocket.prototype.notifyMessage = function(data)
{
	if (typeof(data) == "string")
	{
		// Data will be a Unicode string (16-bit chars). notifyData expects bytes though
		// Encode data as UTF-8. This will align the bytes with the ones expected from the server.
		data = data.toUTF8ByteArray();
		
		this.receive(data, 0, data.length);
	}
	else
	{
		this.receive(new Uint8Array(data));
	}
};
Trap.Transports.HTTP = function()
{
	Trap.AbstractTransport.call(this);
	this._transportPriority	= 100;
	this.expirationDelay = 28000;
	this.connectionTimeout = 10000;
	this.latencyEstimate = 1000; // Start with a reasonably generous latency estimate

	this._buf = [];
};

Trap.Transports.HTTP.prototype = new Trap.AbstractTransport;
Trap.Transports.HTTP.prototype.constructor = new Trap.Transports.HTTP;

Trap.Transports.HTTP.CONFIG_URL = "url";

try
{
	Trap.Transports.HTTP.prototype.supportsBinary = typeof new XMLHttpRequest().responseType === 'string';
}
catch(e)
{
	Trap.Transports.HTTP.prototype.supportsBinary = false;
}

//Trap.supportsBinary = Trap.supportsBinary && Trap.Transports.HTTP.prototype.supportsBinary;

Trap.Transports.HTTP.prototype.init = function() {
	
	this._buf = [];
	
	// Abort any pre-existing polls so they don't interfere with our state changes...
	if (this._longpoll)
		this._longpoll.onreadystatechange = function() {};
	
	Trap.AbstractTransport.prototype.init.call(this);
};

Trap.Transports.HTTP.prototype.getTransportName = function()
{
	return "http";
};

Trap.Transports.HTTP.prototype.getProtocolName = function()
{
	return "http";
};

Trap.Transports.HTTP.prototype.updateConfig = function()
{
	Trap.AbstractTransport.prototype.updateConfig.call(this);
	
	if ((this.getState() == Trap.Transport.State.DISCONNECTED) || (this.getState() == Trap.Transport.State.CONNECTING))
	{
		this.url = this.getOption(Trap.Transports.HTTP.CONFIG_URL);
	}
	else
		this.logger.debug("Updating HTTP configuration while open; changes will not take effect until HTTP is reconnected");

	this.expirationDelay = this._configuration.getIntOption(this._prefix + ".expirationDelay", this.expirationDelay);
	this.connectionTimeout = this._configuration.getIntOption(this._prefix + ".connectionTimeout", this.connectionTimeout);
};

//TODO: Expose IP information on websocket level...
Trap.Transports.HTTP.prototype.fillAuthenticationKeys = function(keys)
{
};

Trap.Transports.HTTP.prototype.updateContext = function()
{
	// TODO Auto-generated method stub

};

Trap.Transports.HTTP.prototype.internalSend = function(message, expectMore) 
{

	var mt = this;
	
	mt._sendQueued = true;

	if (!!message)
		this._buf.push(message);

	if (expectMore)
	{
		if (mt._sendTimer)
			clearTimeout(mt._sendTimer);
		
		mt._sendTimer = setTimeout(function() {
			mt.internalSend(null, false);
		}, 1000);
		
		return;
	}
	
	if (mt._sendTimer)
	{
	    clearTimeout(mt._sendTimer);
	    mt._sendTimer = null;   
	}
	
	if (this._buf.length == 0)
		return; // Erroneous call.

	// Slam the messages
	
	var bos = (this.useBinary ? new Trap.ByteArrayOutputStream() : new Trap.ByteStringOutputStream());
	for (var i=0; i<this._buf.length; i++)
		bos.write(this._buf[i].serialize(this.useBinary));
	
	var data = (this.useBinary ? bos.toArray() : bos.toString());
	
	if (mt.getState() == Trap.Transport.State.AVAILABLE || mt.getState() == Trap.Transport.State.CONNECTED)
		mt.setState(Trap.Transport.State.UNAVAILABLE);
	
	var x = this.openConnection("POST");
	x.setRequestHeader("Content-Type", "x-trap");

	x.send(data.buffer ? data.buffer : data);
	
	x.onreadystatechange = function()
	{
		if (x.readyState == 4)
		{ 
			if (x.hasError || x.hasTimeout || x.isAborted)
			{
				mt._dispatchEvent({type: "failedsending", messages: mt._buf});
				mt.setState(Trap.Transport.State.ERROR);
			}
			else
			{
				
				for (var i=0; i<mt._buf.length; i++)
					if (mt._buf[i].getMessageId() > 0)
						mt._dispatchEvent({type:"messagesent", data: mt._buf[i], message: mt._buf[i]});
				
				mt._buf = [];
				
				// Prevent state to go to AVAILABLE when we're actually DISCONNECTED.
				if (mt.getState() == Trap.Transport.State.UNAVAILABLE || mt.getState() == Trap.Transport.State.CONNECTED)
					mt.setState(Trap.Transport.State.AVAILABLE);
			}
			
			mt._sendQueued = false;
		}
	};

};

Trap.Transports.HTTP.prototype.isClientConfigured = function()
{
	return this.url && typeof(this.url) == "string" && this.url.length > 4;
};

Trap.Transports.HTTP.prototype.internalConnect = function()
{
	
	this.logger.debug("HTTP Transport Opening...");

	// Check for proper configuration
	if (!this.isClientConfigured())
	{
		this.logger.debug("HTTP Transport not properly configured... Unless autoconfigure is enabled (and another transport succeeds) this transport will not be available.");
		this.setState(Trap.Transport.State.ERROR);
		return;
	}
	
	var mt = this;
	try
	{

		var x = this.openConnection("GET");
		x.responseType = "text";
		x.onreadystatechange = function()
		{
			if (x.readyState == 4)
			{
				
				if (x.status == 200 && !x.hasError && !x.hasTimeout && !x.isAborted)
				{
					if ('/' == mt.url.charAt(mt.url.length-1))
						mt.url = mt.url + x.responseText;
					else
						mt.url = mt.url + '/' + x.responseText;

					mt.running = true;
					mt.poll();
					mt.setState(Trap.Transport.State.CONNECTED);
				}
				else
				{
					mt.logger.warn("HTTP transport failed with state ", x.status);
					mt.setState(Trap.Transport.State.ERROR);
					return true;
				}
			}

			return false;
		};
		x.send();
	}
	catch(e)
	{
		this.logger.warn("HTTP transport failed to connect due to ", e);
		if(e.stack)
			this.logger.debug(e.stack);
		this.setState(Trap.Transport.State.ERROR);
		return;
	}
};

Trap.Transports.HTTP.prototype.internalDisconnect = function()
{

	var mt = this;
	
	if (mt._sendQueued)
	{
		setTimeout(function() { mt.internalDisconnect(); }, 100);
		return;
	}
	
	var x = new XMLHttpRequest();
	x.open("POST", this.url, true);

	// The disconnect should succeed whether or not the XHR does.
	// Two ways to call done
	var done = function() {
		mt.running = false;

		if(mt.getState() == Trap.Transport.State.DISCONNECTING)
			mt.setState(Trap.Transport.State.DISCONNECTED);
	};

	// State change -- connection done!
	x.onreadystatechange = function()
	{
		if (x.readyState == 4)
			done();
	};
	
	x.send();

};

Trap.Transports.HTTP.prototype.canConnect = function()
{
	return true;
};

Trap.Transports.HTTP.prototype.poll = function()
{
	
	if (!this.running)
		return;
	
	var x = this.openConnection("GET");
	
	if (this.useBinary)
	try
	{
		x.responseType = "arraybuffer";
	}
	catch(e)
	{
		console.error("Asked to use binary but could not use binary mode transport!");
		this.setState(Trap.Transport.State.ERROR);
		return;
	}
		
	var mt = this;
	
	x.onreadystatechange = function()
	{
		if (x.readyState == 4)
		{
			if (x.isAborted)
			{
				if (x.abortState == 2)
				{
					mt.poll();
					mt._keepalivePredictor.dataReceived();
				}
				else
				{
					mt.setState(Trap.Transport.State.ERROR);
				}
			}
			else
			{
				if (x.status < 300 && x.status >= 200)
				{
					if (x.responseType == "arraybuffer")
					{
						try
						{
							if (x.response)
							{
								var data = new Uint8Array(x.response);
								mt.receive(data);
							}
							
							if (x.status != 0 || x.statusText.length != 0)
								mt.poll();
						}
						catch(e)
						{
							console.log(e);
						}
					}
					else
					{
						var data = x.responseText;
						// Data will be a Unicode string (16-bit chars). notifyData expects bytes though
						// Encode data as UTF-8. This will align the bytes with the ones expected from the server.
						data = data.toUTF8ByteArray();
						mt.receive(data, 0, data.length);
						
						mt.poll();
						mt._keepalivePredictor.dataReceived();
					}
				}
				else if (x.status == 0 || x.status >= 300)
				{
					if (mt.getState() != Trap.Transport.State.DISCONNECTING && mt.getState() != Trap.Transport.State.DISCONNECTED)
						mt.setState(Trap.Transport.State.ERROR);
				}
			}
		}
	};

	x.send();
	mt._keepalivePredictor.dataSent();

	this._longpoll = x;
};


Trap.Transports.HTTP.prototype.openConnection = function(type)
{
	var x = new XMLHttpRequest();
	x.open(type, this.url + "?expires=" + this.expirationDelay , true);
	x.aborted = false;
	x.responseType === 'arraybuffer';
	
	var mt = this;

	function abort(error)
	{
		x.isAborted = true;
		x.abortState = x.readyState;
		
		x.abort();
	}

	var pollTimeout =  null;
	var connTimeoutFun = function() {
		if (x.readyState == 1)
		{
			abort();
			mt.logger.warn("XHR longpoll failed to connect...");
		}
	};
	x.connectionTimer = setTimeout(connTimeoutFun, this.expirationDelay + this.latencyEstimate*3);
	
	var latencyRecorded = false;
	var start = new Date().valueOf();
	
	// Also used to clear the connection timeout, since latency implies connection.
	function recordLatency()
	{
		if (latencyRecorded)
			return;
		
		latencyRecorded = true;
		
		var end = new Date().valueOf();
		var latency = end - start;
		mt.latencyEstimate = (mt.latencyEstimate + latency)/2;
	}
	
	// Handles timeouts for an upload.
	if(x.upload) x.upload.addEventListener("loadstart", function() 
	{
		
		// We can't wait for the connection timeout when we're uploading...
		clearTimeout(x.connectionTimer);
		// Cannot record latency on an upload since the headers will return only after the body is uploaded.
		latencyRecorded = true;
		
		var progressed = false;
		var granularity = 1000;
		var done = false;
		
		// Add progress handlers
		x.upload.addEventListener("progress", function() {
			progressed = true;
		}, true);
		
		x.upload.addEventListener("error", function() {
			x.hasError = true;
		}, true);
		
		x.upload.addEventListener("timeout", function() {
			x.hasTimeout = true;
		}, true);
		
		x.upload.addEventListener("load", function() {
			clearTimeout(pFunTimeout);
			done = true;
			
			// Restart connectionTimeout -- we're waiting for headers now!
			x.connectionTimer = setTimeout(connTimeoutFun, mt.connectionTimeout);
		}, true);
		
		x.upload.addEventListener("loadend", function() {
			if (!done)
			{
				mt.logger.warn("Incomplete upload: loadend without load");
				x.hasError = true;
			}
		}, true);

		var pFun = function() {

			if (!mt.running)
				return;
			
			if (x.readyState == 4)
				return;

			if (!progressed)
			{
				// Timeout has occurred.
				abort();
				return;
			}
			progressed = false;
			setTimeout(pFun, granularity);

		};

		var pFunTimeout = setTimeout(pFun, mt.connectionTimeout);
		
	}, true);
	
	x.addEventListener("loadstart", function() {
		mt.logger.trace("XHR load started...");
	});

	x.addEventListener("readystatechange", function()
	{
		switch(x.readyState)
		{
		
		case 0:
			break;
		case 1:
			break;
		
		case 2:
			mt.logger.debug("XHR switched state to headers received (we have connection to server). Stopping connectionTimeout, starting pollTimeout");
			// We have connected (connTimeout unnecessary)
			clearTimeout(x.connectionTimer);
			recordLatency();
			
			// Just keep track of the polling time.
			pollTimeout = setTimeout(function() {

				// Guard against timeout incorrectly set.
				if (!mt.running)
					return;

				switch(x.readyState)
				{
				case 0:
				case 1:
					// This should be impossible
					abort();
					mt.logger.warn("XHR ended in an inconsistent state...");
					mt.setState(Trap.Transport.State.ERROR);
					return;

				case 2:
					// Headers received but no body. Most likely network failure
					abort();
					mt.logger.debug("Loading failed after headers loaded");
					return;

				case 3:
					// Body in process of being received
					// Do nothing; subsequent code will take care of it.
					break;

				case 4:
					// Should not happen.
					mt.logger.error("HTTP transport in inconsistent state");
					mt.setState(Trap.Transport.State.ERROR);
					return;

				}

				var progressed = false;

				x.onprogress = function() {
					progressed = true;
				};

				var pFun = function() {

					if (!mt.running)
						return;
					
					if (x.readyState == 4)
						return;

					if (!progressed)
					{
						// Timeout has occurred.
						abort();
						return;
					}
					progressed = false;
					setTimeout(pFun, 100);

				};

				setTimeout(pFun, 100);

//			}, this.expirationDelay + this.latencyEstimate * 3); // Add some spare time for expiration delay to kick in/transfer to occur.
				// This should be slightly longer time, as it takes a while for node.js to switch from 2 to 3. Dangit.
			}, 30000); // Add some spare time for expiration delay to kick in/transfer to occur.

			break;

		case 3:
			mt.logger.debug("XHR switched state to Receiving (data incoming from server)");
			break;

		case 4:
			// Prevent timeout function from being called
			clearTimeout(pollTimeout);
			
			// Handle error cases
			if (x.hasError || x.hasTimeout)
			{
				mt.setState(Trap.Transport.State.ERROR);
			}
			break;
		}
	}, true);
	
	var done = false;
	
	x.addEventListener("error", function() {
		x.hasError = true;
	}, true);
	
	x.addEventListener("timeout", function() {
		x.hasTimeout = true;
	}, true);
	
	x.addEventListener("load", function() {
		done = true;
	}, true);
	
	x.addEventListener("loadend", function() {
		if (!done)
		{
			mt.logger.warn("Incomplete download: loadend without load");
			x.hasError = true;
		}
	}, true);
	
	return x;
};

Trap.Transports.HTTP.prototype.flushTransport = function()
{
	this.internalSend(null, false); 
};

Trap.Transports.HTTP.prototype.setState = function()
{
	if (this.getState() == Trap.Transport.State.DISCONNECTED || this.getState() == Trap.Transport.State.ERROR)
		this.running = false;

	Trap.AbstractTransport.prototype.setState.apply(this, arguments);
};
t.Trap=Trap;})(self);
