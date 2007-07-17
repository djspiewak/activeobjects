/*
 * Copyright 2007, Daniel Spiewak
 * All rights reserved
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 *   * Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *   * Redistributions in binary form must reproduce the above
 *     copyright notice, this list of conditions and the following
 *     disclaimer in the documentation and/or other materials provided
 *     with the distribution.
 *   * Neither the name of the ActiveObjects project nor the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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
