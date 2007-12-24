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
package net.java.ao.cache;

import net.java.ao.Common;
import net.java.ao.RawEntity;
import net.spy.memcached.MemcachedClient;

/**
 * @author Daniel Spiewak
 */
public class MemcachedCacheLayer extends RAMCacheLayer {
	private final MemcachedClient client;
	private final String prefix;
	
	MemcachedCacheLayer(MemcachedClient client, String prefix) {
		this.client = client;
		this.prefix = prefix;
	}
	
	@Override
	public void clearDirty() {
		super.clearDirty();
		super.clear();
	}

	public boolean contains(String field) {
		if (dirtyContains(field) && super.contains(field)) {
			return true;
		}
		
		return client.findKeys(prefix + field.toLowerCase()).contains(prefix + field.toLowerCase());
	}

	public Object get(String field) {
		if (dirtyContains(field)) {
			return super.get(field);
		}
		
		return client.get(prefix + field.toLowerCase());
	}

	public void put(String field, Object value) {
		if (dirtyContains(field)) {
			super.put(field, value);
		} else {
			if (value instanceof RawEntity) {
				value = Common.getPrimaryKeyValue((RawEntity<Object>) value);
			}
			
			client.add(prefix + field.toLowerCase(), Integer.MAX_VALUE, value);
		}
	}

	public void remove(String field) {
		super.remove(field);
		
		if (!super.dirtyContains(field)) {
			client.delete(prefix + field.toLowerCase());
		}
	}
}
