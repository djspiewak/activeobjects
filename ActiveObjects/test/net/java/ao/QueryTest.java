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
import net.java.ao.db.EmbeddedDerbyDatabaseProvider;
import net.java.ao.db.HSQLDatabaseProvider;
import net.java.ao.db.JTDSSQLServerDatabaseProvider;
import net.java.ao.db.MySQLDatabaseProvider;
import net.java.ao.db.OracleDatabaseProvider;
import net.java.ao.db.PostgreSQLDatabaseProvider;
import net.java.ao.schema.CamelCaseTableNameConverter;
import net.java.ao.schema.TableNameConverter;

import org.junit.Test;

import test.schema.Company;
import test.schema.Person;

/**
 * @author Daniel Spiewak
 */
public class QueryTest {

	@Test
	public void testToSQL() {
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
		
		assertEquals("SELECT id FROM person", query1.toSQL(Person.class, provider, converter, false));
		assertEquals("SELECT id,firstName,lastName FROM person", query2.toSQL(Person.class, provider, converter, false));
		assertEquals("SELECT id FROM person WHERE name IS NULL AND age = 3", query3.toSQL(Person.class, provider, converter, false));
		assertEquals("SELECT id FROM person ORDER BY name DESC", query4.toSQL(Person.class, provider, converter, false));
		assertEquals("SELECT id FROM person WHERE name IS NULL AND age = 3", query5.toSQL(Person.class, provider, converter, false));
		assertEquals("SELECT id FROM person WHERE name IS NULL AND age = 3", query6.toSQL(Person.class, provider, converter, false));
		assertEquals("SELECT id FROM person WHERE name IS NULL AND age = 3 GROUP BY age", 
				query7.toSQL(Person.class, provider, converter, false));
		assertEquals("SELECT id FROM person JOIN company WHERE name IS NULL AND age = 3 GROUP BY url", 
				query8.toSQL(Person.class, provider, converter, false));
		
		assertEquals("SELECT COUNT(*) FROM person", query1.toSQL(Person.class, provider, converter, true));
		assertEquals("SELECT COUNT(*) FROM person", query2.toSQL(Person.class, provider, converter, true));
		assertEquals("SELECT COUNT(*) FROM person WHERE name IS NULL AND age = 3", query3.toSQL(Person.class, provider, converter, true));
		assertEquals("SELECT COUNT(*) FROM person ORDER BY name DESC", query4.toSQL(Person.class, provider, converter, true));
		assertEquals("SELECT COUNT(*) FROM person WHERE name IS NULL AND age = 3", query5.toSQL(Person.class, provider, converter, true));
		assertEquals("SELECT COUNT(*) FROM person WHERE name IS NULL AND age = 3", query6.toSQL(Person.class, provider, converter, true));
		assertEquals("SELECT COUNT(*) FROM person WHERE name IS NULL AND age = 3 GROUP BY age", 
				query7.toSQL(Person.class, provider, converter, true));
		assertEquals("SELECT COUNT(*) FROM person JOIN company WHERE name IS NULL AND age = 3 GROUP BY url", 
				query8.toSQL(Person.class, provider, converter, true));
		
		provider = new HSQLDatabaseProvider("", "", "");
		
		assertEquals("SELECT id FROM person", query1.toSQL(Person.class, provider, converter, false));
		assertEquals("SELECT id,firstName,lastName FROM person", query2.toSQL(Person.class, provider, converter, false));
		assertEquals("SELECT id FROM person WHERE name IS NULL AND age = 3", query3.toSQL(Person.class, provider, converter, false));
		assertEquals("SELECT id FROM person ORDER BY name DESC", query4.toSQL(Person.class, provider, converter, false));
		assertEquals("SELECT id FROM person WHERE name IS NULL AND age = 3 LIMIT 10", 
				query5.toSQL(Person.class, provider, converter, false));
		assertEquals("SELECT id FROM person WHERE name IS NULL AND age = 3 LIMIT 10 OFFSET 4", 
				query6.toSQL(Person.class, provider, converter, false));
		assertEquals("SELECT id FROM person WHERE name IS NULL AND age = 3 GROUP BY age LIMIT 4", 
				query7.toSQL(Person.class, provider, converter, false));
		assertEquals("SELECT id FROM person JOIN company WHERE name IS NULL AND age = 3 GROUP BY url", 
				query8.toSQL(Person.class, provider, converter, false));
		
		assertEquals("SELECT COUNT(*) FROM person", query1.toSQL(Person.class, provider, converter, true));
		assertEquals("SELECT COUNT(*) FROM person", query2.toSQL(Person.class, provider, converter, true));
		assertEquals("SELECT COUNT(*) FROM person WHERE name IS NULL AND age = 3", query3.toSQL(Person.class, provider, converter, true));
		assertEquals("SELECT COUNT(*) FROM person ORDER BY name DESC", query4.toSQL(Person.class, provider, converter, true));
		assertEquals("SELECT COUNT(*) FROM person WHERE name IS NULL AND age = 3 LIMIT 10", 
				query5.toSQL(Person.class, provider, converter, true));
		assertEquals("SELECT COUNT(*) FROM person WHERE name IS NULL AND age = 3 LIMIT 10 OFFSET 4", 
				query6.toSQL(Person.class, provider, converter, true));
		assertEquals("SELECT COUNT(*) FROM person WHERE name IS NULL AND age = 3 GROUP BY age LIMIT 4", 
				query7.toSQL(Person.class, provider, converter, true));
		assertEquals("SELECT COUNT(*) FROM person JOIN company WHERE name IS NULL AND age = 3 GROUP BY url", 
				query8.toSQL(Person.class, provider, converter, true));
		
		provider = new JTDSSQLServerDatabaseProvider("", "", "");
		
		assertEquals("SELECT id FROM person", query1.toSQL(Person.class, provider, converter, false));
		assertEquals("SELECT id,firstName,lastName FROM person", query2.toSQL(Person.class, provider, converter, false));
		assertEquals("SELECT id FROM person WHERE name IS NULL AND age = 3", query3.toSQL(Person.class, provider, converter, false));
		assertEquals("SELECT id FROM person ORDER BY name DESC", query4.toSQL(Person.class, provider, converter, false));
		assertEquals("SELECT TOP 10 id FROM person WHERE name IS NULL AND age = 3", 
				query5.toSQL(Person.class, provider, converter, false));
		assertEquals("SELECT TOP 14 id FROM person WHERE name IS NULL AND age = 3", 
				query6.toSQL(Person.class, provider, converter, false));
		assertEquals("SELECT TOP 4 id FROM person WHERE name IS NULL AND age = 3 GROUP BY age", 
				query7.toSQL(Person.class, provider, converter, false));
		assertEquals("SELECT id FROM person JOIN company WHERE name IS NULL AND age = 3 GROUP BY url", 
				query8.toSQL(Person.class, provider, converter, false));
		
		assertEquals("SELECT COUNT(*) FROM person", query1.toSQL(Person.class, provider, converter, true));
		assertEquals("SELECT COUNT(*) FROM person", query2.toSQL(Person.class, provider, converter, true));
		assertEquals("SELECT COUNT(*) FROM person WHERE name IS NULL AND age = 3", query3.toSQL(Person.class, provider, converter, true));
		assertEquals("SELECT COUNT(*) FROM person ORDER BY name DESC", query4.toSQL(Person.class, provider, converter, true));
		assertEquals("SELECT TOP 10 COUNT(*) FROM person WHERE name IS NULL AND age = 3", 
				query5.toSQL(Person.class, provider, converter, true));
		assertEquals("SELECT TOP 14 COUNT(*) FROM person WHERE name IS NULL AND age = 3", 
				query6.toSQL(Person.class, provider, converter, true));
		assertEquals("SELECT TOP 4 COUNT(*) FROM person WHERE name IS NULL AND age = 3 GROUP BY age", 
				query7.toSQL(Person.class, provider, converter, true));
		assertEquals("SELECT COUNT(*) FROM person JOIN company WHERE name IS NULL AND age = 3 GROUP BY url", 
				query8.toSQL(Person.class, provider, converter, true));
		
		provider = new MySQLDatabaseProvider("", "", "");
		
		assertEquals("SELECT id FROM person", query1.toSQL(Person.class, provider, converter, false));
		assertEquals("SELECT id,firstName,lastName FROM person", query2.toSQL(Person.class, provider, converter, false));
		assertEquals("SELECT id FROM person WHERE name IS NULL AND age = 3", query3.toSQL(Person.class, provider, converter, false));
		assertEquals("SELECT id FROM person ORDER BY name DESC", query4.toSQL(Person.class, provider, converter, false));
		assertEquals("SELECT id FROM person WHERE name IS NULL AND age = 3 LIMIT 10", 
				query5.toSQL(Person.class, provider, converter, false));
		assertEquals("SELECT id FROM person WHERE name IS NULL AND age = 3 LIMIT 10 OFFSET 4", 
				query6.toSQL(Person.class, provider, converter, false));
		assertEquals("SELECT id FROM person WHERE name IS NULL AND age = 3 GROUP BY age LIMIT 4", 
				query7.toSQL(Person.class, provider, converter, false));
		assertEquals("SELECT id FROM person JOIN company WHERE name IS NULL AND age = 3 GROUP BY url", 
				query8.toSQL(Person.class, provider, converter, false));
		
		assertEquals("SELECT COUNT(*) FROM person", query1.toSQL(Person.class, provider, converter, true));
		assertEquals("SELECT COUNT(*) FROM person", query2.toSQL(Person.class, provider, converter, true));
		assertEquals("SELECT COUNT(*) FROM person WHERE name IS NULL AND age = 3", query3.toSQL(Person.class, provider, converter, true));
		assertEquals("SELECT COUNT(*) FROM person ORDER BY name DESC", query4.toSQL(Person.class, provider, converter, true));
		assertEquals("SELECT COUNT(*) FROM person WHERE name IS NULL AND age = 3 LIMIT 10", 
				query5.toSQL(Person.class, provider, converter, true));
		assertEquals("SELECT COUNT(*) FROM person WHERE name IS NULL AND age = 3 LIMIT 10 OFFSET 4", 
				query6.toSQL(Person.class, provider, converter, true));
		assertEquals("SELECT COUNT(*) FROM person WHERE name IS NULL AND age = 3 GROUP BY age LIMIT 4", 
				query7.toSQL(Person.class, provider, converter, true));
		assertEquals("SELECT COUNT(*) FROM person JOIN company WHERE name IS NULL AND age = 3 GROUP BY url", 
				query8.toSQL(Person.class, provider, converter, true));
		
		provider = new OracleDatabaseProvider("", "", "");
		
		assertEquals("SELECT id FROM person", query1.toSQL(Person.class, provider, converter, false));
		assertEquals("SELECT id,firstName,lastName FROM person", query2.toSQL(Person.class, provider, converter, false));
		assertEquals("SELECT id FROM person WHERE name IS NULL AND age = 3", query3.toSQL(Person.class, provider, converter, false));
		assertEquals("SELECT id FROM person ORDER BY name DESC", query4.toSQL(Person.class, provider, converter, false));
		assertEquals("SELECT id FROM person WHERE name IS NULL AND age = 3",
				query5.toSQL(Person.class, provider, converter, false));
		assertEquals("SELECT id FROM person WHERE name IS NULL AND age = 3", 
				query6.toSQL(Person.class, provider, converter, false));
		assertEquals("SELECT id FROM person WHERE name IS NULL AND age = 3 GROUP BY age", 
				query7.toSQL(Person.class, provider, converter, false));
		assertEquals("SELECT id FROM person JOIN company WHERE name IS NULL AND age = 3 GROUP BY url", 
				query8.toSQL(Person.class, provider, converter, false));
		
		assertEquals("SELECT COUNT(*) FROM person", query1.toSQL(Person.class, provider, converter, true));
		assertEquals("SELECT COUNT(*) FROM person", query2.toSQL(Person.class, provider, converter, true));
		assertEquals("SELECT COUNT(*) FROM person WHERE name IS NULL AND age = 3", query3.toSQL(Person.class, provider, converter, true));
		assertEquals("SELECT COUNT(*) FROM person ORDER BY name DESC", query4.toSQL(Person.class, provider, converter, true));
		assertEquals("SELECT COUNT(*) FROM person WHERE name IS NULL AND age = 3",
				query5.toSQL(Person.class, provider, converter, true));
		assertEquals("SELECT COUNT(*) FROM person WHERE name IS NULL AND age = 3", 
				query6.toSQL(Person.class, provider, converter, true));
		assertEquals("SELECT COUNT(*) FROM person WHERE name IS NULL AND age = 3 GROUP BY age", 
				query7.toSQL(Person.class, provider, converter, true));
		assertEquals("SELECT COUNT(*) FROM person JOIN company WHERE name IS NULL AND age = 3 GROUP BY url", 
				query8.toSQL(Person.class, provider, converter, true));
		
		provider = new PostgreSQLDatabaseProvider("", "", "");
		
		assertEquals("SELECT id FROM person", query1.toSQL(Person.class, provider, converter, false));
		assertEquals("SELECT id,firstName,lastName FROM person", query2.toSQL(Person.class, provider, converter, false));
		assertEquals("SELECT id FROM person WHERE name IS NULL AND age = 3", query3.toSQL(Person.class, provider, converter, false));
		assertEquals("SELECT id FROM person ORDER BY name DESC", query4.toSQL(Person.class, provider, converter, false));
		assertEquals("SELECT id FROM person WHERE name IS NULL AND age = 3 LIMIT 10", 
				query5.toSQL(Person.class, provider, converter, false));
		assertEquals("SELECT id FROM person WHERE name IS NULL AND age = 3 LIMIT 10 OFFSET 4", 
				query6.toSQL(Person.class, provider, converter, false));
		assertEquals("SELECT id FROM person WHERE name IS NULL AND age = 3 GROUP BY age LIMIT 4", 
				query7.toSQL(Person.class, provider, converter, false));
		assertEquals("SELECT id FROM person JOIN company WHERE name IS NULL AND age = 3 GROUP BY url", 
				query8.toSQL(Person.class, provider, converter, false));
		
		assertEquals("SELECT COUNT(*) FROM person", query1.toSQL(Person.class, provider, converter, true));
		assertEquals("SELECT COUNT(*) FROM person", query2.toSQL(Person.class, provider, converter, true));
		assertEquals("SELECT COUNT(*) FROM person WHERE name IS NULL AND age = 3", query3.toSQL(Person.class, provider, converter, true));
		assertEquals("SELECT COUNT(*) FROM person ORDER BY name DESC", query4.toSQL(Person.class, provider, converter, true));
		assertEquals("SELECT COUNT(*) FROM person WHERE name IS NULL AND age = 3 LIMIT 10", 
				query5.toSQL(Person.class, provider, converter, true));
		assertEquals("SELECT COUNT(*) FROM person WHERE name IS NULL AND age = 3 LIMIT 10 OFFSET 4", 
				query6.toSQL(Person.class, provider, converter, true));
		assertEquals("SELECT COUNT(*) FROM person WHERE name IS NULL AND age = 3 GROUP BY age LIMIT 4", 
				query7.toSQL(Person.class, provider, converter, true));
		assertEquals("SELECT COUNT(*) FROM person JOIN company WHERE name IS NULL AND age = 3 GROUP BY url", 
				query8.toSQL(Person.class, provider, converter, true));
	}
}
