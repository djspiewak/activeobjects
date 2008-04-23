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
 * Specifies a specific DDL action type in a database-agnostic
 * manner.  This enum contains all of the DDL action types
 * supported by ActiveObjects, regardless of the fact that not
 * all databases support all of these actions.
 * 
 * @author Daniel Spiewak
 */
public enum DDLActionType {
	CREATE,
	DROP,
	ALTER_ADD_COLUMN,
	ALTER_CHANGE_COLUMN,
	ALTER_DROP_COLUMN,
	ALTER_ADD_KEY,
	ALTER_DROP_KEY,
	CREATE_INDEX,
	DROP_INDEX
}
