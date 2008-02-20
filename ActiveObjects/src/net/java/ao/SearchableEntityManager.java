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
import org.apache.lucene.index.CorruptIndexException;
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
 * <p>Required to manage entities with enabled full-text searching.  This is where
 * all the "meat" of the Lucene indexing support is actually implemented.  It is
 * required to use this {@link EntityManager} implementation to use full-text
 * searching.  Example:</p>
 * 
 * <pre>public interface Post extends Entity {
 *     // ...
 *     
 *     &#064;Searchable
 *     &#064;SQLType(Types.BLOB)
 *     public String getBody();
 *     
 *     &#064;Searchable
 *     &#064;SQLType(Types.BLOB)
 *     public void setBody(String body);
 * }
 * 
 * // ...
 * SearchableEntityManager manager = new SearchableEntityManager(
 *         uri, username, password, FSDirectory.getDirectory("~/lucene_index"));
 * manager.search(Post.class, "my search string");       // returns results as Post[]</pre>
 * 
 * <p>This class does not support any Java full-text search libraries other than
 * Lucene.  Also, the support for Lucene itself is comparatively limited.  If your
 * requirements dictate more advanced functionality, you should consider
 * writing a custom implementation of this class to provide the enhancements
 * you need.  More features are planned for this class in future...</p>
 * 
 * @author Daniel Spiewak
 * @see net.java.ao.Searchable
 */
public class SearchableEntityManager extends EntityManager {
	static boolean asynchronous = true;		// hack for testing
	
	private Directory indexDir;

	private Analyzer analyzer;

	/**
	 * Constructs a new instance with the specified provider and index
	 * {@link Directory}.  Delegates more-or-less all of the functionality
	 * to {@link EntityManager}.
	 * 
	 * @throws IOException		If Lucene was unable to open the index.
	 * @see net.java.ao.EntityManager#EntityManager(DatabaseProvider)
	 */
	public SearchableEntityManager(DatabaseProvider provider, Directory indexDir) throws IOException {
		super(provider);

		init(indexDir);
	}

	/**
	 * Constructs a new instance with the specified provider, index
	 * {@link Directory}, and <code>weaklyCache</code> flag.  Delegates 
	 * more-or-less all of the functionality to {@link EntityManager}.
	 * 
	 * @throws IOException		If Lucene was unable to open the index.
	 * @see net.java.ao.EntityManager#EntityManager(DatabaseProvider, boolean)
	 */
	public SearchableEntityManager(DatabaseProvider provider, Directory indexDir, boolean weaklyCache) throws IOException {
		super(provider, weaklyCache);

		init(indexDir);
	}

	/**
	 * Constructs a new instance with the specified JDBC information and index
	 * {@link Directory}.  Delegates more-or-less all of the functionality to 
	 * {@link EntityManager}.
	 * 
	 * @throws IOException		If Lucene was unable to open the index.
	 * @see net.java.ao.EntityManager#EntityManager(String, String, String)
	 */
	public SearchableEntityManager(String uri, String username, String password, Directory indexDir) throws IOException {
		super(uri, username, password);

		init(indexDir);
	}

	@Override
	protected <T extends RawEntity<K>, K> T getAndInstantiate(Class<T> type, K key) {
		T back = super.getAndInstantiate(type, key);
		back.addPropertyChangeListener(new IndexAppender<T, K>(back));

		return back;
	}

	/**
	 * Runs a Lucene full-text search on the specified entity type with the given
	 * query.  The search will be run on every {@link Searchable} field within
	 * the entity.  No caching is performed in this method.  Rather, AO relies
	 * upon the underlying Lucene code to be performant.
	 * 
	 * @param type		The type of the entities to search for.
	 * @param strQuery	The query to pass to Lucene for the search.
	 * @throws IOException		If Lucene was unable to open the index.
	 * @throws ParseException	If Lucene was unable to parse the search string into a valid query.
	 * @return The entity instances which correspond with the search results.
	 */
	@SuppressWarnings("unchecked")
	public <T extends RawEntity<K>, K> T[] search(Class<T> type, String strQuery) throws IOException, ParseException {
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
		K[] keys = (K[]) new Object[hits.length()];
		
		for (int i = 0; i < hits.length(); i++) {
			keys[i] = (K) dbType.defaultParseValue(hits.doc(i).get(table + "." + primaryKeyField));
		}
		searcher.close();

		return get(type, keys);
	}

	@Override
	public void delete(RawEntity<?>... entities) throws SQLException {
		super.delete(entities);

		IndexReader reader = null;
		try {
			reader = IndexReader.open(indexDir);
			for (RawEntity<?> entity : entities) {
				removeFromIndexImpl(entity, reader);
			}
		} catch (IOException e) {
			throw (SQLException) new SQLException().initCause(e);
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
				}
			}
		}
	}

	/**
	 * Adds the entity instance to the index.  No checking is performed to 
	 * ensure that the entity is not already part of the index.  All of the
	 * {@link Searchable} fields within the entity will be added to the
	 * index as part of the document corresponding to the instance.
	 * 
	 * @param entity	The entity to add to the index.
	 * @throws IOException		If Lucene was unable to open the index.
	 */
	public void addToIndex(RawEntity<?> entity) throws IOException {
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
				Searchable indexAnno = Common.getAnnotationDelegate(getFieldNameConverter(), m).getAnnotation(Searchable.class);

				if (indexAnno != null) {
					shouldAdd = true;
					
					if (Common.isAccessor(m)) {
						String attribute = getFieldNameConverter().getName(m);
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
			if (writer != null) {
				writer.close();
			}
		}
	}

	/**
	 * Removes the specified entity from the Lucene index.  This performs a lookup
	 * in the index based on the value of the entity primary key and removes the
	 * appropriate {@link Document}.
	 * 
	 * @param entity	The entity to remove from the index.
	 * @throws IOException		If Lucene was unable to open the index.
	 */
	public void removeFromIndex(RawEntity<?> entity) throws IOException {
		IndexReader reader = null;
		try {
			reader = IndexReader.open(indexDir);
			removeFromIndexImpl(entity, reader);
		} finally {
			if (reader != null) {
				reader.close();
			}
		}
	}

	private void removeFromIndexImpl(RawEntity<?> entity, IndexReader reader) throws IOException {
		reader.deleteDocuments(new Term(getTableNameConverter().getName(entity.getEntityType()) + "." 
				+ Common.getPrimaryKeyField(entity.getEntityType(), getFieldNameConverter()), 
				Common.getPrimaryKeyType(entity.getEntityType()).valueToString(Common.getPrimaryKeyValue(entity))));
	}

	/**
	 * <p>Optimizes the Lucene index for searching.  This call peers down to
	 * <code>IndexWriter#optimize()</code>.  For sizable indexes, this
	 * call will take some time, so it is best not to perform the operation
	 * in scenarios where it may block interface responsiveness (such as
	 * in the middle of a page request, or within the EDT).</p>
	 * 
	 * <p>This method is the only optimization call made against the Lucene
	 * index.  Meaning, <code>SearchableEntityManager</code> never
	 * optimizes the index automatically, as this could potentially cause major
	 * performance issues.  Developers should be aware of this and the
	 * negative impact lack-of optimization can have upon search performance.</p>
	 * 
	 * @throws IOException		If Lucene was unable to open the index.
	 */
	public void optimize() throws IOException {
		IndexWriter writer = null;
		try {
			writer = new IndexWriter(indexDir, analyzer, false);
			writer.optimize();
		} finally {
			if (writer != null) {
				writer.close();
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

	private class IndexAppender<T extends RawEntity<K>, K> implements PropertyChangeListener {
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
				Thread t = new Thread() {
					{
						setPriority(3);
					}

					@Override
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
							if (writer != null) {
								try {
									writer.close();
								} catch (CorruptIndexException e) {
									e.printStackTrace();
								} catch (IOException e) {
									e.printStackTrace();
								}
							}
						}
					}
				};
				
				if (asynchronous) {
					t.start();
				} else {
					t.run();
				}
			}
		}
	}
}
