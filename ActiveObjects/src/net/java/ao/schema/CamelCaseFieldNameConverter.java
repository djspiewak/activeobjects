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
package net.java.ao.schema;

import net.java.ao.Common;

/**
 * <p>Imposes a standard camelCase convention upon field names.  This will
 * convert field in the following way:</p>
 * 
 * <table border="1">
 * 		<tr>
 * 			<td><b>Method Name</b></td>
 * 			<td><b>Returns Entity?</b></td>
 * 			<td><b>Field Name</b></td>
 * 		</tr>
 * 
 * 		<tr>
 * 			<td>getFirstName</td>
 * 			<td><code>false</code></td>
 * 			<td>firstName</td>
 * 		</tr>
 * 
 * 		<tr>
 * 			<td>setLastName</td>
 * 			<td><code>false</code></td>
 * 			<td>lastName</td>
 * 		</tr>
 * 
 * 		<tr>
 * 			<td>getCompany</td>
 * 			<td><code>true</code></td>
 * 			<td>companyID</td>
 * 		</tr>
 * 
 * 		<tr>
 * 			<td>isCool</td>
 * 			<td><code>false</code></td>
 * 			<td>cool</td>
 * 		</tr>
 * </table>
 * 
 * <p>This is the default field name converter for ActiveObjects.</p>
 * 
 * @author Daniel Spiewak
 */
public class CamelCaseFieldNameConverter extends AbstractFieldNameConverter {
	@Override
	protected String convertName(String name, boolean entity, boolean polyType) {
		name = Common.convertDowncaseName(name);
		
		if (polyType) {
			name += "Type";
		} else if (entity) {
			name += "ID";
		}
		
		return name;
	}
}
