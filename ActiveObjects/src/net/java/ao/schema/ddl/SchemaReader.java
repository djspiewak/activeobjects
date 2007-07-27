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
package net.java.ao.schema.ddl;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.java.ao.DatabaseProvider;
import net.java.ao.Query;

/**
 * @author Daniel Spiewak
 */
public final class SchemaReader {
	
	/**
	 * Currently doesn't account for:
	 * 
	 * <ul>
	 * 	<li>setOnUpdate</li>
	 * 	<li>setUnique</li>
	 * </ul>
	 */
	public static DDLTable[] readSchema(DatabaseProvider provider) throws SQLException {
		Connection conn = provider.getConnection();
		DatabaseMetaData dbmd = conn.getMetaData();
		ResultSet res = dbmd.getTables(null, null, "", null);
		
		List<DDLTable> tables = new ArrayList<DDLTable>();
		
		while (res.next()) {
			DDLTable table = new DDLTable();
			table.setName(res.getString("TABLE_NAME"));
			tables.add(table);
		}
		res.close();
		
		for (DDLTable table : tables) {
			Query query = Query.select("*").from(table.getName()).limit(1);
			String sql = provider.renderQuery(query, null, false);
			
			PreparedStatement stmt = conn.prepareCall(sql);
			provider.setQueryStatementProperties(stmt, query);
			
			res = stmt.executeQuery();
			
			Map<String, DDLField> fields = new HashMap<String, DDLField>();
			ResultSetMetaData rsmd = res.getMetaData();
			for (int i = 1; i < rsmd.getColumnCount() + 1; i++) {
				DDLField field = new DDLField();
				
				field.setName(rsmd.getColumnName(i));
				field.setType(rsmd.getColumnType(i));
				
				field.setPrecision(rsmd.getPrecision(i));
				field.setScale(rsmd.getScale(i));
				
				field.setAutoIncrement(rsmd.isAutoIncrement(i));
				field.setNotNull(rsmd.isNullable(i) == ResultSetMetaData.columnNoNulls);
				
				fields.put(field.getName(), field);
			}
			res.close();
			
			res = dbmd.getColumns(null, null, table.getName(), null);
			while (res.next()) {
				DDLField field = fields.get(res.getString("COLUMN_NAME"));
				
				field.setDefaultValue(provider.parseValue(field.getType(), res.getString("COLUMN_DEF")));
			}
			res.close();
			
			res = dbmd.getPrimaryKeys(null, null, table.getName());
			while (res.next()) {
				DDLField field = fields.get(res.getString("COLUMN_NAME"));
				
				field.setPrimaryKey(true);
			}
			res.close();
			
			List<DDLForeignKey> foreignKeys = new ArrayList<DDLForeignKey>();
			res = dbmd.getImportedKeys(null, null, table.getName());
			while (res.next()) {
				DDLForeignKey key = new DDLForeignKey();
				
				key.setForeignField("id");		// TODO	major hack here!!
				key.setField(res.getString("FKCOLUMN_NAME"));
				key.setTable(res.getString("FKTABLE_NAME"));
				
				foreignKeys.add(key);
			}
			res.close();
			
			table.setFields(fields.values().toArray(new DDLField[fields.size()]));
			table.setForeignKeys(foreignKeys.toArray(new DDLForeignKey[foreignKeys.size()]));
		}
		
		return tables.toArray(new DDLTable[tables.size()]);
	}
}
