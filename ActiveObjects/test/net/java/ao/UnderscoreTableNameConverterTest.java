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
import net.java.ao.schema.UnderscoreTableNameConverter;

import org.junit.Test;

import test.schema.Company;
import test.schema.CompanyAddressInfo;
import test.schema.Person;
import test.schema.PersonLegalDefence;
import test.schema.PersonSuit;

/**
 * @author Daniel Spiewak
 */
public class UnderscoreTableNameConverterTest {

	@Test
	public void testAddClassMappingLowercase() {
		UnderscoreTableNameConverter converter = new UnderscoreTableNameConverter(false);
		
		converter.addClassMapping(Person.class, "rowdy_ones");
		assertEquals("rowdy_ones", converter.getName(Person.class));
		
		converter.addClassMapping(PersonSuit.class, "unfair_procedings");
		assertEquals("unfair_procedings", converter.getName(PersonSuit.class));
	}

	@Test
	public void testAddClassMappingUppercase() {
		UnderscoreTableNameConverter converter = new UnderscoreTableNameConverter(true);
		
		converter.addClassMapping(Person.class, "rowdy_ones");
		assertEquals("rowdy_ones", converter.getName(Person.class));
		
		converter.addClassMapping(PersonSuit.class, "unfair_procedings");
		assertEquals("unfair_procedings", converter.getName(PersonSuit.class));
	}

	@Test
	public void testGetNameLowercase() {
		UnderscoreTableNameConverter converter = new UnderscoreTableNameConverter(false);
		
		assertEquals("person", converter.getName(Person.class));
		assertEquals("company", converter.getName(Company.class));
		assertEquals("person_suit", converter.getName(PersonSuit.class));
		assertEquals("personDefence", converter.getName(PersonLegalDefence.class));
		assertEquals("company_address_info", converter.getName(CompanyAddressInfo.class));
	}

	@Test
	public void testGetNameUppercase() {
		UnderscoreTableNameConverter converter = new UnderscoreTableNameConverter(true);
		
		assertEquals("PERSON", converter.getName(Person.class));
		assertEquals("COMPANY", converter.getName(Company.class));
		assertEquals("PERSON_SUIT", converter.getName(PersonSuit.class));
		assertEquals("personDefence", converter.getName(PersonLegalDefence.class));
		assertEquals("COMPANY_ADDRESS_INFO", converter.getName(CompanyAddressInfo.class));
	}
}
