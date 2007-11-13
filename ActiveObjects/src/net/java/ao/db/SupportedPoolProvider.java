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
package net.java.ao.db;

import net.java.ao.PoolProvider;

/**
 * <p>Contains a list of all internally supported connection pool providers.
 * This list is used in the auto-magical pool provider selection based on the
 * classpath.</p>
 * 
 * <p>This enum is designed primarily for INTERNAL use within AO.  While it
 * is perfectly acceptible to utilize this enum externally, the API may change
 * unnexpectedly, undocumented results may occur, you know the drill.</p>
 * 
 * @author Daniel Spiewak
 * @see net.java.ao.PoolProvider
 */
public enum SupportedPoolProvider {
	DBPOOL(DBPoolProvider.class),
	C3P0(C3P0PoolProvider.class),
	PROXOOL(ProxoolPoolProvider.class),
	DBCP(DBCPPoolProvider.class);
	
	private final Class<? extends PoolProvider> provider;
	
	private SupportedPoolProvider(Class<? extends PoolProvider> provider) {
		this.provider = provider;
	}
	
	public Class<? extends PoolProvider> getProvider() {
		return provider;
	}
}
