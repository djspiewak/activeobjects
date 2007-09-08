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

import java.net.URL;

import net.java.ao.Accessor;
import net.java.ao.Entity;
import net.java.ao.Implementation;
import net.java.ao.ManyToMany;
import net.java.ao.Mutator;
import net.java.ao.OneToMany;

/**
 * @author Daniel Spiewak
 */
@Implementation(PersonImpl.class)
public interface Person extends Entity {
	
	public String getFirstName();
	public void setFirstName(String firstName);
	
	public String getLastName();
	public void setLastName(String lastName);
	
	@Accessor("url")
	public URL getURL();
	@Mutator("url")
	public void setURL(URL url);
	
	public Company getCompany();
	public void setCompany(Company company);
	
	@OneToMany
	public Pen[] getPens();
	
	@ManyToMany(PersonSuit.class)
	public PersonLegalDefence[] getPersonLegalDefences();
}
