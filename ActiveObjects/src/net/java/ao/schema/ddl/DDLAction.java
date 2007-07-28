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

/**
 * @author Daniel Spiewak
 */
public class DDLAction {
	private DDLActionType actionType;
	
	private DDLTable table;
	private DDLField field;
	private DDLForeignKey key;
	
	public DDLAction(DDLActionType actionType) {
		this.actionType = actionType;
	}

	public DDLTable getTable() {
		return table;
	}

	public void setTable(DDLTable table) {
		this.table = table;
	}

	public DDLField getField() {
		return field;
	}

	public void setField(DDLField field) {
		this.field = field;
	}

	public DDLForeignKey getKey() {
		return key;
	}

	public void setKey(DDLForeignKey key) {
		this.key = key;
	}

	public DDLActionType getActionType() {
		return actionType;
	}
}
