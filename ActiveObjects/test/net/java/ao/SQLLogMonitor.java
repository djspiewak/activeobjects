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

import java.util.logging.Handler;
import java.util.logging.LogRecord;

/**
 * @author Daniel Spiewak
 */
public class SQLLogMonitor extends Handler {
	private static SQLLogMonitor instance;
	
	private boolean executedSQL;
	
	private SQLLogMonitor() {
		executedSQL = false;
	}
	
	public void markWatchSQL() {
		executedSQL = false;
	}

	@Override
	public void close() throws SecurityException {
	}

	@Override
	public void flush() {
	}

	@Override
	public void publish(LogRecord record) {
		executedSQL = true;
	}
	
	public boolean isExecutedSQL() {
		return executedSQL;
	}
	
	public static SQLLogMonitor getInstance() {
		if (instance == null) {
			instance = new SQLLogMonitor();
		}
		
		return instance;
	}
}
