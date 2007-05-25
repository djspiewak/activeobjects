/*
 * Copyright 2007, Daniel Spiewak
 * All rights reserved
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 *   * Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *   * Redistributions in binary form must reproduce the above
 *     copyright notice, this list of conditions and the following
 *     disclaimer in the documentation and/or other materials provided
 *     with the distribution.
 *   * Neither the name of the ActiveObjects project nor the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.java.ao.db;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import net.java.ao.DatabaseProvider;

/**
 * @author Daniel Spiewak
 */
public enum SupportedDBProvider {
	MYSQL("jdbc:mysql", MySQLDatabaseProvider.class),
	DERBY("jdbc:derby", EmbeddedDerbyDatabaseProvider.class);
	
	private String prefix;
	private Class<? extends DatabaseProvider> type;
	
	private SupportedDBProvider(String prefix, Class<? extends DatabaseProvider> type) {
		this.prefix = prefix;
		this.type = type;
	}
	
	public String getPrefix() {
		return prefix;
	}
	
	public Class<? extends DatabaseProvider> getType() {
		return type;
	}
	
	public DatabaseProvider createInstance(String uri, String username, String password) {
		DatabaseProvider back = null;
		
		try {
			Constructor<? extends DatabaseProvider> constructor = type.getDeclaredConstructor(String.class, String.class, String.class);
			constructor.setAccessible(true);
			
			back = constructor.newInstance(uri, username, password);
		} catch (SecurityException e) {
		} catch (NoSuchMethodException e) {
		} catch (IllegalArgumentException e) {
		} catch (InstantiationException e) {
		} catch (IllegalAccessException e) {
		} catch (InvocationTargetException e) {
		}
		
		return back;
	}
	
	public static SupportedDBProvider getProviderForURI(String uri) {
		for (SupportedDBProvider provider : values()) {
			if (uri.trim().startsWith(provider.prefix.trim())) {
				return provider;
			}
		}
		
		return null;
	}
}
