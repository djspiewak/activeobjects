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

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

/**
 * @author Daniel Spiewak
 */
public abstract class RSCachingStrategy {
	public static final RSCachingStrategy AGGRESSIVE = new AggressiveStrategy();
	public static final RSCachingStrategy ITERATIVE = new IterativeStrategy();
	public static final RSCachingStrategy NULL = new NullStrategy();
	
	protected RSCachingStrategy() {
	}
	
	public abstract void cache(ResultSet res, EntityProxy<?, ?> proxy) throws SQLException;
	
	private static class AggressiveStrategy extends RSCachingStrategy {
		@Override
		public void cache(ResultSet res, EntityProxy<?, ?> proxy) throws SQLException {
			ResultSetMetaData md = res.getMetaData();
			for (int i = 0; i < md.getColumnCount(); i++) {
				proxy.addToCache(md.getColumnName(i + 1), res.getObject(i + 1));
			}
		}
	}
	
	private static class IterativeStrategy extends RSCachingStrategy {
		@Override
		public void cache(ResultSet res, EntityProxy<?, ?> proxy) {
			throw new UnsupportedOperationException();
		}
	}
	
	private static class NullStrategy extends RSCachingStrategy {
		@Override
		public void cache(ResultSet res, EntityProxy<?, ?> proxy) {
		}
	}
}
