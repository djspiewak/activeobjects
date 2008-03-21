/*
 * Copyright 2008 Daniel Spiewak
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

import net.java.ao.RawEntity;

/**
 * @author Daniel Spiewak
 */
public interface RelationsCache {

	public void flush();

	public void put(RawEntity<?> from, RawEntity<?>[] through, Class<? extends RawEntity<?>> throughType, RawEntity<?>[] to, Class<? extends RawEntity<?>> toType, String[] fields);

	public <T extends RawEntity<?>> T[] get(RawEntity<?> from, Class<T> toType, Class<? extends RawEntity<?>> throughType, String[] fields);

	public void remove(Class<? extends RawEntity<?>>... types);

	public void remove(RawEntity<?> entity, String[] fields);

}