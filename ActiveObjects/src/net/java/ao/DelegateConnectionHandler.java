/*
 * Copyright 2007 Daniel Spiewak
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at
 * 
 *	    http://www.apache.org/licenses/LICENSE-2.0 
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.java.ao;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;

/**
 * @author Daniel Spiewak
 */
class DelegateConnectionHandler implements InvocationHandler {
	private Connection delegate;
	private boolean closeable;
	private boolean closed;
	
	private DelegateConnectionHandler(Connection delegate) {
		this.delegate = delegate;
		closeable = true;
	}

	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		if (method.getName().equals("setCloseable")) {
			if (method.getParameterTypes().length == 1 && method.getParameterTypes()[0].equals(boolean.class)) {
				this.closeable = (Boolean) args[0];
				
				return Void.TYPE;
			}
		} else if (method.getName().equals("isCloseable")) {
			return closeable;
		} else if (method.getName().equals("close") && method.getParameterTypes().length == 0) {
			if (closeable) {
				delegate.close();
				closed = true;
			}
			
			return Void.TYPE;
		} else if (method.getName().equals("isClosed") && method.getReturnType().equals(boolean.class)) {
			return closed;
		}
		
		Class<Connection> clazz = (Class<Connection>) delegate.getClass();
		Method method2 = clazz.getMethod(method.getName(), method.getParameterTypes());
		method2.setAccessible(true);
		
		return method2.invoke(delegate, args);
	}

	public static DelegateConnection newInstance(Connection delegate) {
		return (DelegateConnection) Proxy.newProxyInstance(DelegateConnectionHandler.class.getClassLoader(), 
				new Class[] {DelegateConnection.class}, new DelegateConnectionHandler(delegate));
	}
}
