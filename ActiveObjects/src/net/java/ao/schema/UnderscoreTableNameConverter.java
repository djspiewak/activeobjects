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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.java.ao.Common;
import net.java.ao.RawEntity;

/**
 * <p>Imposes an underscore word-separation convention on table
 * names.  This will convert entity names in the following way:</p>
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
 * 			<td>license_registration</td>
 * 		</tr>
 * 
 * 		<tr>
 * 			<td><code>Volume4</code></td>
 * 			<td>volume_4</td>
 * 		</tr>
 * 
 * 		<tr>
 * 			<td><code>Company</code></td>
 * 			<td>company</td>
 * 		</tr>
 * </table>
 * 
 * <p>This converter allows for both all-lowercase and all-uppercase
 * table name conventions.  For example, depending on the configuration,
 * <code>LicenseRegistration</code> may convert to "LICENSE_REGISTRATION".</p>
 * 
 * <p>This converter, coupled with {@link PluralizedNameConverter} is
 * all that is required to emulate the ActiveRecord table name conversion.</p>
 * 
 * @author Francis Chong
 * @author Daniel Spiewak
 */
public class UnderscoreTableNameConverter extends AbstractTableNameConverter {
	private static final Pattern WORD_PATTERN = Pattern.compile("([a-z\\d])([A-Z\\d])");
	
	private boolean uppercase;
	
	/**
	 * Creates a new table name converter in which all table names will
	 * be either fully uppercase or fully lowercase.
	 * 
	 * @param uppercase	<code>true</code> if table names should be all
	 * 		uppercase, <code>false</code> if table names should be all
	 * 		lowercase. 
	 */
	public UnderscoreTableNameConverter(boolean uppercase) {
		this.uppercase = uppercase;
	}
	
	/**
	 * Returns whether or not resulting table names will be entirely
	 * uppercase.  If <code>false</code>, table names will be entirely
	 * lowercase.
	 */
	public boolean isUppercase() {
		return uppercase;
	}

    @Override
    protected String convertName(Class<? extends RawEntity<?>> entity) {
    	Matcher matcher = WORD_PATTERN.matcher(Common.convertSimpleClassName(entity.getCanonicalName()));
        return matcher.replaceAll("$1_$2");
    }
    
    @Override
    protected String postProcessName(String back) {
		if (uppercase) {
			return back.toUpperCase();
		}
		
		return back.toLowerCase();
    }
}
