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

import net.java.ao.types.DatabaseType;

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
public class SearchableEntityManager extends EntityManager {
	private Directory indexDir;

	private Analyzer analyzer;

	public SearchableEntityManager(DatabaseProvider provider, Directory indexDir) throws IOException {
		super(provider);

		init(indexDir);
	}

	public SearchableEntityManager(DatabaseProvider provider, Directory indexDir, boolean weaklyCache) throws IOException {
		super(provider, weaklyCache);

		init(indexDir);
	}

	public SearchableEntityManager(String uri, String username, String password, Directory indexDir) throws IOException {
		super(uri, username, password);

		init(indexDir);
	}

	@Override
	protected <T extends RawEntity> T getAndInstantiate(Class<T> type, Object key) {
		T back = super.getAndInstantiate(type, key);
		back.addPropertyChangeListener(new IndexAppender<T>(back));

		return back;
	}

	@SuppressWarnings("unchecked")
	public <T extends RawEntity> T[] search(Class<T> type, String strQuery) throws IOException, ParseException {
		String table = getTableNameConverter().getName(type);
		List<String> indexFields = Common.getSearchableFields(this, type);
		String[] searchFields = new String[indexFields.size()];
		String primaryKeyField = Common.getPrimaryKeyField(type, getFieldNameConverter());
		DatabaseType dbType = Common.getPrimaryKeyType(type);

		for (int i = 0; i < searchFields.length; i++) {
			searchFields[i] = table + '.' + indexFields.get(i);
		}

		IndexSearcher searcher = new IndexSearcher(indexDir);
		QueryParser parser = new MultiFieldQueryParser(searchFields, analyzer);
		org.apache.lucene.search.Query query = parser.parse(strQuery);

		Hits hits = searcher.search(query);
		Object[] keys = new Object[hits.length()];
		
		for (int i = 0; i < hits.length(); i++) {
			keys[i] = dbType.defaultParseValue(hits.doc(i).get(table + "." + primaryKeyField));
		}
		searcher.close();

		return get(type, keys);
	}

	@Override
	public void delete(RawEntity... entities) throws SQLException {
		super.delete(entities);

		IndexReader reader = null;
		try {
			reader = IndexReader.open(indexDir);
			for (RawEntity entity : entities) {
				removeFromIndexImpl(entity, reader);
			}
		} catch (IOException e) {
			throw (SQLException) new SQLException().initCause(e);
		} finally {
			try {
				reader.close();
			} catch (Throwable t) {
			}
		}
	}

	public void addToIndex(RawEntity entity) throws IOException {
		String table = getTableNameConverter().getName(entity.getEntityType());

		IndexWriter writer = null;
		try {
			writer = new IndexWriter(indexDir, analyzer, false);

			Document doc = new Document();
			doc.add(new Field(getTableNameConverter().getName(entity.getEntityType()) + "." 
					+ Common.getPrimaryKeyField(entity.getEntityType(), getFieldNameConverter()), 
					Common.getPrimaryKeyType(entity.getEntityType()).valueToString(Common.getPrimaryKeyValue(entity)), 
					Field.Store.YES, Field.Index.UN_TOKENIZED));

			boolean shouldAdd = false;
			for (Method m : entity.getEntityType().getMethods()) {
				Searchable indexAnno = m.getAnnotation(Searchable.class);

				if (indexAnno != null) {
					shouldAdd = true;
					
					if (Common.isAccessor(m)) {
						String attribute = getFieldNameConverter().getName(entity.getEntityType(), m);
						Object value = m.invoke(entity);

						if (value != null) {
							doc.add(new Field(table + '.' + attribute, value.toString(), Field.Store.YES, Field.Index.TOKENIZED));
						}
					}
				}
			}

			if (shouldAdd) {
				writer.addDocument(doc);
			}
		} catch (IllegalArgumentException e) {
			throw (IOException) new IOException().initCause(e);
		} catch (IllegalAccessException e) {
			throw (IOException) new IOException().initCause(e);
		} catch (InvocationTargetException e) {
			throw (IOException) new IOException().initCause(e);
		} finally {
			try {
				writer.close();
			} catch (Throwable t) {
			}
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
			} catch (Throwable t) {
			}
		}
	}

	private void removeFromIndexImpl(RawEntity entity, IndexReader reader) throws IOException {
		reader.deleteDocuments(new Term(getTableNameConverter().getName(entity.getEntityType()) + "." 
				+ Common.getPrimaryKeyField(entity.getEntityType(), getFieldNameConverter()), 
				Common.getPrimaryKeyType(entity.getEntityType()).valueToString(Common.getPrimaryKeyValue(entity))));
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

	private class IndexAppender<T extends RawEntity> implements PropertyChangeListener {
		private List<String> indexFields;

		private Document doc;

		private IndexAppender(T entity) {
			indexFields = Common.getSearchableFields(SearchableEntityManager.this, entity.getEntityType());

			doc = new Document();
			doc.add(new Field(getTableNameConverter().getName(entity.getEntityType()) + "." 
					+ Common.getPrimaryKeyField(entity.getEntityType(), getFieldNameConverter()), 
					Common.getPrimaryKeyType(entity.getEntityType()).valueToString(Common.getPrimaryKeyValue(entity)), 
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

						doc.add(new Field(getTableNameConverter().getName(entity.getEntityType()) + '.' 
								+ evt.getPropertyName(), evt.getNewValue().toString(), Field.Store.YES, Field.Index.TOKENIZED));

						IndexWriter writer = null;
						try {
							writer = new IndexWriter(getIndexDir(), getAnalyzer(), false);
							writer.updateDocument(new Term(getTableNameConverter().getName(entity.getEntityType()) + "." 
									+ Common.getPrimaryKeyField(entity.getEntityType(), getFieldNameConverter()), 
									Common.getPrimaryKeyType(entity.getEntityType()).valueToString(Common.getPrimaryKeyValue(entity))), doc);
						} catch (IOException e) {
						} finally {
							try {
								writer.close();
							} catch (Throwable t) {
							}
						}
					}
				}.start();
			}
		}
	}
}
