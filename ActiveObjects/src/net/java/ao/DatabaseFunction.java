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

/**
 * An enum containing database agnostic representations of common
 * functions.  These functions will be executed in the relevant manner
 * when executed on different databases.  For example, the
 * <code>CURRENT_TIMESTAMP</code> function will execute
 * <code>CURRENT_TIMESTAMP</code> on MySQL, <code>now()</code>
 * on PostgreSQL and <code>GetDate()</code> on SQL Server.
 * 
 * @author Daniel Spiewak
 */
public enum DatabaseFunction {
	
	/**
	 * Returns the current date without time information (e.g. 
	 * "2007-09-11").  For databases which do not support such a
	 * function, CURRENT_DATE will be equivalent to
	 * CURRENT_TIMESTAMP.
	 */
	CURRENT_DATE,
	
	/**
	 * Returns the current date and time according to the database.
	 * (e.g. "2007-09-11 14:27:00")
	 */
	CURRENT_TIMESTAMP;
	
	/**
	 * <p>Performs a linear search through all database functions available
	 * within this enum and returns the value which matches the
	 * specified String name.  Matching is performed based upon the
	 * field name of the enum value.  (e.g. passing a value of
	 * "CURRENT_TIMESTAMP" will return the {@link #CURRENT_TIMESTAMP}
	 * value0).</p>
	 * 
	 * @param value	The String name of the function to look for.
	 * @return The enum value which corresponds to the specified function
	 * 	name, or <code>null</code>.
	 */
	public static DatabaseFunction get(String value) {
		for (DatabaseFunction func : values()) {
			if (func.name().equalsIgnoreCase(value)) {
				return func;
			}
		}
		
		return null;
	}
}
