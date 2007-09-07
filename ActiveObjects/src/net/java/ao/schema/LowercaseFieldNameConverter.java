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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * @author Daniel Spiewak
 */
public class LowercaseFieldNameConverter extends AbstractFieldNameConverter {
	private static final Pattern WORD_PATTERN = Pattern.compile("[a-z\\d][A-Z\\d]");
	
	@Override
	protected String convertName(String name, boolean entity) {
		List<Integer> indexes = new ArrayList<Integer>();
		Matcher matcher = WORD_PATTERN.matcher(name);
		StringBuilder back = new StringBuilder();
		
		while (matcher.find()) {
			indexes.add(matcher.start() + 1);
		}
		indexes.add(name.length());
		
		int start = 0;
		for (int index : indexes) {
			back.append(name.substring(start, index).toLowerCase()).append('_');
			
			start += index;
		}
		back.setLength(back.length() - 1);
		
		if (entity) {
			back.append("_id");
		}
		
		return back.toString();
	}
}
