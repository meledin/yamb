package us.yamb.mb;

public interface Observable<CallbackIface, MessageBus>
{

	/**
	 * Sets an object as a callback object. The callbacks that will be set
	 * correspond to the interfaces that the object implements.
	 * 
	 * @param callback
	 *            The object to set the callbacks for.
	 */
	public MessageBus setCallback(CallbackIface callback);

	/**
	 * Sets a callback using reflection. Use this to set custom names for the
	 * callbacks. The callbacks MUST use the same method signature as the one
	 * defined in the interface specified in callbackType.
	 * 
	 * @param callbackType
	 *            The callback to register
	 * @param object
	 *            The object to call for the callback
	 * @param function
	 *            The method name to register for the callback
	 * @throws NoSuchMethodException
	 *             If the method could not be called.
	 */
	public MessageBus setCallback(Class<? extends CallbackIface> callbackType, Object object, String function) throws NoSuchMethodException;

	/**
	 * Removes a callback.
	 * @param callbackType The callback type to remove.
	 */
	public MessageBus unsetCallback(Class<? extends CallbackIface> callbackType);
}
