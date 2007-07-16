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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.StopAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.LockObtainFailedException;

/**
 * @author Daniel Spiewak
 */
public class IndexingEntityManager extends EntityManager {
	private Directory indexDir;
	private Analyzer analyzer;
	
	public IndexingEntityManager(DatabaseProvider provider, Directory indexDir) throws IOException {
		super(provider);
		
		init(indexDir);
	}

	public IndexingEntityManager(DatabaseProvider provider, Directory indexDir, boolean weaklyCache) throws IOException {
		super(provider, weaklyCache);
		
		init(indexDir);
	}

	public IndexingEntityManager(String uri, String username, String password, Directory indexDir) throws IOException {
		super(uri, username, password);
		
		init(indexDir);
	}
	
	@Override
	protected <T extends Entity> T getAndInstantiate(Class<T> type, int id) {
		T back = super.getAndInstantiate(type, id);
		back.addPropertyChangeListener(new IndexAppender<T>(back));
		
		return back;
	}
	
	public <T extends Entity> T[] search(Class<T> type, String strQuery) throws IOException, ParseException {
		String table = getNameConverter().getName(type);
		
		IndexSearcher searcher = new IndexSearcher(indexDir);
		QueryParser parser = new QueryParser(table + '.' + getDefaultSearchField(type), analyzer);
		org.apache.lucene.search.Query query = parser.parse(strQuery);
		
		Hits hits = searcher.search(query);
		int[] ids = new int[hits.length()];
		for (int i = 0; i < hits.length(); i++) {
			Document doc = hits.doc(i);
			
			ids[i] = Integer.parseInt(doc.get(table + ".id"));
		}
		searcher.close();
		
		return get(type, ids);
	}
	
	public void optimize() throws IOException {
		IndexWriter writer = null;
		try {
			writer = new IndexWriter(indexDir, analyzer, false);
			writer.optimize();
		} finally {
			try {
				writer.close();
			} catch (NullPointerException e) {
			}
		}
	}
	
	public Directory getIndexDir() {
		return indexDir;
	}
	
	public Analyzer getAnalyzer() {
		return analyzer;
	}

	private void init(Directory indexDir) throws IOException {
		this.indexDir = indexDir;
		
		analyzer = new StopAnalyzer();
		
		if (!indexDir.fileExists("segments.gen")) {
			new IndexWriter(indexDir, analyzer, true).close();
		}
	}
	
	private static List<String> getIndexFields(Class<? extends Entity> type) {
		List<String> back = new ArrayList<String>();
		
		for (Method m : type.getMethods()) {
			Index annot = m.getAnnotation(Index.class);
			
			if (annot != null) {
				Class<?> attributeType = Common.getAttributeTypeFromMethod(m);
				String name = Common.getAttributeNameFromMethod(m);
				
				// don't index Entity fields
				if (name != null && !Common.interfaceInheritsFrom(attributeType, Entity.class)) {
					back.add(name);
				}
			}
		}
		
		return back;
	}
	
	private static String getDefaultSearchField(Class<? extends Entity> type) {
		for (Method m : type.getMethods()) {
			Index annot = m.getAnnotation(Index.class);
			
			if (annot != null) {
				Class<?> attributeType = Common.getAttributeTypeFromMethod(m);
				String name = Common.getAttributeNameFromMethod(m);
				
				// don't index Entity fields
				if (name != null && !Common.interfaceInheritsFrom(attributeType, Entity.class)) {
					if (annot.value()) {
						return name;
					}
				}
			}
		}
		
		return getIndexFields(type).get(0);
	}
	
	private class IndexAppender<T extends Entity> implements PropertyChangeListener {
		private List<String> indexFields;
		
		private Document doc;
		
		private IndexAppender(T entity) {
			indexFields = getIndexFields(entity.getEntityType());
			
			doc = new Document();
			doc.add(new Field(getNameConverter().getName(entity.getEntityType()) + ".id", "" + entity.getID(), 
					Field.Store.YES, Field.Index.NO));
		}
		
		public void propertyChange(final PropertyChangeEvent evt) {
			if (indexFields.contains(evt.getPropertyName())) {
				new Thread() {
					{
						setPriority(3);
					}
					
					public void run() {
						T entity = (T) evt.getSource();
						
						doc.add(new Field(getNameConverter().getName(entity.getEntityType()) + '.' + evt.getPropertyName(), 
								evt.getNewValue().toString(), Field.Store.YES, Field.Index.TOKENIZED));
						
						IndexWriter writer = null;
						try {
							writer = new IndexWriter(getIndexDir(), getAnalyzer(), false);
							writer.updateDocument(new Term(getNameConverter().getName(entity.getEntityType()) + ".id", "" + entity.getID()), doc);
						} catch (CorruptIndexException e) {
						} catch (LockObtainFailedException e) {
						} catch (IOException e) {
						} finally {
							try {
								writer.close();
							} catch (Throwable t) {}
						}
					}
				}.start();
			}
		}
	}
}
