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
import net.java.ao.schema.PluralizedNameConverter;

import org.junit.Before;
import org.junit.Test;

import test.schema.Commentable;
import test.schema.Company;
import test.schema.Distribution;
import test.schema.OnlineDistribution;
import test.schema.Pen;
import test.schema.Person;
import test.schema.Photo;
import test.schema.Post;
import test.schema.PrintDistribution;

/**
 * @author Daniel Spiewak
 */
public class DefaultPolymorphicTypeMapperTest {
	private DefaultPolymorphicTypeMapper mapper;
	
	@Before
	public void setUp() {
		mapper = new DefaultPolymorphicTypeMapper(Person.class, Company.class, Post.class, Photo.class, Pen.class);
		mapper.resolveMappings(new PluralizedNameConverter());
	}

	@Test
	public void testConvert() {
		assertEquals("person", mapper.convert(Person.class));
		assertEquals("company", mapper.convert(Company.class));
		assertEquals("post", mapper.convert(Post.class));
		assertEquals("photo", mapper.convert(Photo.class));
		assertEquals("pen", mapper.convert(Pen.class));
		
		assertEquals("test.schema.PrintDistribution", mapper.convert(PrintDistribution.class));
	}

	@Test
	public void testInvert() {
		assertEquals(Person.class, mapper.invert(Entity.class, "person"));
		assertEquals(Company.class, mapper.invert(Company.class, "company"));
		assertEquals(Post.class, mapper.invert(Commentable.class, "post"));
		assertEquals(Photo.class, mapper.invert(Commentable.class, "photo"));
		assertEquals(Pen.class, mapper.invert(Entity.class, "pen"));

		assertEquals(PrintDistribution.class, mapper.invert(Distribution.class, "test.schema.PrintDistribution"));
		assertEquals(OnlineDistribution.class, mapper.invert(Distribution.class, "test.schema.OnlineDistribution"));
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testInvalid() {
		mapper.invert(Entity.class, "sldkfjsdflkjsdflkjdsflkdsjf");
	}
}
