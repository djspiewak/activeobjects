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

import net.java.ao.Common;
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
			
			PreparedStatement stmt = conn.prepareStatement(sql);
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
				field.setNotNull(res.getString("IS_NULLABLE").equals("NO"));
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
				
				key.setForeignField(res.getString("PKCOLUMN_NAME"));
				key.setField(res.getString("FKCOLUMN_NAME"));
				key.setTable(res.getString("PKTABLE_NAME"));
				key.setDomesticTable(table.getName());
				
				foreignKeys.add(key);
			}
			res.close();
			
			table.setFields(fields.values().toArray(new DDLField[fields.size()]));
			table.setForeignKeys(foreignKeys.toArray(new DDLForeignKey[foreignKeys.size()]));
		}
		
		return tables.toArray(new DDLTable[tables.size()]);
	}
	
	/**
	 * Returns the difference between <code>from</code> and
	 * <code>onto</code> with a bias toward <code>from</code>.
	 * Thus, if a table is present in <code>from</code> but not
	 * <code>onto</code>, a <code>CREATE TABLE</code>
	 * statement will be generated.
	 */
	public static DDLAction[] diffSchema(DDLTable[] fromArray, DDLTable[] ontoArray) {
		List<DDLAction> actions = new ArrayList<DDLAction>();
		
		List<DDLTable> createTables = new ArrayList<DDLTable>();
		List<DDLTable> dropTables = new ArrayList<DDLTable>();
		List<DDLTable> alterTables = new ArrayList<DDLTable>();
		
		Map<String, DDLTable> from = new HashMap<String, DDLTable>();
		Map<String, DDLTable> onto = new HashMap<String, DDLTable>();
		
		for (DDLTable table : fromArray) {
			from.put(table.getName(), table);
		}
		for (DDLTable table : ontoArray) {
			onto.put(table.getName(), table);
		}
		
		for (DDLTable table : fromArray) {
			if (onto.containsKey(table.getName())) {
				alterTables.add(table);
			} else {
				createTables.add(table);
			}
		}
		
		for (DDLTable table : ontoArray) {
			if (!from.containsKey(table.getName())) {
				dropTables.add(table);
			}
		}
		
		for (DDLTable table : createTables) {
			DDLAction action = new DDLAction(DDLActionType.CREATE);
			action.setTable(table);
			actions.add(action);
		}
		
		for (DDLTable table : dropTables) {
			DDLAction action = new DDLAction(DDLActionType.DROP);
			action.setTable(table);
			actions.add(action);
		}
		
		for (DDLTable fromTable : alterTables) {
			DDLTable ontoTable = onto.get(fromTable.getName());
			
			List<DDLField> createFields = new ArrayList<DDLField>();
			List<DDLField> dropFields = new ArrayList<DDLField>();
			List<DDLField> alterFields = new ArrayList<DDLField>();
			
			Map<String, DDLField> fromFields = new HashMap<String, DDLField>();
			Map<String, DDLField> ontoFields = new HashMap<String, DDLField>();
			
			for (DDLField field : fromTable.getFields()) {
				fromFields.put(field.getName(), field);
			}
			for (DDLField field : ontoTable.getFields()) {
				ontoFields.put(field.getName(), field);
			}
			
			for (DDLField field : fromTable.getFields()) {
				if (ontoFields.containsKey(field.getName())) {
					alterFields.add(field);
				} else {
					createFields.add(field);
				}
			}
			
			for (DDLField field : ontoTable.getFields()) {
				if (!fromFields.containsKey(field.getName())) {
					dropFields.add(field);
				}
			}
			
			for (DDLField field : createFields) {
				DDLAction action = new DDLAction(DDLActionType.ALTER_ADD_COLUMN);
				action.setTable(fromTable);
				action.setField(field);
				actions.add(action);
			}
			
			for (DDLField field : dropFields) {
				DDLAction action = new DDLAction(DDLActionType.ALTER_DROP_COLUMN);
				action.setTable(fromTable);
				action.setField(field);
				actions.add(action);
			}
			
			for (DDLField fromField : alterFields) {
				DDLField ontoField = ontoFields.get(fromField.getName());
				
				if (fromField.getDefaultValue() == null && ontoField.getDefaultValue() != null) {
					actions.add(createColumnAlterAction(fromTable, ontoField, fromField));
				} else if (fromField.getDefaultValue() != null 
						&& !Common.fuzzyCompare(fromField.getDefaultValue(), ontoField.getDefaultValue())) {
					actions.add(createColumnAlterAction(fromTable, ontoField, fromField));
				} /*else if (!fromField.getOnUpdate().equals(ontoField.getOnUpdate())) {
					actions.add(createColumnAlterAction(fromTable, fromField));
				} else if (fromField.getPrecision() != ontoField.getPrecision()) {
					actions.add(createColumnAlterAction(fromTable, ontoField, fromField));
				} else if (fromField.getScale() != ontoField.getScale()) {
					actions.add(createColumnAlterAction(fromTable, ontoField, fromField));
				}*/ else if (!Common.fuzzyTypeCompare(fromField.getType(), ontoField.getType())) {
					actions.add(createColumnAlterAction(fromTable, ontoField, fromField));
				} else if (fromField.isAutoIncrement() != ontoField.isAutoIncrement()) {
					actions.add(createColumnAlterAction(fromTable, ontoField, fromField));
				} /*else if (fromField.isNotNull() != ontoField.isNotNull()) {
					actions.add(createColumnAlterAction(fromTable, ontoField, fromField));
				} else if (fromField.isUnique() != ontoField.isUnique()) {
					actions.add(createColumnAlterAction(fromTable, fromField));
				}*/
			}
			
			// foreign keys
			List<DDLForeignKey> addKeys = new ArrayList<DDLForeignKey>();
			List<DDLForeignKey> dropKeys = new ArrayList<DDLForeignKey>();
			
			for (DDLForeignKey fromKey : fromTable.getForeignKeys()) {
				for (DDLForeignKey ontoKey : ontoTable.getForeignKeys()) {
					if (!(fromKey.getTable().equals(ontoKey.getTable()) && fromKey.getField().equals(ontoKey.getField())
							&& fromKey.getForeignField().equals(ontoKey.getForeignField())
							&& fromKey.getDomesticTable().equals(ontoKey.getDomesticTable()))) {
						addKeys.add(fromKey);
					}
				}
			}
			
			for (DDLForeignKey ontoKey : ontoTable.getForeignKeys()) {
				for (DDLForeignKey fromKey : fromTable.getForeignKeys()) {
					if (!(ontoKey.getTable().equals(fromKey.getTable()) && ontoKey.getField().equals(fromKey.getField())
							&& ontoKey.getForeignField().equals(fromKey.getForeignField())
							 && ontoKey.getDomesticTable().equals(fromKey.getDomesticTable()))) {
						dropKeys.add(ontoKey);
					}
				}
			}
			
			for (DDLForeignKey key : addKeys) {
				DDLAction action = new DDLAction(DDLActionType.ALTER_ADD_KEY);
				action.setKey(key);
				actions.add(action);
			}

			for (DDLForeignKey key : dropKeys) {
				DDLAction action = new DDLAction(DDLActionType.ALTER_DROP_KEY);
				action.setKey(key);
				actions.add(action);
			}
		}
		
		return actions.toArray(new DDLAction[actions.size()]);
	}
	
	private static DDLAction createColumnAlterAction(DDLTable table, DDLField oldField, DDLField field) {
		DDLAction action = new DDLAction(DDLActionType.ALTER_CHANGE_COLUMN);
		action.setTable(table);
		action.setField(field);
		action.setOldField(oldField);
		return action;
	}
}
