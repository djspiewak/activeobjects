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
import net.java.ao.schema.CamelCaseTableNameConverter;

import org.junit.Test;

import test.schema.Company;
import test.schema.CompanyAddressInfo;
import test.schema.Person;
import test.schema.PersonLegalDefence;
import test.schema.PersonSuit;

/**
 * @author Daniel Spiewak
 */
public class CamelCaseTableNameConverterTest {

	@Test
	public void testAddClassMapping() {
		CamelCaseTableNameConverter converter = new CamelCaseTableNameConverter();
		
		converter.addClassMapping(Person.class, "rowdy_ones");
		assertEquals("rowdy_ones", converter.getName(Person.class));
		
		converter.addClassMapping(PersonSuit.class, "unfair_procedings");
		assertEquals("unfair_procedings", converter.getName(PersonSuit.class));
	}

	@Test
	public void testGetName() {
		CamelCaseTableNameConverter converter = new CamelCaseTableNameConverter();
		
		assertEquals("person", converter.getName(Person.class));
		assertEquals("company", converter.getName(Company.class));
		assertEquals("personSuit", converter.getName(PersonSuit.class));
		assertEquals("personDefence", converter.getName(PersonLegalDefence.class));
		assertEquals("companyAddressInfo", converter.getName(CompanyAddressInfo.class));
	}
}
