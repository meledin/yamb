package us.yamb.amb.spi;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import us.yamb.mb.Observable;

public abstract class ObservableBase<I, T> implements Observable<I, T>
{

	public T setCallback(Class<? extends I> callbackType, final Object object, String methodName) throws NoSuchMethodException
	{

		Method[] methods = callbackType.getDeclaredMethods();

		if (methods.length != 1)
			throw new NoSuchMethodException("Invalid callback type; the callback interface should only implement one method");

		final Method cbMethod = object.getClass().getMethod(methodName, methods[0].getParameterTypes());

		Object proxy = Proxy.newProxyInstance(object.getClass().getClassLoader(), new Class<?>[] { callbackType }, new InvocationHandler()
		{

			public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
			{

				if (!cbMethod.equals(method))
					throw new NoSuchMethodError("An invalid method was called on this proxy");

				return cbMethod.invoke(object, args);
			}

		});

		return setCallback(callbackType.cast(proxy));
	}

	@SuppressWarnings("unchecked")
	public T setCallback(I callback)
	{

		Class<?>[] cs = callback.getClass().getInterfaces();

		for (Class<?> c : cs)
		{

			String name = c.getSimpleName();

			try
			{
				Class<?> cc = this.getClass();

				do
				{

					try
					{

						cc.getDeclaredField(name.toLowerCase()).set(this, callback);
						break;
					}
					catch (NoSuchFieldException e)
					{
					}

					cc = cc.getSuperclass();
				}
				while (cc != null);
			}
			catch (IllegalArgumentException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			catch (IllegalAccessException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			catch (SecurityException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return (T) this;
	}

	@SuppressWarnings("unchecked")
	public T unsetCallback(Class<? extends I> callbackType)
	{

		try
		{
			this.getClass().getDeclaredField(callbackType.getSimpleName().toLowerCase()).set(this, null);
		}
		catch (IllegalArgumentException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (IllegalAccessException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (NoSuchFieldException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (SecurityException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return (T) this;
	}

}
