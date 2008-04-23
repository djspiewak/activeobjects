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

import java.io.InputStream;

import net.java.ao.Generator;
import net.java.ao.OneToMany;
import net.java.ao.RawEntity;
import net.java.ao.Searchable;
import net.java.ao.schema.NotNull;
import net.java.ao.schema.PrimaryKey;

/**
 * @author Daniel Spiewak
 */
public interface Company extends RawEntity<Long> {
	
	@PrimaryKey
	@NotNull
	@Generator(TimestampGenerator.class)
	public long getCompanyID();
	
	@Searchable
	public String getName();
	@Searchable
	public void setName(String name);
	
	public boolean isCool();
	public void setCool(boolean cool);
	
	@Generator(MotivationGenerator.class)
	public String getMotivation();
	
	@Generator(MotivationGenerator.class)
	public void setMotivation(String motivation);
	
	public CompanyAddressInfo getAddressInfo();
	public void setAddressInfo(CompanyAddressInfo info);
	
	public InputStream getImage();
	public void setImage(InputStream image);
	
	@OneToMany
	public Person[] getPeople();
}
