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
package test.schema;

import net.java.ao.schema.NotNull;
import net.java.ao.schema.Unique;

/**
 * @author daniel
 */
public interface DataUser {
	@NotNull
	public byte[] getPasswordHash();
	public void setPasswordHash(byte[] passwordHash);
	
	@NotNull
	@Unique
	public String getUsername();
	public void setUsername(String username);
	
	public String getRoleString();
	public void setRoleString(String roleString);
}
