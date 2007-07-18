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
	private RelatedEntity<?> entity;
	
	public RelatedEntityImpl(RelatedEntity<?> entity) {
		this.entity = entity;
	}
	
	public RelatedEntity<?>[] getRelated() throws IOException {
		Class<? extends Entity> type = entity.getEntityType();
		String table = entity.getEntityManager().getNameConverter().getName(type);
		List<String> indexFields = Common.getIndexFields(type);
		String[] searchFields = new String[indexFields.size()];
		
		for (int i = 0; i < searchFields.length; i++) {
			searchFields[i] = table + '.' + indexFields.get(i);
		}

		Directory indexDir = ((IndexingEntityManager) entity.getEntityManager()).getIndexDir();
		IndexReader reader = null;
		
		try {
			reader = IndexReader.open(indexDir);
			IndexSearcher searcher = new IndexSearcher(indexDir);

			MoreLikeThis more = new MoreLikeThis(reader);
			more.setFieldNames(searchFields);
			more.setAnalyzer(((IndexingEntityManager) entity.getEntityManager()).getAnalyzer());
			
			int docID = -1;
			TermDocs docs = reader.termDocs(new Term(table + ".id", "" + entity.getID()));
			if (docs.next()) {
				docID = docs.doc();
			}

			org.apache.lucene.search.Query query = more.like(docID);
			Hits hits = searcher.search(query);
			RelatedEntity<?>[] back = (RelatedEntity<?>[]) Array.newInstance(type, hits.length());

			for (int i = 0; i < back.length; i++) {
				back[i] = (RelatedEntity<?>) entity.getEntityManager().get(type, Integer.parseInt(hits.doc(i).get(table + ".id")));
			}

			return back;
		} finally {
			try {
				reader.close();
			} catch (Throwable t) {}
		}
	}
}
