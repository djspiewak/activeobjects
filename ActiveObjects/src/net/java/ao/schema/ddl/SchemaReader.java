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
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.java.ao.Common;
import net.java.ao.DatabaseProvider;
import net.java.ao.Query;
import net.java.ao.types.TypeManager;

/**
 * WARNING: <i>Not</i> part of the public API.  This class is public only
 * to allow its use within other packages in the ActiveObjects library.
 * 
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
		ResultSet res = provider.getTables(conn);
		TypeManager manager = TypeManager.getInstance();
		
		if (res == null) {
			return new DDLTable[0];
		}
		
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
				field.setType(manager.getType(rsmd.getColumnType(i)));
				
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
				
				field.setDefaultValue(provider.parseValue(field.getType().getType(), res.getString("COLUMN_DEF")));
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
		Set<DDLAction> actions = new HashSet<DDLAction>();
		
		List<DDLTable> createTables = new ArrayList<DDLTable>();
		List<DDLTable> dropTables = new ArrayList<DDLTable>();
		List<DDLTable> alterTables = new ArrayList<DDLTable>();
		
		Map<String, DDLTable> from = new HashMap<String, DDLTable>();
		Map<String, DDLTable> onto = new HashMap<String, DDLTable>();
		
		for (DDLTable table : fromArray) {
			from.put(table.getName().toLowerCase(), table);
		}
		for (DDLTable table : ontoArray) {
			onto.put(table.getName().toLowerCase(), table);
		}
		
		for (DDLTable table : fromArray) {
			if (onto.containsKey(table.getName().toLowerCase())) {
				alterTables.add(table);
			} else {
				createTables.add(table);
			}
		}
		
		for (DDLTable table : ontoArray) {
			if (!from.containsKey(table.getName().toLowerCase())) {
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
			DDLTable ontoTable = onto.get(fromTable.getName().toLowerCase());
			
			List<DDLField> createFields = new ArrayList<DDLField>();
			List<DDLField> dropFields = new ArrayList<DDLField>();
			List<DDLField> alterFields = new ArrayList<DDLField>();
			
			Map<String, DDLField> fromFields = new HashMap<String, DDLField>();
			Map<String, DDLField> ontoFields = new HashMap<String, DDLField>();
			
			for (DDLField field : fromTable.getFields()) {
				fromFields.put(field.getName().toLowerCase(), field);
			}
			for (DDLField field : ontoTable.getFields()) {
				ontoFields.put(field.getName().toLowerCase(), field);
			}
			
			for (DDLField field : fromTable.getFields()) {
				if (ontoFields.containsKey(field.getName().toLowerCase())) {
					alterFields.add(field);
				} else {
					createFields.add(field);
				}
			}
			
			for (DDLField field : ontoTable.getFields()) {
				if (!fromFields.containsKey(field.getName().toLowerCase())) {
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
				DDLField ontoField = ontoFields.get(fromField.getName().toLowerCase());
				
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
				}*/ else if (!Common.fuzzyTypeCompare(fromField.getType().getType(), ontoField.getType().getType())) {
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
					if (!(fromKey.getTable().equalsIgnoreCase(ontoKey.getTable()) 
							&& fromKey.getForeignField().equalsIgnoreCase(ontoKey.getForeignField()))
							&& fromKey.getField().equalsIgnoreCase(ontoKey.getField())
							&& fromKey.getDomesticTable().equalsIgnoreCase(ontoKey.getDomesticTable())) {
						addKeys.add(fromKey);
					}
				}
			}
			
			for (DDLForeignKey ontoKey : ontoTable.getForeignKeys()) {
				for (DDLForeignKey fromKey : fromTable.getForeignKeys()) {
					if (!(ontoKey.getTable().equalsIgnoreCase(fromKey.getTable()) 
							&& ontoKey.getForeignField().equalsIgnoreCase(fromKey.getForeignField()))
							&& ontoKey.getField().equalsIgnoreCase(fromKey.getField())
							 && ontoKey.getDomesticTable().equalsIgnoreCase(fromKey.getDomesticTable())) {
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
			
			// field indexes
//			List<DDLIndex> addIndexes = new ArrayList<DDLIndex>();
//			List<DDLIndex> dropIndexes = new ArrayList<DDLIndex>();
//			
//			for (DDLIndex fromIndex : fromTable.getIndexes()) {
//				boolean found = false;
//				
//				for (DDLIndex ontoIndex : ontoTable.getIndexes()) {
//					if (fromIndex.getTable().equalsIgnoreCase(ontoIndex.getTable()) 
//							&& fromIndex.getField().equalsIgnoreCase(ontoIndex.getField())) {
//						found = true;
//						break;
//					}
//				}
//				
//				if (!found) {
//					addIndexes.add(fromIndex);
//				}
//			}
//			
//			for (DDLIndex ontoIndex : ontoTable.getIndexes()) {
//				boolean found = false;
//				
//				for (DDLIndex fromIndex : fromTable.getIndexes()) {
//					if (ontoIndex.getTable().equalsIgnoreCase(fromIndex.getTable()) 
//							&& ontoIndex.getField().equalsIgnoreCase(fromIndex.getField())) {
//						found = true;
//						break;
//					}
//				}
//				
//				if (!found) {
//					dropIndexes.add(ontoIndex);
//				}
//			}
//			
//			for (DDLIndex index : addIndexes) {
//				DDLAction action = new DDLAction(DDLActionType.CREATE_INDEX);
//				action.setIndex(index);
//				actions.add(action);
//			}
//
//			for (DDLIndex index : dropIndexes) {
//				DDLAction action = new DDLAction(DDLActionType.DROP_INDEX);
//				action.setIndex(index);
//				actions.add(action);
//			}
		}
		
		return actions.toArray(new DDLAction[actions.size()]);
	}
	
	public static DDLAction[] sortTopologically(DDLAction[] actions) {
		List<DDLAction> back = new LinkedList<DDLAction>();
		Map<DDLAction, Set<DDLAction>> deps = new HashMap<DDLAction, Set<DDLAction>>();
		Set<DDLAction> roots = new HashSet<DDLAction>();
		
		performSort(actions, deps, roots);
		
		while (!roots.isEmpty()) {
			DDLAction[] rootsArray = roots.toArray(new DDLAction[roots.size()]);
			roots.remove(rootsArray[0]);
			
			back.add(rootsArray[0]);
			
			List<DDLAction> toRemove = new LinkedList<DDLAction>();
			Iterator<DDLAction> depIterator = deps.keySet().iterator();
			while (depIterator.hasNext()) {
				DDLAction depAction = depIterator.next();
				
				Set<DDLAction> individualDeps = deps.get(depAction);
				individualDeps.remove(rootsArray[0]);

				if (individualDeps.isEmpty()) {
					roots.add(depAction);
					toRemove.add(depAction);
				}
			}
			
			for (DDLAction action : toRemove) {
				deps.remove(action);
			}
		}
		
		return back.toArray(new DDLAction[back.size()]);
	}
	
	/*
	 * DROP_KEY
	 * DROP_INDEX
	 * DROP_COLUMN
	 * CHANGE_COLUMN
	 * DROP
	 * CREATE
	 * ADD_COLUMN
	 * ADD_KEY
	 * CREATE_INDEX
	 */
	private static void performSort(DDLAction[] actions, Map<DDLAction, Set<DDLAction>> deps, Set<DDLAction> roots) {
		List<DDLAction> dropKeys = new LinkedList<DDLAction>();
		List<DDLAction> dropIndexes = new LinkedList<DDLAction>();
		List<DDLAction> dropColumns = new LinkedList<DDLAction>();
		List<DDLAction> changeColumns = new LinkedList<DDLAction>();
		List<DDLAction> drops = new LinkedList<DDLAction>();
		List<DDLAction> creates = new LinkedList<DDLAction>();
		List<DDLAction> addColumns = new LinkedList<DDLAction>();
		List<DDLAction> addKeys = new LinkedList<DDLAction>();
		List<DDLAction> createIndexes = new LinkedList<DDLAction>();
		
		for (DDLAction action : actions) {
			switch (action.getActionType()) {
				case ALTER_DROP_KEY:
					dropKeys.add(action);
				break;
				
				case DROP_INDEX:
					dropIndexes.add(action);
				break;
				
				case ALTER_DROP_COLUMN:
					dropColumns.add(action);
				break;
				
				case ALTER_CHANGE_COLUMN:
					changeColumns.add(action);
				break;
				
				case DROP:
					drops.add(action);
				break;
				
				case CREATE:
					creates.add(action);
				break;
				
				case ALTER_ADD_COLUMN:
					addColumns.add(action);
				break;
				
				case ALTER_ADD_KEY:
					addKeys.add(action);
				break;
				
				case CREATE_INDEX:
					createIndexes.add(action);
				break;
			}
		}
		
		roots.addAll(dropKeys);
		roots.addAll(dropIndexes);
		
		for (DDLAction action : dropColumns) {
			Set<DDLAction> dependencies = new HashSet<DDLAction>();
			
			for (DDLAction depAction : dropKeys) {
				DDLForeignKey key = depAction.getKey();
				
				if ((key.getTable().equals(action.getTable().getName()) && key.getForeignField().equals(action.getField().getName()))
						|| (key.getDomesticTable().equals(action.getTable().getName()) && key.getField().equals(action.getField().getName()))) {
					dependencies.add(depAction);
				}
			}
			
			if (dependencies.size() == 0) {
				roots.add(action);
			} else {
				deps.put(action, dependencies);
			}
		}
		
		for (DDLAction action : changeColumns) {
			Set<DDLAction> dependencies = new HashSet<DDLAction>();
			
			for (DDLAction depAction : dropKeys) {
				DDLForeignKey key = depAction.getKey();
				
				if ((key.getTable().equals(action.getTable().getName()) && key.getForeignField().equals(action.getField().getName()))
						|| (key.getDomesticTable().equals(action.getTable().getName()) && key.getField().equals(action.getField().getName()))) {
					dependencies.add(depAction);
				}
			}
			
			for (DDLAction depAction : dropColumns) {
				if ((depAction.getTable().equals(action.getTable()) && depAction.getField().equals(action.getField()))
						|| (depAction.getTable().equals(action.getTable()) && depAction.getField().equals(action.getOldField()))) {
					dependencies.add(depAction);
				}
			}
			
			if (dependencies.size() == 0) {
				roots.add(action);
			} else {
				deps.put(action, dependencies);
			}
		}
		
		for (DDLAction action : drops) {
			Set<DDLAction> dependencies = new HashSet<DDLAction>();
			
			for (DDLAction depAction : dropKeys) {
				DDLForeignKey key = depAction.getKey();
				
				if (key.getTable().equals(action.getTable().getName()) || key.getDomesticTable().equals(action.getTable().getName())) {
					dependencies.add(depAction);
				}
			}
			
			for (DDLAction depAction : dropColumns) {
				if (depAction.getTable().equals(action.getTable())) {
					dependencies.add(depAction);
				}
			}
			
			for (DDLAction depAction : changeColumns) {
				if (depAction.getTable().equals(action.getTable())) {
					dependencies.add(depAction);
				}
			}
			
			if (dependencies.size() == 0) {
				roots.add(action);
			} else {
				deps.put(action, dependencies);
			}
		}
		
		for (DDLAction action : creates) {
			Set<DDLAction> dependencies = new HashSet<DDLAction>();
			
			for (DDLForeignKey key : action.getTable().getForeignKeys()) {
				for (DDLAction depAction : creates) {
					if (depAction != action && depAction.getTable().getName().equals(key.getTable())) {
						dependencies.add(depAction);
					}
				}
				
				for (DDLAction depAction : addColumns) {
					if (depAction.getTable().getName().equals(key.getTable())
							&& depAction.getField().getName().equals(key.getForeignField())) {
						dependencies.add(depAction);
					}
				}
				
				for (DDLAction depAction : changeColumns) {
					if (depAction.getTable().getName().equals(key.getTable())
							&& depAction.getField().getName().equals(key.getForeignField())) {
						dependencies.add(depAction);
					}
				}
			}
			
			if (dependencies.size() == 0) {
				roots.add(action);
			} else {
				deps.put(action, dependencies);
			}
		}
		
		for (DDLAction action : addColumns) {
			Set<DDLAction> dependencies = new HashSet<DDLAction>();
			
			for (DDLAction depAction : creates) {
				if (depAction.getTable().equals(action.getTable())) {
					dependencies.add(depAction);
				}
			}
			
			if (dependencies.size() == 0) {
				roots.add(action);
			} else {
				deps.put(action, dependencies);
			}
		}
		
		for (DDLAction action : addKeys) {
			Set<DDLAction> dependencies = new HashSet<DDLAction>();
			DDLForeignKey key = action.getKey();
			
			for (DDLAction depAction : creates) {
				if (depAction.getTable().getName().equals(key.getTable()) 
						|| depAction.getTable().getName().equals(key.getDomesticTable())) {
					dependencies.add(depAction);
				}
			}
			
			for (DDLAction depAction : addColumns) {
				if ((depAction.getTable().getName().equals(key.getTable()) && depAction.getField().getName().equals(key.getForeignField())) 
						|| (depAction.getTable().getName().equals(key.getDomesticTable())) && depAction.getField().getName().equals(key.getField())) {
					dependencies.add(depAction);
				}
			}
			
			for (DDLAction depAction : changeColumns) {
				if ((depAction.getTable().getName().equals(key.getTable()) && depAction.getField().getName().equals(key.getForeignField())) 
						|| (depAction.getTable().getName().equals(key.getDomesticTable())) && depAction.getField().getName().equals(key.getField())) {
					dependencies.add(depAction);
				}
			}
			
			if (dependencies.size() == 0) {
				roots.add(action);
			} else {
				deps.put(action, dependencies);
			}
		}
		
		for (DDLAction action : createIndexes) {
			Set<DDLAction> dependencies = new HashSet<DDLAction>();
			DDLIndex index = action.getIndex();
			
			for (DDLAction depAction : creates) {
				if (depAction.getTable().getName().equals(index.getTable())) {
					dependencies.add(depAction);
				}
			}
			
			for (DDLAction depAction : addColumns) {
				if (depAction.getTable().getName().equals(index.getTable()) || depAction.getField().getName().equals(index.getField())) {
					dependencies.add(depAction);
				}
			}
			
			for (DDLAction depAction : changeColumns) {
				if (depAction.getTable().getName().equals(index.getTable()) || depAction.getField().getName().equals(index.getField())) {
					dependencies.add(depAction);
				}
			}
			
			if (dependencies.size() == 0) {
				roots.add(action);
			} else {
				deps.put(action, dependencies);
			}
		}
	}
	
	private static DDLAction createColumnAlterAction(DDLTable table, DDLField oldField, DDLField field) {
		DDLAction action = new DDLAction(DDLActionType.ALTER_CHANGE_COLUMN);
		action.setTable(table);
		action.setField(field);
		action.setOldField(oldField);
		return action;
	}
}