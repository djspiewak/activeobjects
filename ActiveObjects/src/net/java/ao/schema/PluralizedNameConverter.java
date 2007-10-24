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

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import net.java.ao.RawEntity;

/**
 * @author Daniel Spiewak
 */
public class PluralizedNameConverter extends AbstractTableNameConverter {
	private TableNameConverter delegate;
	
	public PluralizedNameConverter() {
		this(new CamelCaseTableNameConverter());
	}
	
	@SuppressWarnings("unchecked")
	public PluralizedNameConverter(TableNameConverter delegateConverter) {
		OrderedProperties rules = new OrderedProperties();
		
		InputStream is = PluralizedNameConverter.class.getResourceAsStream(
				"/net/java/ao/schema/englishPluralRules.properties");
		
		try {
			rules.load(is);
		} catch (IOException e) {
			return;
		} finally {
			try {
				is.close();
			} catch (IOException e) {
			}
		}
		
		addPatternMappings((Map) rules, rules.iterator());
		
		delegate = delegateConverter;
	}
	
	@Override
	protected String convertName(Class<? extends RawEntity<?>> entity) {
		return delegate.getName(entity);
	}
	
	@Override
	protected String postProcessName(String back) {
		if (delegate instanceof AbstractTableNameConverter) {
			return ((AbstractTableNameConverter) delegate).postProcessName(back);
		}
		
		return back;
	}
}
