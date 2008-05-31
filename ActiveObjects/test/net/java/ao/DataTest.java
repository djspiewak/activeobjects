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

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;

import net.java.ao.schema.CamelCaseFieldNameConverter;
import net.java.ao.schema.CamelCaseTableNameConverter;
import net.java.ao.schema.FieldNameConverter;
import net.java.ao.schema.PluralizedNameConverter;
import net.java.ao.schema.TableNameConverter;
import net.java.ao.schema.UnderscoreFieldNameConverter;
import net.java.ao.schema.UnderscoreTableNameConverter;
import net.java.ao.types.ClassType;
import net.java.ao.types.TypeManager;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import test.schema.Distribution;

/**
 * @author Daniel Spiewak
 */
@RunWith(Parameterized.class)
public abstract class DataTest {
	protected final EntityManager manager;
	
	protected int personID;
	protected int noseID;
	protected long companyID;

	protected int[] penIDs;
	protected int[] defenceIDs;
	protected int[] suitIDs;

	protected long[] coolCompanyIDs;

	protected int postID;
	protected int photoID;

	protected int[] postCommentIDs;
	protected int[] photoCommentIDs;

	protected int[] bookIDs;
	protected int[] magazineIDs;

	protected int[][] bookAuthorIDs;
	protected int[][] magazineAuthorIDs;

	protected int[][] bookDistributionIDs;
	protected Class<? extends Distribution>[][] bookDistributionTypes;

	protected int[][] magazineDistributionIDs;
	protected Class<? extends Distribution>[][] magazineDistributionTypes;

	protected int[] addressIDs;
	protected int[] messageIDs;

	public DataTest(TableNameConverter tableConverter, FieldNameConverter fieldConverter) throws SQLException {
		manager = new EntityManager("jdbc:hsqldb:mem:test_database" + System.currentTimeMillis(), "sa", "");
//		manager = new EntityManager("jdbc:derby:test_database;create=true", "sa", "jeffbridges");
//		manager = new EntityManager("jdbc:oracle:thin:@192.168.101.17:1521:xe", "activeobjects", "password");
		
		manager.setTableNameConverter(tableConverter);
		manager.setFieldNameConverter(fieldConverter);
	}
	
	@Before
	public void setup() throws SQLException {
		DataStruct data = TestUtilities.setUpEntityManager(manager);
		
		personID = data.personID;
		noseID = data.noseID;
		companyID = data.companyID;
		penIDs = data.penIDs;
		defenceIDs = data.defenceIDs;
		suitIDs = data.suitIDs;
		coolCompanyIDs = data.coolCompanyIDs;
		postID = data.postID;
		photoID = data.photoID;
		postCommentIDs = data.postCommentIDs;
		photoCommentIDs = data.photoCommentIDs;
		bookIDs = data.bookIDs;
		magazineIDs = data.magazineIDs;
		bookAuthorIDs = data.bookAuthorIDs;
		magazineAuthorIDs = data.magazineAuthorIDs;
		bookDistributionIDs = data.bookDistributionIDs;
		bookDistributionTypes = data.bookDistributionTypes;
		magazineDistributionIDs = data.magazineDistributionIDs;
		magazineDistributionTypes = data.magazineDistributionTypes;
		addressIDs = data.addressIDs;
		messageIDs = data.messageIDs;
	}

	@After
	public void tearDown() throws SQLException {
		TestUtilities.tearDownEntityManager(manager);
		manager.getProvider().dispose();
	}
	
	@Parameters
	public static Collection<Object[]> data() {
		CamelCaseTableNameConverter camelCaseTableNameConverter = new CamelCaseTableNameConverter();
		UnderscoreTableNameConverter underscoreTableNameConverter = new UnderscoreTableNameConverter(false);
		UnderscoreTableNameConverter underscoreTableNameConverter2 = new UnderscoreTableNameConverter(true);
		
		PluralizedNameConverter pluralizedCamelNameConverter = new PluralizedNameConverter(camelCaseTableNameConverter);
		PluralizedNameConverter pluralizedUnderscore2NameConverter = new PluralizedNameConverter(underscoreTableNameConverter2);
		
		CamelCaseFieldNameConverter camelCaseFieldNameConverter = new CamelCaseFieldNameConverter();
		UnderscoreFieldNameConverter underscoreFieldNameConverter = new UnderscoreFieldNameConverter(false);
		UnderscoreFieldNameConverter underscoreFieldNameConverter2 = new UnderscoreFieldNameConverter(true);
		
		// try all combinations, just for fun
		return Arrays.asList(new Object[][] {
			{camelCaseTableNameConverter, camelCaseFieldNameConverter},
//			{camelCaseTableNameConverter, underscoreFieldNameConverter},
//			{camelCaseTableNameConverter, underscoreFieldNameConverter2},
			
			{underscoreTableNameConverter, camelCaseFieldNameConverter},
//			{underscoreTableNameConverter, underscoreFieldNameConverter},
//			{underscoreTableNameConverter, underscoreFieldNameConverter2},

			{pluralizedCamelNameConverter, camelCaseFieldNameConverter},
//			{pluralizedCamelNameConverter, underscoreFieldNameConverter},
//			{pluralizedCamelNameConverter, underscoreFieldNameConverter2},
			
			{pluralizedUnderscore2NameConverter, camelCaseFieldNameConverter}
//			{pluralizedUnderscore2NameConverter, underscoreFieldNameConverter},
//			{pluralizedUnderscore2NameConverter, underscoreFieldNameConverter2}
		});
	}

	@BeforeClass
	public static void classSetup() throws SQLException {
		TypeManager.getInstance().addType(new ClassType());
	}
}
