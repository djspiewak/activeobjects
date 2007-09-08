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

import net.java.ao.schema.CamelCaseFieldNameConverter;

import org.junit.Test;

import test.schema.Company;
import test.schema.CompanyAddressInfo;
import test.schema.Person;
import test.schema.PersonLegalDefence;
import test.schema.PersonSuit;

/**
 * @author Daniel Spiewak
 */
public class CamelCaseFieldNameConverterTest {

	@Test
	public void testGetName() throws SecurityException, NoSuchMethodException {
		CamelCaseFieldNameConverter converter = new CamelCaseFieldNameConverter();
		
		assertEquals("firstName", converter.getName(Person.class, Person.class.getMethod("getFirstName")));
		assertEquals("firstName", converter.getName(Person.class, Person.class.getMethod("setFirstName", String.class)));
		
		assertEquals("url", converter.getName(Person.class, Person.class.getMethod("getURL")));
		assertEquals("url", converter.getName(Person.class, Person.class.getMethod("setURL", URL.class)));
		
		assertEquals("companyID", converter.getName(Person.class, Person.class.getMethod("getCompany")));
		assertEquals("companyID", converter.getName(Person.class, Person.class.getMethod("setCompany", Company.class)));
		
		assertNull(converter.getName(Person.class, Person.class.getMethod("getPersonLegalDefences")));

		assertEquals("name", converter.getName(Company.class, Company.class.getMethod("getName")));
		assertEquals("name", converter.getName(Company.class, Company.class.getMethod("setName", String.class)));

		assertNull(converter.getName(Company.class, Company.class.getMethod("getPeople")));

		assertEquals("personLegalDefenceID", 
				converter.getName(PersonSuit.class, PersonSuit.class.getMethod("getPersonLegalDefence")));
		assertEquals("personLegalDefenceID", 
				converter.getName(PersonSuit.class, PersonSuit.class.getMethod("setPersonLegalDefence", PersonLegalDefence.class)));

		assertEquals("addressLine1", converter.getName(CompanyAddressInfo.class, CompanyAddressInfo.class.getMethod("getAddressLine1")));
		assertEquals("addressLine1", converter.getName(CompanyAddressInfo.class, CompanyAddressInfo.class.getMethod("setAddressLine1", 
				String.class)));
	}

	@Test
	public void testGetIDField() {
		CamelCaseFieldNameConverter converter = new CamelCaseFieldNameConverter();
		
		assertEquals("id", converter.getIDField(Person.class));
		assertEquals("id", converter.getIDField(Company.class));
		assertEquals("id", converter.getIDField(PersonLegalDefence.class));
	}
}
