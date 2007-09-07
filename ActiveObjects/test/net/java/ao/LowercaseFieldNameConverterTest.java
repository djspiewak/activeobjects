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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.net.URL;

import net.java.ao.schema.LowercaseFieldNameConverter;

import org.junit.Test;

/**
 * @author Daniel Spiewak
 */
public class LowercaseFieldNameConverterTest {

	@Test
	public void testGetName() throws SecurityException, NoSuchMethodException {
		LowercaseFieldNameConverter converter = new LowercaseFieldNameConverter();
		
		assertEquals("first_name", converter.getName(Person.class, Person.class.getMethod("getFirstName")));
		assertEquals("first_name", converter.getName(Person.class, Person.class.getMethod("setFirstName", String.class)));
		
		assertEquals("url", converter.getName(Person.class, Person.class.getMethod("getURL")));
		assertEquals("url", converter.getName(Person.class, Person.class.getMethod("setURL", URL.class)));
		
		assertEquals("company_id", converter.getName(Person.class, Person.class.getMethod("getCompany")));
		assertEquals("company_id", converter.getName(Person.class, Person.class.getMethod("setCompany", Company.class)));
		
		assertNull(converter.getName(Person.class, Person.class.getMethod("getPersonLegalDefences")));

		assertEquals("name", converter.getName(Company.class, Company.class.getMethod("getName")));
		assertEquals("name", converter.getName(Company.class, Company.class.getMethod("setName", String.class)));

		assertNull(converter.getName(Company.class, Company.class.getMethod("getPeople")));

		assertEquals("person_legal_defence_id", 
				converter.getName(PersonSuit.class, PersonSuit.class.getMethod("getPersonLegalDefence")));
		assertEquals("person_legal_defence_id", 
				converter.getName(PersonSuit.class, PersonSuit.class.getMethod("setPersonLegalDefence", PersonLegalDefence.class)));

		assertEquals("address_line_1", converter.getName(CompanyAddressInfo.class, CompanyAddressInfo.class.getMethod("getAddressLine1")));
		assertEquals("address_line_1", converter.getName(CompanyAddressInfo.class, CompanyAddressInfo.class.getMethod("setAddressLine1", 
				String.class)));
	}

	@Test
	public void testGetIDField() {
		LowercaseFieldNameConverter converter = new LowercaseFieldNameConverter();
		
		assertEquals("id", converter.getIDField(Person.class));
		assertEquals("id", converter.getIDField(Company.class));
		assertEquals("id", converter.getIDField(PersonLegalDefence.class));
	}
}
