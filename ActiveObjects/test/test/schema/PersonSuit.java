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

import net.java.ao.Entity;
import net.java.ao.schema.Default;

/**
 * @author Daniel Spiewak
 */
public interface PersonSuit extends Entity {
	
	public Person getPerson();
	public void setPerson(Person person);
	
	public PersonLegalDefence getPersonLegalDefence();
	public void setPersonLegalDefence(PersonLegalDefence defence);
	
	@Default("false")
	public boolean isDeleted();
	@Default("false")
	public void setDeleted(boolean deleted);
}
