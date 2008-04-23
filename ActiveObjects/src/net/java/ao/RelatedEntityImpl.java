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
package net.java.ao;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.similar.MoreLikeThis;
import org.apache.lucene.store.Directory;

/**
 * @author Daniel Spiewak
 */
class RelatedEntityImpl {
	private RawEntity<Object> entity;
	
	public RelatedEntityImpl(RelatedEntity<?> entity) {
		this.entity = (RawEntity<Object>) entity;
	}
	
	public RelatedEntity<?>[] getRelated() throws IOException {
		Class<? extends RawEntity<Object>> type = entity.getEntityType();
		String table = entity.getEntityManager().getTableNameConverter().getName(type);
		List<String> indexFields = Common.getSearchableFields(entity.getEntityManager(), type);
		String[] searchFields = new String[indexFields.size()];
		
		for (int i = 0; i < searchFields.length; i++) {
			searchFields[i] = table + '.' + indexFields.get(i);
		}

		Directory indexDir = ((SearchableEntityManager) entity.getEntityManager()).getIndexDir();
		IndexReader reader = null;
		
		try {
			reader = IndexReader.open(indexDir);
			IndexSearcher searcher = new IndexSearcher(indexDir);

			MoreLikeThis more = new MoreLikeThis(reader);
			more.setFieldNames(searchFields);
			more.setAnalyzer(((SearchableEntityManager) entity.getEntityManager()).getAnalyzer());
			
			int docID = -1;
			String primaryKeyField = Common.getPrimaryKeyField(entity.getEntityType(), entity.getEntityManager().getFieldNameConverter());
			Object primaryKeyValue = Common.getPrimaryKeyValue(entity);
			
			TermDocs docs = reader.termDocs(new Term(table + "." + primaryKeyField, 	
					Common.getPrimaryKeyType(type).valueToString(primaryKeyValue)));
			if (docs.next()) {
				docID = docs.doc();
			}
			
			if (docID < 0) {
				return (RelatedEntity<?>[]) Array.newInstance(type, 0);
			}

			org.apache.lucene.search.Query query = more.like(docID);
			Hits hits = searcher.search(query);
			List<RelatedEntity<?>> back = new ArrayList<RelatedEntity<?>>();

			for (int i = 0; i < hits.length(); i++) {
				String entityKey = hits.doc(i).get(table + "." + primaryKeyField);
				if (entityKey.equals(primaryKeyValue.toString())) {
					continue;
				}
				
				back.add((RelatedEntity<?>) entity.getEntityManager().get(type, Common.getPrimaryKeyType(type).defaultParseValue(entityKey)));
			}

			return back.toArray((RelatedEntity<?>[]) Array.newInstance(type, back.size()));
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {}
			}
		}
	}
}
