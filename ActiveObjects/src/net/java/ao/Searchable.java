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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>Used to tag methods as being full-text searchable within a 
 * Lucene index.  This annotation is only relevant when used in
 * conjunction with an instance of {@link SearchableEntityManager}.
 * Otherwise, it is ignored by ActiveObjects.  Example:</p>
 * 
 * <pre>public interface Post extends Entity {
 *     public String getTitle();
 *     public void setTitle(String title);
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
 * SearchableEntityManager manager = ...;
 * manager.search(Post.class, "my search string");    // returns search results as Post[]</pre>
 * 
 * <p>At the moment, it is impossible to specify the Lucene field and
 * type which will be used in the index.  By default, <code>tablename.fieldname</code>
 * is utilized to determine the Lucene field.  More features are planned
 * for this annotation in future.</p>
 * 
 * @author Daniel Spiewak
 * @see net.java.ao.SearchableEntityManager
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Searchable {}
