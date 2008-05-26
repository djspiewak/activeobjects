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
package net.java.ao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * @author Daniel Spiewak
 */
public class DBParamTest {

	@Test
	public void testDBParam() {
		Object value = new Object();
		DBParam param = new DBParam("field", value);
		
		assertEquals("field", param.getField());
		assertEquals(value, param.getValue());
	}

	@Test(expected=NullPointerException.class)
	public void testDBParamWithNull() {
		new DBParam(null, new Object());
	}

	@Test
	public void testSetField() {
		Object value = new Object();
		DBParam param = new DBParam("field", value);
		
		assertEquals("field", param.getField());
		assertEquals(value, param.getValue());
		
		param.setField("newField");
		
		assertEquals("newField", param.getField());
		assertEquals(value, param.getValue());
	}

	@Test(expected=NullPointerException.class)
	public void testSetFieldWithNull() {
		new DBParam("field", new Object()).setField(null);
	}

	@Test
	public void testSetValue() {
		Object value = new Object();
		DBParam param = new DBParam("field", value);
		
		assertEquals("field", param.getField());
		assertEquals(value, param.getValue());
		
		param.setValue("test value");
		
		assertEquals("field", param.getField());
		assertEquals("test value", param.getValue());
	}

	@Test
	public void testEquals() {
		Object value = new Object();
		
		assertTrue(new DBParam("field", value).equals(new DBParam("field", value)));
		assertTrue(new DBParam("field", "testing").equals(new DBParam("field", "test" + "ing")));
		
		assertFalse(new DBParam("field", value).equals(new DBParam("field2", value)));
		assertFalse(new DBParam("field", value).equals(new DBParam("field", new Object())));
		assertFalse(new DBParam("field", value).equals(new DBParam("field2", new Object())));
	}
}
