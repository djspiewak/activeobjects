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
package test.schema;

import net.java.ao.EntityManager;
import net.java.ao.ValueGenerator;

/**
 * @author Daniel Spiewak
 */
public class TimestampGenerator implements ValueGenerator<Long> {

	public Long generateValue(EntityManager manager) {
		try {
			Thread.sleep(5);	// just enough to prevent clashes
		} catch (InterruptedException e) {
		}
		
		return System.currentTimeMillis();
	}
}
