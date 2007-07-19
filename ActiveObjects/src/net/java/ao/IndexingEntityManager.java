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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.StopAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.MultiFieldQueryParser;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;

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
		List<String> indexFields = Common.getIndexFields(type);
		String[] searchFields = new String[indexFields.size()];
		
		for (int i = 0; i < searchFields.length; i++) {
			searchFields[i] = table + '.' + indexFields.get(i);
		}
		
		IndexSearcher searcher = new IndexSearcher(indexDir);
		QueryParser parser = new MultiFieldQueryParser(searchFields, analyzer);
		org.apache.lucene.search.Query query = parser.parse(strQuery);
		
		Hits hits = searcher.search(query);
		int[] ids = new int[hits.length()];
		for (int i = 0; i < hits.length(); i++) {
			ids[i] = Integer.parseInt(hits.doc(i).get(table + ".id"));
		}
		searcher.close();
		
		return get(type, ids);
	}
	
	@Override
	public void delete(Entity... entities) throws SQLException {
		super.delete(entities);
		
		IndexReader reader = null;
		try {
			reader = IndexReader.open(indexDir);
			for (Entity entity : entities) {
				removeFromIndexImpl(entity, reader);
			}
		} catch (IOException e) {
			throw new SQLException(e);
		} finally {
			try {
				reader.close();
			} catch (Throwable t) {}
		}
	}
	
	public void addToIndex(Entity entity) throws IOException {
		String table = getNameConverter().getName(entity.getEntityType());
		
		IndexWriter writer = null;
		try {
			writer = new IndexWriter(indexDir, analyzer, false);
			
			Document doc = new Document();
			doc.add(new Field(getNameConverter().getName(entity.getEntityType()) + ".id", "" + entity.getID(), 
					Field.Store.YES, Field.Index.UN_TOKENIZED));
			
			for (Method m : entity.getEntityType().getMethods()) {
				Index indexAnno = m.getAnnotation(Index.class);
				
				if (indexAnno != null) {
					if (m.getName().startsWith("get") || m.getName().startsWith("is") || m.getAnnotation(Accessor.class) != null) {
						String attribute = Common.getAttributeNameFromMethod(m);
						Object value = m.invoke(entity);
						
						if (value != null) {
							doc.add(new Field(table + '.' + attribute, 	value.toString(), Field.Store.YES, Field.Index.TOKENIZED));
						}
					}
				}
			}
			
			writer.addDocument(doc);
		} catch (IllegalArgumentException e) {
			throw new IOException(e);
		} catch (IllegalAccessException e) {
			throw new IOException(e);
		} catch (InvocationTargetException e) {
			throw new IOException(e);
		} finally {
			try {
				writer.close();
			} catch (Throwable t) {}
		}
	}
	
	public void removeFromIndex(Entity entity) throws IOException {
		IndexReader reader = null;
		try {
			reader = IndexReader.open(indexDir);
			removeFromIndexImpl(entity, reader);
		} finally {
			try {
				reader.close();
			} catch (Throwable t) {}
		}
	}
	
	private void removeFromIndexImpl(Entity entity, IndexReader reader) throws IOException {
		reader.deleteDocuments(new Term(getNameConverter().getName(entity.getEntityType()) + ".id", "" + entity.getID()));
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
		
		if (!IndexReader.indexExists(indexDir)) {
			new IndexWriter(indexDir, analyzer, true).close();
		}
	}
	
	private class IndexAppender<T extends Entity> implements PropertyChangeListener {
		private List<String> indexFields;
		
		private Document doc;
		
		private IndexAppender(T entity) {
			indexFields = Common.getIndexFields(entity.getEntityType());
			
			doc = new Document();
			doc.add(new Field(getNameConverter().getName(entity.getEntityType()) + ".id", "" + entity.getID(), 
					Field.Store.YES, Field.Index.UN_TOKENIZED));
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
