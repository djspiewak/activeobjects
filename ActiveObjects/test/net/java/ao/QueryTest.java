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

import java.sql.SQLException;

import net.java.ao.db.EmbeddedDerbyDatabaseProvider;
import net.java.ao.db.HSQLDatabaseProvider;
import net.java.ao.db.JTDSSQLServerDatabaseProvider;
import net.java.ao.db.MySQLDatabaseProvider;
import net.java.ao.db.OracleDatabaseProvider;
import net.java.ao.db.PostgreSQLDatabaseProvider;
import net.java.ao.schema.CamelCaseFieldNameConverter;
import net.java.ao.schema.CamelCaseTableNameConverter;
import net.java.ao.schema.FieldNameConverter;
import net.java.ao.schema.TableNameConverter;

import org.junit.Before;
import org.junit.Test;

import test.schema.Comment;
import test.schema.Company;
import test.schema.Person;

/**
 * @author Daniel Spiewak
 */
public class QueryTest extends DataTest {
	
	public QueryTest(TableNameConverter tableConverter, FieldNameConverter fieldConverter) throws SQLException {
		super(tableConverter, fieldConverter);
	}

	private FieldNameConverter converter;
	
	@Before
	public void instanceSetUp() {
		converter = new CamelCaseFieldNameConverter();
	}

	@Test
	public void testToSQL() {
		String personTableName = manager.getTableNameConverter().getName(Person.class);
		personTableName = manager.getProvider().processID(personTableName);

		String companyTableName = manager.getTableNameConverter().getName(Company.class);
		companyTableName = manager.getProvider().processID(companyTableName);
		
		Query query1 = Query.select();
		Query query2 = Query.select("id,firstName,lastName");
		Query query3 = Query.select().where("name IS NULL AND age = 3");
		Query query4 = Query.select().order("name DESC");
		Query query5 = Query.select().where("name IS NULL AND age = 3").limit(10);
		Query query6 = Query.select().where("name IS NULL AND age = 3").limit(10).offset(4);
		Query query7 = Query.select().where("name IS NULL AND age = 3").limit(4).group("age");
		Query query8 = Query.select().join(Company.class).where("name IS NULL AND age = 3").group("url");
		
		TableNameConverter converter = new CamelCaseTableNameConverter();
		DatabaseProvider provider = new EmbeddedDerbyDatabaseProvider("", "", "");
		
		assertEquals("SELECT id FROM " + personTableName, query1.toSQL(Person.class, provider, converter, getFieldNameConverter(), false));
		assertEquals("SELECT id,firstName,lastName FROM " + personTableName, query2.toSQL(Person.class, provider, converter, getFieldNameConverter(), false));
		assertEquals("SELECT id FROM " + personTableName + " WHERE name IS NULL AND age = 3", query3.toSQL(Person.class, provider, converter, getFieldNameConverter(), false));
		assertEquals("SELECT id FROM " + personTableName + " ORDER BY name DESC", query4.toSQL(Person.class, provider, converter, getFieldNameConverter(), false));
		assertEquals("SELECT id FROM " + personTableName + " WHERE name IS NULL AND age = 3", query5.toSQL(Person.class, provider, converter, getFieldNameConverter(), false));
		assertEquals("SELECT id FROM " + personTableName + " WHERE name IS NULL AND age = 3", query6.toSQL(Person.class, provider, converter, getFieldNameConverter(), false));
		assertEquals("SELECT id FROM " + personTableName + " WHERE name IS NULL AND age = 3 GROUP BY age", 
				query7.toSQL(Person.class, provider, converter, getFieldNameConverter(), false));
		assertEquals("SELECT id FROM " + personTableName + " JOIN " + companyTableName + " WHERE name IS NULL AND age = 3 GROUP BY url", 
				query8.toSQL(Person.class, provider, converter, getFieldNameConverter(), false));
		
		assertEquals("SELECT COUNT(*) FROM " + personTableName, query1.toSQL(Person.class, provider, converter, getFieldNameConverter(), true));
		assertEquals("SELECT COUNT(*) FROM " + personTableName, query2.toSQL(Person.class, provider, converter, getFieldNameConverter(), true));
		assertEquals("SELECT COUNT(*) FROM " + personTableName + " WHERE name IS NULL AND age = 3", query3.toSQL(Person.class, provider, converter, getFieldNameConverter(), true));
		assertEquals("SELECT COUNT(*) FROM " + personTableName + " ORDER BY name DESC", query4.toSQL(Person.class, provider, converter, getFieldNameConverter(), true));
		assertEquals("SELECT COUNT(*) FROM " + personTableName + " WHERE name IS NULL AND age = 3", query5.toSQL(Person.class, provider, converter, getFieldNameConverter(), true));
		assertEquals("SELECT COUNT(*) FROM " + personTableName + " WHERE name IS NULL AND age = 3", query6.toSQL(Person.class, provider, converter, getFieldNameConverter(), true));
		assertEquals("SELECT COUNT(*) FROM " + personTableName + " WHERE name IS NULL AND age = 3 GROUP BY age", 
				query7.toSQL(Person.class, provider, converter, getFieldNameConverter(), true));
		assertEquals("SELECT COUNT(*) FROM " + personTableName + " JOIN " + companyTableName + " WHERE name IS NULL AND age = 3 GROUP BY url", 
				query8.toSQL(Person.class, provider, converter, getFieldNameConverter(), true));
		
		provider = new HSQLDatabaseProvider("", "", "");
		
		assertEquals("SELECT id FROM " + personTableName, query1.toSQL(Person.class, provider, converter, getFieldNameConverter(), false));
		assertEquals("SELECT id,firstName,lastName FROM " + personTableName, query2.toSQL(Person.class, provider, converter, getFieldNameConverter(), false));
		assertEquals("SELECT id FROM " + personTableName + " WHERE name IS NULL AND age = 3", query3.toSQL(Person.class, provider, converter, getFieldNameConverter(), false));
		assertEquals("SELECT id FROM " + personTableName + " ORDER BY name DESC", query4.toSQL(Person.class, provider, converter, getFieldNameConverter(), false));
		assertEquals("SELECT LIMIT 0 10 id FROM " + personTableName + " WHERE name IS NULL AND age = 3", 
				query5.toSQL(Person.class, provider, converter, getFieldNameConverter(), false));
		assertEquals("SELECT LIMIT 4 10 id FROM " + personTableName + " WHERE name IS NULL AND age = 3", 
				query6.toSQL(Person.class, provider, converter, getFieldNameConverter(), false));
		assertEquals("SELECT LIMIT 0 4 id FROM " + personTableName + " WHERE name IS NULL AND age = 3 GROUP BY age", 
				query7.toSQL(Person.class, provider, converter, getFieldNameConverter(), false));
		assertEquals("SELECT id FROM person JOIN " + companyTableName + " WHERE name IS NULL AND age = 3 GROUP BY url", 
				query8.toSQL(Person.class, provider, converter, getFieldNameConverter(), false));
		
		assertEquals("SELECT COUNT(*) FROM " + personTableName, query1.toSQL(Person.class, provider, converter, getFieldNameConverter(), true));
		assertEquals("SELECT COUNT(*) FROM " + personTableName, query2.toSQL(Person.class, provider, converter, getFieldNameConverter(), true));
		assertEquals("SELECT COUNT(*) FROM " + personTableName + " WHERE name IS NULL AND age = 3", query3.toSQL(Person.class, provider, converter, getFieldNameConverter(), true));
		assertEquals("SELECT COUNT(*) FROM " + personTableName + " ORDER BY name DESC", query4.toSQL(Person.class, provider, converter, getFieldNameConverter(), true));
		assertEquals("SELECT LIMIT 0 10 COUNT(*) FROM " + personTableName + " WHERE name IS NULL AND age = 3", 
				query5.toSQL(Person.class, provider, converter, getFieldNameConverter(), true));
		assertEquals("SELECT LIMIT 4 10 COUNT(*) FROM " + personTableName + " WHERE name IS NULL AND age = 3", 
				query6.toSQL(Person.class, provider, converter, getFieldNameConverter(), true));
		assertEquals("SELECT LIMIT 0 4 COUNT(*) FROM " + personTableName + " WHERE name IS NULL AND age = 3 GROUP BY age", 
				query7.toSQL(Person.class, provider, converter, getFieldNameConverter(), true));
		assertEquals("SELECT COUNT(*) FROM " + personTableName + " JOIN " + companyTableName + " WHERE name IS NULL AND age = 3 GROUP BY url", 
				query8.toSQL(Person.class, provider, converter, getFieldNameConverter(), true));
		
		provider = new JTDSSQLServerDatabaseProvider("", "", "");
		
		assertEquals("SELECT id FROM " + personTableName, query1.toSQL(Person.class, provider, converter, getFieldNameConverter(), false));
		assertEquals("SELECT id,firstName,lastName FROM " + personTableName, query2.toSQL(Person.class, provider, converter, getFieldNameConverter(), false));
		assertEquals("SELECT id FROM " + personTableName + " WHERE name IS NULL AND age = 3", query3.toSQL(Person.class, provider, converter, getFieldNameConverter(), false));
		assertEquals("SELECT id FROM " + personTableName + " ORDER BY name DESC", query4.toSQL(Person.class, provider, converter, getFieldNameConverter(), false));
		assertEquals("SELECT TOP 10 id FROM " + personTableName + " WHERE name IS NULL AND age = 3", 
				query5.toSQL(Person.class, provider, converter, getFieldNameConverter(), false));
		assertEquals("SELECT TOP 14 id FROM " + personTableName + " WHERE name IS NULL AND age = 3", 
				query6.toSQL(Person.class, provider, converter, getFieldNameConverter(), false));
		assertEquals("SELECT TOP 4 id FROM " + personTableName + " WHERE name IS NULL AND age = 3 GROUP BY age", 
				query7.toSQL(Person.class, provider, converter, getFieldNameConverter(), false));
		assertEquals("SELECT id FROM " + personTableName + " JOIN " + companyTableName + " WHERE name IS NULL AND age = 3 GROUP BY url", 
				query8.toSQL(Person.class, provider, converter, getFieldNameConverter(), false));
		
		assertEquals("SELECT COUNT(*) FROM " + personTableName, query1.toSQL(Person.class, provider, converter, getFieldNameConverter(), true));
		assertEquals("SELECT COUNT(*) FROM " + personTableName, query2.toSQL(Person.class, provider, converter, getFieldNameConverter(), true));
		assertEquals("SELECT COUNT(*) FROM " + personTableName + " WHERE name IS NULL AND age = 3", query3.toSQL(Person.class, provider, converter, getFieldNameConverter(), true));
		assertEquals("SELECT COUNT(*) FROM " + personTableName + " ORDER BY name DESC", query4.toSQL(Person.class, provider, converter, getFieldNameConverter(), true));
		assertEquals("SELECT TOP 10 COUNT(*) FROM " + personTableName + " WHERE name IS NULL AND age = 3", 
				query5.toSQL(Person.class, provider, converter, getFieldNameConverter(), true));
		assertEquals("SELECT TOP 14 COUNT(*) FROM " + personTableName + " WHERE name IS NULL AND age = 3", 
				query6.toSQL(Person.class, provider, converter, getFieldNameConverter(), true));
		assertEquals("SELECT TOP 4 COUNT(*) FROM " + personTableName + " WHERE name IS NULL AND age = 3 GROUP BY age", 
				query7.toSQL(Person.class, provider, converter, getFieldNameConverter(), true));
		assertEquals("SELECT COUNT(*) FROM " + personTableName + " JOIN " + companyTableName + " WHERE name IS NULL AND age = 3 GROUP BY url", 
				query8.toSQL(Person.class, provider, converter, getFieldNameConverter(), true));
		
		provider = new MySQLDatabaseProvider("", "", "");
		
		assertEquals("SELECT id FROM " + personTableName, query1.toSQL(Person.class, provider, converter, getFieldNameConverter(), false));
		assertEquals("SELECT id,firstName,lastName FROM " + personTableName, query2.toSQL(Person.class, provider, converter, getFieldNameConverter(), false));
		assertEquals("SELECT id FROM " + personTableName + " WHERE name IS NULL AND age = 3", query3.toSQL(Person.class, provider, converter, getFieldNameConverter(), false));
		assertEquals("SELECT id FROM " + personTableName + " ORDER BY name DESC", query4.toSQL(Person.class, provider, converter, getFieldNameConverter(), false));
		assertEquals("SELECT id FROM " + personTableName + " WHERE name IS NULL AND age = 3 LIMIT 10", 
				query5.toSQL(Person.class, provider, converter, getFieldNameConverter(), false));
		assertEquals("SELECT id FROM " + personTableName + " WHERE name IS NULL AND age = 3 LIMIT 10 OFFSET 4", 
				query6.toSQL(Person.class, provider, converter, getFieldNameConverter(), false));
		assertEquals("SELECT id FROM " + personTableName + " WHERE name IS NULL AND age = 3 GROUP BY age LIMIT 4", 
				query7.toSQL(Person.class, provider, converter, getFieldNameConverter(), false));
		assertEquals("SELECT id FROM " + personTableName + " JOIN " + companyTableName + " WHERE name IS NULL AND age = 3 GROUP BY url", 
				query8.toSQL(Person.class, provider, converter, getFieldNameConverter(), false));
		
		assertEquals("SELECT COUNT(*) FROM " + personTableName, query1.toSQL(Person.class, provider, converter, getFieldNameConverter(), true));
		assertEquals("SELECT COUNT(*) FROM " + personTableName, query2.toSQL(Person.class, provider, converter, getFieldNameConverter(), true));
		assertEquals("SELECT COUNT(*) FROM " + personTableName + " WHERE name IS NULL AND age = 3", query3.toSQL(Person.class, provider, converter, getFieldNameConverter(), true));
		assertEquals("SELECT COUNT(*) FROM " + personTableName + " ORDER BY name DESC", query4.toSQL(Person.class, provider, converter, getFieldNameConverter(), true));
		assertEquals("SELECT COUNT(*) FROM " + personTableName + " WHERE name IS NULL AND age = 3 LIMIT 10", 
				query5.toSQL(Person.class, provider, converter, getFieldNameConverter(), true));
		assertEquals("SELECT COUNT(*) FROM " + personTableName + " WHERE name IS NULL AND age = 3 LIMIT 10 OFFSET 4", 
				query6.toSQL(Person.class, provider, converter, getFieldNameConverter(), true));
		assertEquals("SELECT COUNT(*) FROM " + personTableName + " WHERE name IS NULL AND age = 3 GROUP BY age LIMIT 4", 
				query7.toSQL(Person.class, provider, converter, getFieldNameConverter(), true));
		assertEquals("SELECT COUNT(*) FROM " + personTableName + " JOIN " + companyTableName + " WHERE name IS NULL AND age = 3 GROUP BY url", 
				query8.toSQL(Person.class, provider, converter, getFieldNameConverter(), true));
		
		provider = new OracleDatabaseProvider("", "", "");
		
		assertEquals("SELECT id FROM " + personTableName, query1.toSQL(Person.class, provider, converter, getFieldNameConverter(), false));
		assertEquals("SELECT id,firstName,lastName FROM " + personTableName, query2.toSQL(Person.class, provider, converter, getFieldNameConverter(), false));
		assertEquals("SELECT id FROM " + personTableName + " WHERE name IS NULL AND age = 3", query3.toSQL(Person.class, provider, converter, getFieldNameConverter(), false));
		assertEquals("SELECT id FROM " + personTableName + " ORDER BY name DESC", query4.toSQL(Person.class, provider, converter, getFieldNameConverter(), false));
		assertEquals("SELECT id FROM " + personTableName + " WHERE name IS NULL AND age = 3",
				query5.toSQL(Person.class, provider, converter, getFieldNameConverter(), false));
		assertEquals("SELECT id FROM " + personTableName + " WHERE name IS NULL AND age = 3", 
				query6.toSQL(Person.class, provider, converter, getFieldNameConverter(), false));
		assertEquals("SELECT id FROM " + personTableName + " WHERE name IS NULL AND age = 3 GROUP BY age", 
				query7.toSQL(Person.class, provider, converter, getFieldNameConverter(), false));
		assertEquals("SELECT id FROM " + personTableName + " JOIN " + companyTableName + " WHERE name IS NULL AND age = 3 GROUP BY url", 
				query8.toSQL(Person.class, provider, converter, getFieldNameConverter(), false));
		
		assertEquals("SELECT COUNT(*) FROM " + personTableName, query1.toSQL(Person.class, provider, converter, getFieldNameConverter(), true));
		assertEquals("SELECT COUNT(*) FROM " + personTableName, query2.toSQL(Person.class, provider, converter, getFieldNameConverter(), true));
		assertEquals("SELECT COUNT(*) FROM " + personTableName + " WHERE name IS NULL AND age = 3", query3.toSQL(Person.class, provider, converter, getFieldNameConverter(), true));
		assertEquals("SELECT COUNT(*) FROM " + personTableName + " ORDER BY name DESC", query4.toSQL(Person.class, provider, converter, getFieldNameConverter(), true));
		assertEquals("SELECT COUNT(*) FROM " + personTableName + " WHERE name IS NULL AND age = 3",
				query5.toSQL(Person.class, provider, converter, getFieldNameConverter(), true));
		assertEquals("SELECT COUNT(*) FROM " + personTableName + " WHERE name IS NULL AND age = 3", 
				query6.toSQL(Person.class, provider, converter, getFieldNameConverter(), true));
		assertEquals("SELECT COUNT(*) FROM " + personTableName + " WHERE name IS NULL AND age = 3 GROUP BY age", 
				query7.toSQL(Person.class, provider, converter, getFieldNameConverter(), true));
		assertEquals("SELECT COUNT(*) FROM " + personTableName + " JOIN " + companyTableName + " WHERE name IS NULL AND age = 3 GROUP BY url", 
				query8.toSQL(Person.class, provider, converter, getFieldNameConverter(), true));
		
		provider = new PostgreSQLDatabaseProvider("", "", "");
		
		assertEquals("SELECT id FROM " + personTableName, query1.toSQL(Person.class, provider, converter, getFieldNameConverter(), false));
		assertEquals("SELECT id,firstName,lastName FROM " + personTableName, query2.toSQL(Person.class, provider, converter, getFieldNameConverter(), false));
		assertEquals("SELECT id FROM " + personTableName + " WHERE name IS NULL AND age = 3", query3.toSQL(Person.class, provider, converter, getFieldNameConverter(), false));
		assertEquals("SELECT id FROM " + personTableName + " ORDER BY name DESC", query4.toSQL(Person.class, provider, converter, getFieldNameConverter(), false));
		assertEquals("SELECT id FROM " + personTableName + " WHERE name IS NULL AND age = 3 LIMIT 10", 
				query5.toSQL(Person.class, provider, converter, getFieldNameConverter(), false));
		assertEquals("SELECT id FROM " + personTableName + " WHERE name IS NULL AND age = 3 LIMIT 10 OFFSET 4", 
				query6.toSQL(Person.class, provider, converter, getFieldNameConverter(), false));
		assertEquals("SELECT id FROM " + personTableName + " WHERE name IS NULL AND age = 3 GROUP BY age LIMIT 4", 
				query7.toSQL(Person.class, provider, converter, getFieldNameConverter(), false));
		assertEquals("SELECT id FROM " + personTableName + " JOIN " + companyTableName + " WHERE name IS NULL AND age = 3 GROUP BY url", 
				query8.toSQL(Person.class, provider, converter, getFieldNameConverter(), false));
		
		assertEquals("SELECT COUNT(*) FROM " + personTableName, query1.toSQL(Person.class, provider, converter, getFieldNameConverter(), true));
		assertEquals("SELECT COUNT(*) FROM " + personTableName, query2.toSQL(Person.class, provider, converter, getFieldNameConverter(), true));
		assertEquals("SELECT COUNT(*) FROM " + personTableName + " WHERE name IS NULL AND age = 3", query3.toSQL(Person.class, provider, converter, getFieldNameConverter(), true));
		assertEquals("SELECT COUNT(*) FROM " + personTableName + " ORDER BY name DESC", query4.toSQL(Person.class, provider, converter, getFieldNameConverter(), true));
		assertEquals("SELECT COUNT(*) FROM " + personTableName + " WHERE name IS NULL AND age = 3 LIMIT 10", 
				query5.toSQL(Person.class, provider, converter, getFieldNameConverter(), true));
		assertEquals("SELECT COUNT(*) FROM " + personTableName + " WHERE name IS NULL AND age = 3 LIMIT 10 OFFSET 4", 
				query6.toSQL(Person.class, provider, converter, getFieldNameConverter(), true));
		assertEquals("SELECT COUNT(*) FROM " + personTableName + " WHERE name IS NULL AND age = 3 GROUP BY age LIMIT 4", 
				query7.toSQL(Person.class, provider, converter, getFieldNameConverter(), true));
		assertEquals("SELECT COUNT(*) FROM " + personTableName + " JOIN " + companyTableName + " WHERE name IS NULL AND age = 3 GROUP BY url", 
				query8.toSQL(Person.class, provider, converter, getFieldNameConverter(), true));
	}

	@Test
	public void testLimitOffset() throws SQLException {
		Query query = Query.select().limit(3).offset(1);
		Comment[] comments = manager.find(Comment.class, query);
		
		assertEquals(3, comments.length);
		assertEquals(postCommentIDs[1], comments[0].getID());
		assertEquals(photoCommentIDs[0], comments[1].getID());
		assertEquals(postCommentIDs[2], comments[2].getID());
	}
	
	private FieldNameConverter getFieldNameConverter() {
		return converter;
	}
}
