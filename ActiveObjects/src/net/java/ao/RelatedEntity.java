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

import net.java.ao.schema.Ignore;

/**
 * <p>A superinterface for entities which are related to each other using
 * Lucene heuristics.  This is <i>not</i> required as a superinterface
 * for entities related to one another at the database level.  Thus, this
 * interface is only relevant to those already using full-text indexing.</p>
 * 
 * <p>While this entity does not extend {@link RawEntity} itself, it should
 * only be inherited by interfaces which do.  In fact, this is enforced by
 * the type parameterization of this interface.  Example:</p>
 * 
 * <pre>public interface Post extends Entity, RelatedEntity&lt;Post&gt; {
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
 * Post p = manager.get(Post.class, 123);
 * Post[] relatedPosts = p.getRelated();      // searches Lucene index for related posts</pre>
 * 
 * <p>Under most circumstances, the type parameter used when
 * inheriting from this interface should be the entity interface itself, as
 * in the above example.</p>
 * 
 * @author Daniel Spiewak
 * @see net.java.ao.SearchableEntityManager
 */
@Implementation(RelatedEntityImpl.class)
public interface RelatedEntity<T extends RelatedEntity<T> & RawEntity<?>> {
	
	/**
	 * Retrieves the entities related to this by using full-text search
	 * heuristics.  No database invocation is involved in calling this
	 * method.  Also, the results are uncached, relying instead on the 
	 * underlying performance optimizations of Lucene itself.
	 * 
	 * @return Any entities of the relevant type which relate to the current 
	 * 	instance in the full-text index.
	 */
	@Ignore
	public T[] getRelated();
}
