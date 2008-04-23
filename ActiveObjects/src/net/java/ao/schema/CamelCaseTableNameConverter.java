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
import net.java.ao.RawEntity;

/**
 * <p>Imposes a standard camelCase convention upon table names.  This will
 * convert entity names in the following way:</p>
 * 
 * <table border="1">
 * 		<tr>
 * 			<td><b>Entity Name</b></td>
 * 			<td><b>Table Name</b></td>
 * 		</tr>
 * 
 * 		<tr>
 * 			<td><code>Person</code></td>
 * 			<td>person</td>
 * 		</tr>
 * 
 * 		<tr>
 * 			<td><code>LicenseRegistration</code></td>
 * 			<td>licenseRegistration</td>
 * 		</tr>
 * 
 * 		<tr>
 * 			<td><code>Volume4</code></td>
 * 			<td>volume4</td>
 * 		</tr>
 * 
 * 		<tr>
 * 			<td><code>Company</code></td>
 * 			<td>company</td>
 * 		</tr>
 * </table>
 * 
 * <p>This is the default table name converter for ActiveObjects.</p>
 * 
 * @author Daniel Spiewak
 */
public class CamelCaseTableNameConverter extends AbstractTableNameConverter {

	@Override
	protected String convertName(Class<? extends RawEntity<?>> type) {
		return Common.convertDowncaseName(Common.convertSimpleClassName(type.getCanonicalName()));
	}
}
